/*
 * Copyright (c) 2006-2026 Chris Collins
 */
package com.hitorro.base.objects;

import com.hitorro.kvstore.KVStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the DMS ↔ KVStore glue.
 *
 * <p>Where {@link KvStoreBackendTest} covers the low-level RocksDB
 * wrapper, this suite exercises {@link KvStoreBackend#writeContent}
 * — the EXACT code path that
 * {@code Content.setContentAux()}'s KVStore switch branch dispatches
 * to. Uses a real {@link Store} configured with rootPath +
 * {@code storeType="KVStore"}, then invokes {@code init()} manually
 * (bypasses the JPA @PostLoad trigger so no Hibernate is needed).</p>
 *
 * <p>What this suite proves that the low-level tests can't:</p>
 * <ul>
 *   <li>fileName is derived via {@code idToHexPath} (same convention
 *       as the File backend — a future migration tool wouldn't need to
 *       translate names)</li>
 *   <li>Publicly-visible Stores get the extension appended when the
 *       original filename has one</li>
 *   <li>Publicly-visible Stores DON'T get an extension appended for
 *       original names without one (avoid trailing-dot bug)</li>
 *   <li>Non-publicly-visible Stores never get an extension, even
 *       when the original has one</li>
 *   <li>WriteResult contains the same size as the actual bytes in
 *       RocksDB — Content.contentSize would be correct</li>
 *   <li>Store.init() correctly resolves rootDir for KVStore type
 *       (regression: previously only File / Unmanaged did)</li>
 * </ul>
 */
class KvStoreContentIntegrationTest {

    @AfterEach
    void tearDown() {
        KvStoreBackend.closeAll();
    }

    /** Build a Store configured for the KVStore backend, with rootDir
     *  pointing at a temp dir. Manually invokes init() the same way
     *  Hibernate's @PostLoad would after loading it from the DB. */
    private Store makeKvStore(Path rootDir, String name, boolean publiclyVisible) {
        Store s = new Store();
        s.setName(name);
        s.setStoreType("KVStore");
        // Prefix with "file:" so BaseFileSystem picks the file adapter;
        // bare paths fall through to DFS which fails in test contexts.
        s.setRootPath("file:" + rootDir.toString());
        s.setIsPubliclyVisible(publiclyVisible);
        s.init();
        return s;
    }

    private static InputStream bytes(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void storeInit_resolvesRootDir_forKvStoreType(@TempDir Path tmp) {
        Store s = makeKvStore(tmp, "s1", false);
        assertNotNull(s.getRootPathPath(),
                "Store.init() must resolve rootDir for KVStore — previously only File/Unmanaged");
        assertEquals(StoreType.KVStore, s.getStoreTypeType());
    }

    @Test
    void writeContent_derivesFileName_viaIdToHexPath(@TempDir Path tmp) throws Exception {
        Store s = makeKvStore(tmp, "s1", false);
        KvStoreBackend.WriteResult r = KvStoreBackend.writeContent(
                s, "original.pdf", /*assignedId=*/12345L, bytes("hello"));
        // idToHexPath(12345) → hex path (typically nested for filesystem
        // storage but the exact layout is the File backend's convention;
        // we assert it's non-blank and doesn't accidentally include the
        // original name.
        assertNotNull(r.fileName());
        assertFalse(r.fileName().isBlank());
        assertFalse(r.fileName().contains("original"),
                "fileName must NOT leak the original filename — that's the whole point of hex-encoding");
    }

    @Test
    void writeContent_publicStore_withExtension_appendsIt(@TempDir Path tmp) throws Exception {
        Store s = makeKvStore(tmp, "public-store", /*publiclyVisible=*/true);
        KvStoreBackend.WriteResult r = KvStoreBackend.writeContent(
                s, "doc.pdf", 42L, bytes("x"));
        assertTrue(r.fileName().endsWith(".pdf"),
                "publicly-visible store MUST append original extension so browsers can dispatch by MIME");
    }

    @Test
    void writeContent_publicStore_withoutExtension_noTrailingDot(@TempDir Path tmp) throws Exception {
        Store s = makeKvStore(tmp, "public-store", /*publiclyVisible=*/true);
        KvStoreBackend.WriteResult r = KvStoreBackend.writeContent(
                s, "README", 42L, bytes("x"));
        assertFalse(r.fileName().endsWith("."),
                "no extension on the original should not produce a trailing dot");
    }

    @Test
    void writeContent_privateStore_neverAppendsExtension(@TempDir Path tmp) throws Exception {
        Store s = makeKvStore(tmp, "private-store", /*publiclyVisible=*/false);
        KvStoreBackend.WriteResult r = KvStoreBackend.writeContent(
                s, "doc.pdf", 42L, bytes("x"));
        assertFalse(r.fileName().endsWith(".pdf"),
                "private store must NOT leak the extension — Content.getFileName is opaque");
    }

    @Test
    void writeContent_returnedSize_matchesBytesInKv(@TempDir Path tmp) throws Exception {
        Store s = makeKvStore(tmp, "s1", false);
        String payload = "twelve bytes"; // 12
        KvStoreBackend.WriteResult r = KvStoreBackend.writeContent(
                s, "irrelevant.txt", 42L, bytes(payload));
        assertEquals(12, r.size(),
                "WriteResult.size drives Content.contentSize — must equal the actual bytes written");

        // Read back through the get() path and assert content matches.
        InputStream got = KvStoreBackend.get(s, r.fileName());
        assertNotNull(got);
        assertEquals(payload, new String(got.readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void writeThenRead_roundTripsThroughRealStore(@TempDir Path tmp) throws Exception {
        // The full "operator scenario": write a file, forget everything
        // except the fileName, later a fresh caller reads it back.
        Store s = makeKvStore(tmp, "s1", false);
        KvStoreBackend.WriteResult r = KvStoreBackend.writeContent(
                s, "manuscript.md", 100L, bytes("# Hello"));

        // Simulate a Content.getContent() call using only the fileName
        // that the setter would have persisted.
        InputStream is = KvStoreBackend.get(s, r.fileName());
        assertNotNull(is);
        assertEquals("# Hello", new String(is.readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void twoDistinctStores_haveIndependentKvSpaces(@TempDir Path tmp) throws Exception {
        Store s1 = makeKvStore(tmp.resolve("s1"), "s1", false);
        Store s2 = makeKvStore(tmp.resolve("s2"), "s2", false);

        KvStoreBackend.WriteResult r1 = KvStoreBackend.writeContent(
                s1, "same.txt", 1L, bytes("from s1"));
        KvStoreBackend.WriteResult r2 = KvStoreBackend.writeContent(
                s2, "same.txt", 1L, bytes("from s2"));

        // Both derived fileNames may collide (same id → same hex path);
        // the underlying RocksDB dir differs so no cross-Store contamination.
        assertEquals(r1.fileName(), r2.fileName(),
                "same id + same publicly-visible flag → same hex fileName; isolation must come from Store dir");
        assertEquals("from s1", new String(
                KvStoreBackend.get(s1, r1.fileName()).readAllBytes(), StandardCharsets.UTF_8));
        assertEquals("from s2", new String(
                KvStoreBackend.get(s2, r2.fileName()).readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void deleteViaBackend_removesFromRealStore(@TempDir Path tmp) throws Exception {
        Store s = makeKvStore(tmp, "s1", false);
        KvStoreBackend.WriteResult r = KvStoreBackend.writeContent(
                s, "tmp.txt", 1L, bytes("delete-me"));
        assertNotNull(KvStoreBackend.get(s, r.fileName()));

        KvStoreBackend.delete(s, r.fileName());
        assertNull(KvStoreBackend.get(s, r.fileName()),
                "post-delete: get returns null (matches File backend's missing-file behaviour)");
    }

    @Test
    void forStore_reusesCachedKvHandle(@TempDir Path tmp) throws Exception {
        Store s = makeKvStore(tmp, "s1", false);
        KVStore first  = KvStoreBackend.forStore(s);
        KVStore second = KvStoreBackend.forStore(s);
        assertSame(first, second,
                "forStore must reuse the process-wide cached RocksDB handle — otherwise " +
                "concurrent Content ops would race on file-lock acquisition");
    }
}
