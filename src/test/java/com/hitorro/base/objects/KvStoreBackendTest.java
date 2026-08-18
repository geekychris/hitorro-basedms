/*
 * Copyright (c) 2006-2026 Chris Collins
 */
package com.hitorro.base.objects;

import com.hitorro.kvstore.KVStore;
import com.hitorro.kvstore.RocksDBStore;
import com.hitorro.kvstore.DatabaseConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the DMS RocksDB backend. Uses {@code TempDir} so each
 * test gets a fresh RocksDB directory; closes the handle explicitly in
 * {@link #tearDown} so the temp dir cleans up without file-lock races.
 *
 * <p>The tests exercise {@link KvStoreBackend}'s package-private raw
 * overloads directly so we don't need a fully-initialised DMS
 * {@code Store} (which drags Hibernate). The Store-taking overloads
 * are thin delegates to the same code paths.</p>
 */
class KvStoreBackendTest {

    /** Fresh RocksDB opened per test — closed in tearDown. */
    private KVStore kv;

    @AfterEach
    void tearDown() throws Exception {
        if (kv != null) kv.close();
        // Also clear the process-wide cache so subsequent tests don't
        // reuse an old handle.
        KvStoreBackend.closeAll();
    }

    private KVStore openFresh(Path dir) throws Exception {
        Files.createDirectories(dir);
        DatabaseConfig cfg = DatabaseConfig.builder(dir.toString())
                .createIfMissing(true).enableWAL(true).build();
        kv = new RocksDBStore(cfg);
        return kv;
    }

    private static InputStream bytes(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String readAll(InputStream is) throws IOException {
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void putThenGet_roundTripsBytes(@TempDir Path tmp) throws Exception {
        KVStore kv = openFresh(tmp);
        long written = KvStoreBackend.putRaw(kv, "hex/a1", bytes("hello world"));
        assertEquals(11, written);

        InputStream is = KvStoreBackend.getRaw(kv, "hex/a1");
        assertNotNull(is);
        assertEquals("hello world", readAll(is));
    }

    @Test
    void getAbsent_returnsNull(@TempDir Path tmp) throws Exception {
        KVStore kv = openFresh(tmp);
        assertNull(KvStoreBackend.getRaw(kv, "not-there"));
    }

    @Test
    void multipleContents_dontCollide(@TempDir Path tmp) throws Exception {
        KVStore kv = openFresh(tmp);
        KvStoreBackend.putRaw(kv, "file-1", bytes("first"));
        KvStoreBackend.putRaw(kv, "file-2", bytes("second"));
        KvStoreBackend.putRaw(kv, "file-3", bytes("third"));

        assertEquals("first", readAll(KvStoreBackend.getRaw(kv, "file-1")));
        assertEquals("second", readAll(KvStoreBackend.getRaw(kv, "file-2")));
        assertEquals("third", readAll(KvStoreBackend.getRaw(kv, "file-3")));
    }

    @Test
    void put_overwritesExistingKey(@TempDir Path tmp) throws Exception {
        KVStore kv = openFresh(tmp);
        KvStoreBackend.putRaw(kv, "file-1", bytes("v1"));
        KvStoreBackend.putRaw(kv, "file-1", bytes("v2-longer"));
        assertEquals("v2-longer", readAll(KvStoreBackend.getRaw(kv, "file-1")));
    }

    @Test
    void delete_removesKey_getReturnsNull(@TempDir Path tmp) throws Exception {
        KVStore kv = openFresh(tmp);
        KvStoreBackend.putRaw(kv, "victim", bytes("bye"));
        assertNotNull(KvStoreBackend.getRaw(kv, "victim"));

        KvStoreBackend.deleteRaw(kv, "victim");
        assertNull(KvStoreBackend.getRaw(kv, "victim"));
    }

    @Test
    void delete_ofAbsentKey_isSafe(@TempDir Path tmp) throws Exception {
        KVStore kv = openFresh(tmp);
        // RocksDB delete of a non-existent key returns success — no throw.
        KvStoreBackend.deleteRaw(kv, "never-was");
    }

    @Test
    void keysArePrefixed_soContent_doesntCollideWithFutureRowKinds(
            @TempDir Path tmp) throws Exception {
        KVStore kv = openFresh(tmp);
        KvStoreBackend.putRaw(kv, "same-name", bytes("via-content-prefix"));

        // Directly put a bare "same-name" key — proves the backend's
        // "content:" prefix isolates it. This validates the design
        // choice, not the code path.
        kv.put("same-name".getBytes(StandardCharsets.UTF_8),
               "bare-key".getBytes(StandardCharsets.UTF_8));

        assertEquals("via-content-prefix",
                readAll(KvStoreBackend.getRaw(kv, "same-name")));
    }

    @Test
    void largeContent_roundTrips(@TempDir Path tmp) throws Exception {
        KVStore kv = openFresh(tmp);
        // 1 MB of pseudorandom bytes — big enough to exercise buffered
        // copy path in putRaw, still tiny for RocksDB.
        byte[] big = new byte[1_000_000];
        new Random(42).nextBytes(big);

        KvStoreBackend.putRaw(kv, "big", new ByteArrayInputStream(big));
        InputStream is = KvStoreBackend.getRaw(kv, "big");
        byte[] read = is.readAllBytes();
        assertArrayEquals(big, read);
    }

    @Test
    void forRootPath_cachesHandleAcrossCalls(@TempDir Path tmp) throws Exception {
        // Two "opens" against the same rootPath should return the SAME
        // KVStore instance. Proves the process-wide cache is working —
        // otherwise every put/get would open a new RocksDB and hit
        // "already locked" errors immediately.
        var ffs = new com.hitorro.util.basefile.fs.file.FileFileSystem(tmp.toFile());
        var bf = ffs.getFile(".");
        KVStore first  = KvStoreBackend.forRootPath(bf, "store-a");
        KVStore second = KvStoreBackend.forRootPath(bf, "store-a");
        assertSame(first, second);
    }

    @Test
    void closeAll_dropsCachedHandles_soReopenWorks(@TempDir Path tmp) throws Exception {
        var ffs = new com.hitorro.util.basefile.fs.file.FileFileSystem(tmp.toFile());
        var bf = ffs.getFile(".");
        KVStore first = KvStoreBackend.forRootPath(bf, "store-a");
        assertNotNull(first);

        KvStoreBackend.closeAll();

        // Post-close: a fresh open must work — closeAll cleared the cache
        // AND released the RocksDB file lock.
        KVStore reopened = KvStoreBackend.forRootPath(bf, "store-a");
        assertNotNull(reopened);
        assertNotSame(first, reopened);
    }

    @Test
    void storeType_KVStore_flagIsSetCorrectly() {
        // Reflection-free sanity check on the enum extension.
        assertTrue(StoreType.KVStore.isKvStore());
        assertFalse(StoreType.KVStore.isFileStore());
        assertFalse(StoreType.KVStore.isBlobStore());
        assertFalse(StoreType.KVStore.isLinkStore());
        assertFalse(StoreType.File.isKvStore());
        assertFalse(StoreType.Blob.isKvStore());
        // Enum lookup by name (used by Store to resolve StoreType from
        // its persisted storeType string) still works with the new value.
        assertEquals(StoreType.KVStore, StoreType.get("kvstore"));
    }
}
