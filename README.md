# Hitorro Base DMS

This is the **hitorro-basedms** project, a standalone Document Management System with database persistence, content management, and workflow capabilities.

## Overview

The Hitorro Base DMS provides:

- **Document Management**: Version control, metadata management, content storage
- **Database Persistence**: Hibernate ORM integration with Derby database
- **Content Processing**: 
  - RSS/Atom feed handling
  - MP3 metadata extraction
  - Office document processing (Word, Excel, PowerPoint)
- **User Management**: Authentication, permissions, roles
- **Workflow & Job Scheduling**: Quartz-based job scheduling
- **Storage**: File system and S3 storage backends
- **SSH/SFTP Support**: Remote file transfer capabilities
- **Web Framework**: Apache Tapestry integration

## Project Structure

This is a standalone Maven JAR project.

```
hitorro-basedms/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/hitorro/base/objects/     # Domain objects
│   │   │   ├── com/hitorro/basedms/          # DMS services
│   │   │   └── ...
│   │   └── resources/
│   └── test/
├── pom.xml
└── README.md
```

## Storage backends

The DMS resolves each `Content` to a physical bytes-source via its
`Store`. `Store.storeType` picks the backend; the four shipped types
plug in as `case` branches in `Content.setContentAux()` +
`Content.getContent()`.

| StoreType | Bytes live in | Requires | Pick when |
|---|---|---|---|
| `Blob` | JDBC BLOB column | Hibernate-backed DB | Small contents that must be transactionally consistent with metadata; simplest ops model. |
| `File` | `<Store.rootPath>/<hex-idToHexPath>` | Filesystem writable at `rootPath` (via `BaseFile` — supports `file:`, `hdfs://`, `s3://`, `ftp://`) | Default for large + medium contents; scales with the underlying FS. |
| `Unmanaged` | External path stored verbatim in `originalFileName` | Read-only source | Registering pre-existing files without copying — DMS metadata over foreign bytes. |
| `Link` | Fetched from a URL at read time | Reachable HTTP endpoint | Content sourced from an external system; no local copy at rest. |
| `KVStore` | RocksDB at `<Store.rootPath>/rocksdb/` | `hitorro-kvstore` on classpath (~15 MB rocksdbjni) | Many small contents where LSM compaction beats one-file-per-content; single-process access. |

### Choosing a backend

* **`File`** is still the default for most workloads — cheap, streamable,
  works with any filesystem `BaseFile` knows about (local, HDFS, S3).
* **`Blob`** for transactional consistency with metadata (single DB
  commit covers both) — but tops out at practical BLOB sizes.
* **`Unmanaged`** and **`Link`** for foreign bytes the DMS doesn't own.
* **`KVStore`** when you have millions of small contents (few KB each)
  and one-file-per-content overwhelms the FS inode table. RocksDB's
  LSM merges keep it compact; ideal for chat message attachments,
  thumbnails, per-user profile blobs.

### KVStore key layout

Content bytes under `content:<fileName>` — `fileName` uses the same
`FileUtil.idToHexPath` scheme as the `File` backend, so a future
File ↔ KVStore migration tool wouldn't have to translate names.
The `content:` prefix reserves the keyspace for future row kinds
(index metadata, version pointers, etc.) without a migration.

Handle lifecycle: one `RocksDBStore` per DMS Store, cached
process-wide by absolute rootPath. Multiple concurrent Content ops on
the same Store share a single open handle (no file-lock races). A JVM
shutdown hook closes every cached handle so RocksDB releases its
locks cleanly on graceful exit.

### Migrating between backends

`StoreMigrator` bulk-copies contents between two Stores. Because
File and KVStore use the same `idToHexPath` fileName convention,
migration is a pure byte-copy — no name translation, no id remapping.

From code:

```java
Store src  = /* your File-backed Store */;
Store dest = /* your KVStore-backed Store */;
StoreMigrator.Report r = StoreMigrator.migrate(src, dest);
System.out.println("copied=" + r.copied() + " errors=" + r.errors().size());
```

From the shell (bundled CLI):

```bash
java -cp hitorro-basedms.jar:… com.hitorro.base.objects.StoreMigratorCli \
    --source-type File    --source-root /var/dms/legacy \
    --dest-type   KVStore --dest-root  /var/dms/rocksdb
# Output:
#   copied:  47122
#   skipped: 0
#   errors:  0
#   elapsed: 8341ms
```

Supported directions: File ↔ KVStore, File → File, KVStore → KVStore.
Blob is not supported by the standalone CLI (needs a JDBC session);
`StoreMigrator.migrate()` from within an app can add Blob paths.

**Not transactional** — a mid-copy failure leaves both stores populated
with the successfully-migrated subset. Re-run: puts overwrite, so
already-copied contents are equal-byte no-ops. Consult `Report.errors()`
for what to investigate.

**Does NOT touch Content DB rows** — after the byte-copy finishes, run
a SQL `UPDATE content SET store_id = ?` to point existing Content rows
at the new Store. Then a sanity-scan (`SELECT COUNT(*) FROM content
WHERE store_id = ?`) to confirm.

### Adding a new backend

`StoreType` is an enum + inline switches — no strategy pattern today.
To add a fifth backend:

1. Add the enum value + capability flag (`isXStore()`) in
   `StoreType.java`.
2. Add a `case` branch in `Content.getContent()` (read path).
3. Add a `case` branch in `Content.setContentAux()` (write path).
4. If the backend needs lifecycle management (open handles, connection
   pools), model it on `KvStoreBackend`: static cache keyed by Store
   rootPath, shutdown hook to close all.

If the switch grows unwieldy, refactor to a `StoreBackend` strategy —
each `StoreType` maps to a `StoreBackend` bean; `Content` delegates to
`registry.forStoreType(type).read(...)`. Out of scope for the current
codebase but a natural next step if a sixth backend arrives.

## Building

```bash
mvn clean install
```

**Note:** This project currently has compilation issues due to dependencies on deleted modules (`hitorro-base` and `hitorro-util`). These need to be either:
1. Recreated as separate library projects
2. Refactored out of the codebase
3. Replaced with external library equivalents

## Known Issues

- References to `com.hitorro.util.*` packages (from deleted hitorro-util module)
- Missing type system utilities
- Missing base type classes

## Requirements

- Java 19 or higher
- Maven 3.6+

## Key Dependencies

- **Hibernate 6.4.4**: ORM framework
- **Apache Derby 10.17**: Embedded database
- **Apache Tapestry 5.4.3**: Web framework
- **Apache POI 5.2.5**: Office document processing
- **ROME 2.1.0**: RSS/Atom feed processing
- **JAudioTagger 3.0.1**: MP3 metadata extraction
- **Apache MINA 2.0.21**: Network application framework
- **JetS3t 0.9.4**: Amazon S3 integration
- **MongoDB Morphia 1.1.1**: MongoDB object mapping

## License

See LICENSE file for details.
