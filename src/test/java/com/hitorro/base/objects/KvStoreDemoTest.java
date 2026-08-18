/*
 * Copyright (c) 2006-2026 Chris Collins
 */
package com.hitorro.base.objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Living-documentation walkthrough of the {@link StoreType#KVStore}
 * backend. Each {@code @DisplayName} reads as a step the operator
 * would take; each method body shows the code that step maps to.
 * Skim top-to-bottom to see the whole lifecycle without hunting
 * across the class-tree.
 *
 * <p>Companion to the "Storage backends" section in {@code README.md}
 * — if you change how KVStore is used, update both.</p>
 */
class KvStoreDemoTest {

    @AfterEach
    void tearDown() {
        // Release every cached RocksDB handle so the @TempDir cleaner
        // isn't fighting a file lock.
        KvStoreBackend.closeAll();
    }

    @Test
    @DisplayName("1. Create a KVStore-backed Store")
    void step1_createStore(@TempDir Path tmp) {
        // Store is a normal @Entity POJO — in production, Hibernate
        // would load one from the DB. In tests / embedded contexts,
        // construct + call init() manually.
        Store store = new Store();
        store.setName("attachments");
        store.setStoreType("KVStore");                // one of Blob / File / Unmanaged / Link / KVStore
        store.setRootPath("file:" + tmp);             // where the rocksdb/ subdir will live
        store.setIsPubliclyVisible(false);             // affects fileName's extension policy
        store.init();

        assertEquals(StoreType.KVStore, store.getStoreTypeType());
        assertNotNull(store.getRootPathPath());
    }

    @Test
    @DisplayName("2. Write a content byte-stream — DMS assigns the fileName")
    void step2_writeContent(@TempDir Path tmp) throws Exception {
        Store store = newStore(tmp, "attachments", false);
        InputStream bytes = new ByteArrayInputStream("Hello, KV!".getBytes(StandardCharsets.UTF_8));

        // In prod, Content.setContentAux calls this with a fresh id
        // from ContentIdNamedLong. In tests we pass an id directly.
        KvStoreBackend.WriteResult result = KvStoreBackend.writeContent(
                store, "greeting.txt", /*assignedId=*/1001L, bytes);

        // Callers persist result.fileName() on the Content object;
        // it's opaque (hex-encoded) — the original name isn't leaked.
        assertNotNull(result.fileName());
        assertFalse(result.fileName().contains("greeting"),
                "fileName is hex-encoded — original name never leaks for private stores");
        // result.size() is what Content.contentSize should be set to.
        assertEquals(10, result.size());
    }

    @Test
    @DisplayName("3. Read the bytes back using the fileName the setter returned")
    void step3_readContent(@TempDir Path tmp) throws Exception {
        Store store = newStore(tmp, "attachments", false);
        KvStoreBackend.WriteResult wr = KvStoreBackend.writeContent(
                store, "greeting.txt", 1001L, in("Hello, KV!"));

        // In prod, Content.getContent() looks like:
        //   return KvStoreBackend.get(store, this.getFileName());
        InputStream got = KvStoreBackend.get(store, wr.fileName());
        assertNotNull(got);
        assertEquals("Hello, KV!", new String(got.readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("4. Delete a content — get returns null after (matches File backend)")
    void step4_deleteContent(@TempDir Path tmp) throws Exception {
        Store store = newStore(tmp, "attachments", false);
        KvStoreBackend.WriteResult wr = KvStoreBackend.writeContent(
                store, "tmp.txt", 42L, in("delete me"));

        KvStoreBackend.delete(store, wr.fileName());
        assertNull(KvStoreBackend.get(store, wr.fileName()),
                "get() on a deleted key returns null (matches File backend's missing-file behaviour)");
    }

    @Test
    @DisplayName("5. Publicly-visible Stores preserve the original extension")
    void step5_publiclyVisible_extensionPolicy(@TempDir Path tmp) throws Exception {
        Store publicStore  = newStore(tmp.resolve("pub"),   "public",  /*publiclyVisible=*/true);
        Store privateStore = newStore(tmp.resolve("priv"),  "private", /*publiclyVisible=*/false);

        String orig = "manuscript.pdf";
        var pubRes  = KvStoreBackend.writeContent(publicStore,  orig, 1L, in("x"));
        var privRes = KvStoreBackend.writeContent(privateStore, orig, 1L, in("x"));

        assertTrue(pubRes.fileName().endsWith(".pdf"),
                "publicly-visible: extension appended so browsers MIME-dispatch");
        assertFalse(privRes.fileName().endsWith(".pdf"),
                "private: filename stays opaque");
    }

    @Test
    @DisplayName("6. Multiple Stores on the same host don't cross-contaminate")
    void step6_multiStoreIsolation(@TempDir Path tmp) throws Exception {
        Store users     = newStore(tmp.resolve("users"),     "users",     false);
        Store thumbs    = newStore(tmp.resolve("thumbs"),    "thumbs",    false);

        // Same id → same fileName across Stores (isolation is at the
        // RocksDB-directory level, not at the key level).
        var u = KvStoreBackend.writeContent(users,  "profile.jpg", 100L, in("user-bytes"));
        var t = KvStoreBackend.writeContent(thumbs, "profile.jpg", 100L, in("thumb-bytes"));
        assertEquals(u.fileName(), t.fileName());
        assertEquals("user-bytes",
                new String(KvStoreBackend.get(users,  u.fileName()).readAllBytes(), StandardCharsets.UTF_8));
        assertEquals("thumb-bytes",
                new String(KvStoreBackend.get(thumbs, t.fileName()).readAllBytes(), StandardCharsets.UTF_8));
    }

    // ---- helpers ----

    private static Store newStore(Path root, String name, boolean publiclyVisible) {
        Store s = new Store();
        s.setName(name);
        s.setStoreType("KVStore");
        s.setRootPath("file:" + root);
        s.setIsPubliclyVisible(publiclyVisible);
        s.init();
        return s;
    }

    private static InputStream in(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}
