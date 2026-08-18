/*
 * Copyright (c) 2006-2026 Chris Collins
 */
package com.hitorro.base.objects;

import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.io.IOUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link StoreMigrator}. Uses real Stores of both types
 * (init-called-manually pattern from {@link KvStoreContentIntegrationTest}).
 * Exercises every supported direction + validates the byte-level
 * round-trip that makes File↔KVStore migration usable in production.
 */
class StoreMigratorTest {

    @AfterEach
    void tearDown() {
        KvStoreBackend.closeAll();
    }

    private static Store newStore(String type, Path root, String name) {
        Store s = new Store();
        s.setName(name);
        s.setStoreType(type);
        s.setRootPath("file:" + root);
        s.setIsPubliclyVisible(false);
        s.init();
        return s;
    }

    /** Seed a File-backed Store by writing content directly through the
     *  Content path (bypassing the switch — same as writing via any
     *  other setContent call in production). */
    private static String seedFile(Store store, long id, String payload) throws Exception {
        String fn = com.hitorro.util.io.FileUtil.idToHexPath(id);
        BaseFile out = store.getRootPathPath().getChild(fn);
        assertTrue(out.mkParentDir());
        try (OutputStream os = out.getDataOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }
        return fn;
    }

    private static String seedKv(Store store, long id, String payload) throws Exception {
        return KvStoreBackend.writeContent(
                store, "irrelevant.txt", id, in(payload)).fileName();
    }

    private static InputStream in(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String readFileBytes(Store store, String fileName) throws Exception {
        BaseFile f = store.getRootPathPath().getChild(fileName);
        try (InputStream is = f.getDataInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String readKvBytes(Store store, String fileName) throws Exception {
        try (InputStream is = KvStoreBackend.get(store, fileName)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ---- File → KVStore ----

    @Test
    void fileToKv_copiesEveryContent(@TempDir Path tmp) throws Exception {
        Store src = newStore("File",    tmp.resolve("src"), "src");
        Store dst = newStore("KVStore", tmp.resolve("dst"), "dst");

        String a = seedFile(src, 100L, "alpha");
        String b = seedFile(src, 200L, "beta");
        String c = seedFile(src, 300L, "gamma");

        StoreMigrator.Report r = StoreMigrator.migrate(src, dst);
        assertEquals(3, r.copied());
        assertEquals(0, r.skipped());
        assertTrue(r.errors().isEmpty());

        assertEquals("alpha", readKvBytes(dst, a));
        assertEquals("beta",  readKvBytes(dst, b));
        assertEquals("gamma", readKvBytes(dst, c));
    }

    @Test
    void fileToKv_emptySource_reportsZeroCopied(@TempDir Path tmp) throws Exception {
        Store src = newStore("File",    tmp.resolve("src"), "src");
        Store dst = newStore("KVStore", tmp.resolve("dst"), "dst");
        // No seed — source is empty. Migrator must handle gracefully.
        StoreMigrator.Report r = StoreMigrator.migrate(src, dst);
        assertEquals(0, r.copied());
        assertEquals(0, r.skipped());
    }

    @Test
    void fileToKv_skipsRocksdbSubdir_forInPlaceRoundTrip(@TempDir Path tmp) throws Exception {
        // Corner: source is a File store, but shares its rootPath with
        // a KVStore that was tried earlier (rocksdb/ subdir present).
        // Migrator must NOT try to slurp the RocksDB files as content.
        Store src = newStore("File", tmp, "src");
        seedFile(src, 100L, "the-real-content");
        // Simulate a stray rocksdb/ subdir
        BaseFile rocks = src.getRootPathPath().getChild("rocksdb");
        assertTrue(rocks.mkdir());
        BaseFile stray = rocks.getChild("SST_00001.sst");
        try (OutputStream os = stray.getDataOutputStream()) {
            os.write("would-be-corrupt-if-slurped".getBytes(StandardCharsets.UTF_8));
        }

        Store dst = newStore("KVStore", tmp.resolve("dst"), "dst");
        StoreMigrator.Report r = StoreMigrator.migrate(src, dst);
        // Only the one real content — SST_00001.sst was walked over.
        assertEquals(1, r.copied());
    }

    // ---- KVStore → File ----

    @Test
    void kvToFile_copiesEveryContent(@TempDir Path tmp) throws Exception {
        Store src = newStore("KVStore", tmp.resolve("src"), "src");
        Store dst = newStore("File",    tmp.resolve("dst"), "dst");

        String a = seedKv(src, 100L, "alpha");
        String b = seedKv(src, 200L, "beta");

        StoreMigrator.Report r = StoreMigrator.migrate(src, dst);
        assertEquals(2, r.copied());
        assertTrue(r.errors().isEmpty());

        assertEquals("alpha", readFileBytes(dst, a));
        assertEquals("beta",  readFileBytes(dst, b));
    }

    @Test
    void kvToFile_emptySource_reportsZeroCopied(@TempDir Path tmp) throws Exception {
        Store src = newStore("KVStore", tmp.resolve("src"), "src");
        Store dst = newStore("File",    tmp.resolve("dst"), "dst");
        StoreMigrator.Report r = StoreMigrator.migrate(src, dst);
        assertEquals(0, r.copied());
    }

    // ---- Same-type copies ----

    @Test
    void kvToKv_copiesEveryContent(@TempDir Path tmp) throws Exception {
        Store src = newStore("KVStore", tmp.resolve("src"), "src");
        Store dst = newStore("KVStore", tmp.resolve("dst"), "dst");
        seedKv(src, 1L, "x");
        seedKv(src, 2L, "y");
        StoreMigrator.Report r = StoreMigrator.migrate(src, dst);
        assertEquals(2, r.copied());
    }

    @Test
    void fileToFile_copiesEveryContent(@TempDir Path tmp) throws Exception {
        Store src = newStore("File", tmp.resolve("src"), "src");
        Store dst = newStore("File", tmp.resolve("dst"), "dst");
        seedFile(src, 1L, "x");
        seedFile(src, 2L, "y");
        StoreMigrator.Report r = StoreMigrator.migrate(src, dst);
        assertEquals(2, r.copied());
    }

    // ---- Guards ----

    @Test
    void unsupportedPair_throws(@TempDir Path tmp) {
        Store src = newStore("File", tmp.resolve("src"), "src");
        Store blob = new Store();
        blob.setName("blob");
        blob.setStoreType("Blob");
        blob.init();
        assertThrows(com.hitorro.util.io.StoreException.class,
                () -> StoreMigrator.migrate(src, blob),
                "Blob migration isn't in scope (needs JDBC session plumbing)");
    }

    // ---- Round-trip preservation ----

    @Test
    void roundTrip_fileToKvToFile_preservesBytes(@TempDir Path tmp) throws Exception {
        Store fileA = newStore("File",    tmp.resolve("fileA"), "fileA");
        Store kv    = newStore("KVStore", tmp.resolve("kv"),    "kv");
        Store fileB = newStore("File",    tmp.resolve("fileB"), "fileB");

        String fn = seedFile(fileA, 999L, "the-original-payload");
        assertEquals(1, StoreMigrator.migrate(fileA, kv).copied());
        assertEquals(1, StoreMigrator.migrate(kv,    fileB).copied());

        assertEquals("the-original-payload", readFileBytes(fileB, fn));
    }
}
