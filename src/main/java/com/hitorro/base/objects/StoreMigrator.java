/*
 * Copyright (c) 2006-2026 Chris Collins
 */
package com.hitorro.base.objects;

import com.hitorro.kvstore.KVStore;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.io.IOUtil;
import com.hitorro.util.io.StoreException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Bi-directional bulk copy between two DMS {@link Store}s of different
 * {@link StoreType}s. Both {@link StoreType#File} and
 * {@link StoreType#KVStore} back-ends deliberately use the same
 * {@code FileUtil.idToHexPath} naming convention, so migration is a
 * pure byte-copy — no fileName translation, no id remapping.
 *
 * <p>Only the File ↔ KVStore pairs are implemented here; adding
 * Blob support would need JDBC session plumbing that lives elsewhere.</p>
 *
 * <p>Not transactional — a partial copy leaves both stores populated
 * with the successfully-migrated subset. On a failure mid-migration,
 * consult {@link Report#errors()} and re-run (idempotent: puts
 * overwrite; already-copied contents are equal-byte no-ops).</p>
 *
 * <p>Does NOT touch {@link Content} database rows or update Store
 * pointers on them — that's an application-level concern (usually a
 * SQL UPDATE of {@code content.store_id} + a sanity-scan). This class
 * just moves bytes.</p>
 */
public final class StoreMigrator {

    private StoreMigrator() {}

    /** Outcome of a migration — counts + first-N error snapshots for
     *  the operator to inspect. */
    public record Report(
            int copied,
            int skipped,
            List<String> errors) { }

    /** Copy every content from {@code source} to {@code dest}. Both
     *  Stores must be initialised (Store.init() run). Direction is
     *  inferred from the store types; unsupported pairs throw. */
    public static Report migrate(Store source, Store dest) throws StoreException {
        StoreType from = source.getStoreTypeType();
        StoreType to = dest.getStoreTypeType();
        if (from == StoreType.File && to == StoreType.KVStore) {
            return fileToKv(source, dest);
        }
        if (from == StoreType.KVStore && to == StoreType.File) {
            return kvToFile(source, dest);
        }
        if (from == StoreType.KVStore && to == StoreType.KVStore) {
            return kvToKv(source, dest);
        }
        if (from == StoreType.File && to == StoreType.File) {
            return fileToFile(source, dest);
        }
        throw new StoreException("StoreMigrator: unsupported pair " + from + " → " + to
                + " (supported: File↔KVStore, File→File, KVStore→KVStore)");
    }

    // ---- File → KVStore ----

    private static Report fileToKv(Store source, Store dest) throws StoreException {
        List<String> errors = new ArrayList<>();
        int[] counters = new int[]{0, 0};   // [copied, skipped]
        BaseFile root = source.getRootPathPath();
        if (root == null || !root.exists()) return new Report(0, 0, errors);

        try {
            walk(root, root, (relativeFileName, sourceFile) -> {
                try (InputStream is = sourceFile.getDataInputStream()) {
                    KvStoreBackend.put(dest, relativeFileName, is);
                    counters[0]++;
                } catch (Exception e) {
                    errors.add(relativeFileName + ": " + e.getMessage());
                    counters[1]++;
                }
            });
        } catch (IOException e) {
            throw new StoreException("StoreMigrator: walk of source failed: " + e.getMessage());
        }
        return new Report(counters[0], counters[1], errors);
    }

    // ---- KVStore → File ----

    private static Report kvToFile(Store source, Store dest) throws StoreException {
        List<String> errors = new ArrayList<>();
        int copied = 0, skipped = 0;
        KVStore kv = KvStoreBackend.forStore(source);
        Iterator<Map.Entry<byte[], byte[]>> it = kv.scanByPrefixWithKeys(
                "content:".getBytes(StandardCharsets.UTF_8));
        BaseFile destRoot = dest.getRootPathPath();
        while (it.hasNext()) {
            Map.Entry<byte[], byte[]> e = it.next();
            String fileName = new String(e.getKey(), StandardCharsets.UTF_8)
                    .substring("content:".length());
            try {
                BaseFile out = destRoot.getChild(fileName);
                if (!out.mkParentDir()) {
                    throw new IOException("mkParentDir failed for " + out.getAbsolutePath());
                }
                try (OutputStream os = out.getDataOutputStream()) {
                    IOUtil.copyStream(new ByteArrayInputStream(e.getValue()), os);
                    os.flush();
                }
                copied++;
            } catch (Exception ex) {
                errors.add(fileName + ": " + ex.getMessage());
                skipped++;
            }
        }
        return new Report(copied, skipped, errors);
    }

    // ---- Same-type copies (utility — mostly for tests + backup ops) ----

    private static Report kvToKv(Store source, Store dest) throws StoreException {
        List<String> errors = new ArrayList<>();
        int copied = 0, skipped = 0;
        KVStore src = KvStoreBackend.forStore(source);
        KVStore dst = KvStoreBackend.forStore(dest);
        Iterator<Map.Entry<byte[], byte[]>> it = src.scanByPrefixWithKeys(
                "content:".getBytes(StandardCharsets.UTF_8));
        while (it.hasNext()) {
            var e = it.next();
            var res = dst.put(e.getKey(), e.getValue());
            if (res.isSuccess()) copied++;
            else { skipped++; errors.add(new String(e.getKey(), StandardCharsets.UTF_8)
                    + ": " + res.getError().orElse("unknown")); }
        }
        return new Report(copied, skipped, errors);
    }

    private static Report fileToFile(Store source, Store dest) throws StoreException {
        List<String> errors = new ArrayList<>();
        int[] counters = new int[]{0, 0};
        BaseFile root = source.getRootPathPath();
        if (root == null || !root.exists()) return new Report(0, 0, errors);
        try {
            walk(root, root, (relativeFileName, sourceFile) -> {
                try (InputStream is = sourceFile.getDataInputStream()) {
                    BaseFile out = dest.getRootPathPath().getChild(relativeFileName);
                    if (!out.mkParentDir()) throw new IOException("mkParentDir failed");
                    try (OutputStream os = out.getDataOutputStream()) {
                        IOUtil.copyStream(is, os);
                        os.flush();
                    }
                    counters[0]++;
                } catch (Exception e) {
                    errors.add(relativeFileName + ": " + e.getMessage());
                    counters[1]++;
                }
            });
        } catch (IOException e) {
            throw new StoreException("StoreMigrator: walk of source failed: " + e.getMessage());
        }
        return new Report(counters[0], counters[1], errors);
    }

    // ---- Helpers ----

    /** Recursively walk {@code dir}, calling {@code visitor} for every
     *  regular file with (relativePath-from-root, file). Skips the
     *  rocksdb/ subdir so File→KV round-trips don't accidentally suck
     *  in a co-resident RocksDB. */
    private static void walk(BaseFile root, BaseFile dir, FileVisitor visitor) throws IOException {
        BaseFile[] children = dir.listFiles();
        if (children == null) return;
        for (BaseFile c : children) {
            if (c.isDir()) {
                if ("rocksdb".equals(c.getName())) continue;   // hands off
                walk(root, c, visitor);
            } else {
                String rel = relativePath(root, c);
                visitor.visit(rel, c);
            }
        }
    }

    /** Compute a source-relative path that PRESERVES the leading "/"
     *  because DMS filenames (from {@code FileUtil.idToHexPath}) start
     *  with one — e.g. {@code /00/00/…/64}. Stripping it here would
     *  break byte-for-byte round-trip: seedFile → migrate → read on
     *  the destination would look under {@code content:00/…} while
     *  the DMS setter keyed under {@code content:/00/…}. */
    private static String relativePath(BaseFile root, BaseFile child) {
        String rootPath = root.getAbsolutePath();
        String childPath = child.getAbsolutePath();
        if (childPath.startsWith(rootPath)) {
            String rel = childPath.substring(rootPath.length());
            return rel.isEmpty() ? "/" : (rel.startsWith("/") ? rel : "/" + rel);
        }
        return "/" + child.getName();
    }

    @FunctionalInterface
    private interface FileVisitor {
        void visit(String relativeFileName, BaseFile file);
    }
}
