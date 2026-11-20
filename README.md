<p align="center">
  <img src="assets/logo.png" alt="Hitorro Basedms logo" width="180">
</p>

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

Backends plug in via the `StoreBackend` interface + `StoreBackendRegistry`.
`Content.getContent()` / `setContent()` dispatch through the registry
— no switch statements to modify. To add a sixth backend:

1. Add the enum value + capability flag (`isXStore()`) in
   `StoreType.java`.
2. Implement `StoreBackend`:
   ```java
   final class MyBackend implements StoreBackend {
       @Override public StoreType type() { return StoreType.MyType; }
       @Override public InputStream read(Store store, Content content) { … }
       @Override public boolean write(Store store, Content content,
                                      String originalFileName,
                                      InputStream is, long assignedId) { … }
   }
   ```
3. Register: `StoreBackendRegistry.register(new MyBackend())` — either
   in the registry's static init block (built-in) or from a
   ServiceLoader-loaded module (optional dep).
4. Lifecycle management (open handles, connection pools) — model on
   `KvStoreBackend`: static cache keyed by Store rootPath, JVM
   shutdown hook to close all cached handles.

Read-only backends (`Link`, `Unmanaged`) throw a `StoreException`
from `write` — see `LinkBackend.java` for the pattern.

**Module-split future:** `KvStoreBackend` + `KvStoreBackendAdapter` +
`StoreMigrator` + `StoreMigratorCli` are already adapter-shaped —
splitting into `hitorro-basedms-kvstore` is a mechanical file move
plus a ServiceLoader entry, no `basedms` code change required.

## Building

```bash
mvn clean install
```

**Note:** This project currently has compilation issues due to dependencies on deleted modules (`hitorro-base` and `hitorro-util`). These need to be either:
1. Recreated as separate library projects
2. Refactored out of the codebase
3. Replaced with external library equivalents

## Testing on PostgreSQL and MySQL

`mvn test` runs the Hibernate tests on in-memory H2. The same tests also run on PostgreSQL 17
and MySQL 8.4 in containers (Testcontainers): `DMSSessionHibernate7*Test`,
`VersionTreeDelete*Test` and `DmsSchema*Test` (every basedms entity: schema creation and
validation, a blob round trip, the native SQL). Without Docker those classes are skipped and the
build still passes. With Rancher Desktop:

```bash
DOCKER_HOST=unix://$HOME/.rd/docker.sock TESTCONTAINERS_RYUK_DISABLED=true mvn test
```

A matrix class extends the H2 test and overrides `openDatabase()` (see `TestSessionFactory` and
`TestDatabases`). Native SQL must name tables exactly as their `@Table` does: MySQL on Linux
compares table names case-sensitively.

### Index names

Index names are unique per table (`<table>_<column>_idx`, e.g. `user_t_name_idx`,
`content_guid_idx`): PostgreSQL and H2 scope index names to the schema, so before 3.1.2 the shared
`name_idx` / `guid_idx` were created on the first table only. With `hbm2ddl=update`, an existing
database gets the new indexes added; an existing MySQL database (where index names are per table)
also keeps its old `name_idx` / `guid_idx` indexes, which are now redundant and can be dropped.

## Two SessionFactories in a Spring Boot app

An app that lists `com.hitorro.basedms.db.HibernateService` under `services` runs basedms's own
SessionFactory beside Spring's on the same database. HibernateService only sees the properties
`DatabaseConfigProvider` gives it (hitorro-spring-boot passes dialect, driver and `hbm2ddl`), not
`spring.jpa.*`. Keep the two on the same identifiers:

- `spring.jpa.hibernate.naming.physical-strategy: org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl`
  (Spring Boot's default, camelCase to snake_case, gives different column names);
- no `hibernate.globally_quoted_identifiers` (with it, Spring creates quoted mixed-case names such
  as `"commitCount"` and `"Extensions"`, which basedms's unquoted SQL does not find on PostgreSQL).

`NamingConsistencyTest` checks that these settings give basedms's entities the same tables and
columns as Hibernate's defaults. Without `HibernateService` in `services`, hitorro-spring-boot's
`DMSAutoConfiguration` hands Spring's own SessionFactory to basedms and there is only one.

## Known Issues

- References to `com.hitorro.util.*` packages (from deleted hitorro-util module)
- Missing type system utilities
- Missing base type classes

## Requirements

- Java 19 or higher
- Maven 3.6+

## Key Dependencies

- **Hibernate 7.4** (Jakarta Persistence 3.2): ORM framework. Since 3.1.0, `DMSSession.saveOrUpdate/update` persist new objects and merge managed or detached ones (Hibernate 7 removed re-attachment), and the BeforeSave callbacks run from a MERGE listener.
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
