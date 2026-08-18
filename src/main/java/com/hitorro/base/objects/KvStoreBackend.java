/*
 * Copyright (c) 2006-2026 Chris Collins
 */
package com.hitorro.base.objects;

import com.hitorro.kvstore.DatabaseConfig;
import com.hitorro.kvstore.KVStore;
import com.hitorro.kvstore.RocksDBStore;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.io.IOUtil;
import com.hitorro.util.io.StoreException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges the DMS {@link StoreType#KVStore} backend to
 * {@link com.hitorro.kvstore.RocksDBStore}. One RocksDB instance
 * per DMS {@link Store}, held under
 * {@code <Store.rootPath>/rocksdb/} — instances cache in a
 * process-wide map so multiple Content ops against the same Store
 * share a single open handle. A JVM shutdown hook closes them all
 * on process exit.
 *
 * <p>Key layout: {@code content:<fileName>} where {@code fileName}
 * is the hex-encoded content id that the DMS already assigns
 * (matches the File backend's on-disk naming). Values are the raw
 * content bytes — no framing, no compression beyond RocksDB's own.</p>
 *
 * <p>Not intended for very large contents — RocksDB values load
 * whole into memory on read. Anything over a few MB should stay on
 * the File backend. A future streaming/chunked layout can layer on
 * top of the same StoreType without a schema change.</p>
 */
public final class KvStoreBackend {

    /** Sub-directory under {@code Store.rootPath} that holds the RocksDB
     *  files. Isolated so a Store that later switches to File doesn't
     *  step on the KV data. */
    private static final String KV_SUBDIR = "rocksdb";

    /** Key prefix — leaves room to add other row kinds (index metadata,
     *  version pointers, etc.) later without a migration. */
    private static final String KEY_PREFIX = "content:";

    /** Process-wide cache: normalized rootPath → open KVStore. */
    private static final ConcurrentHashMap<String, KVStore> INSTANCES = new ConcurrentHashMap<>();

    static {
        // Best-effort shutdown so we don't leak RocksDB file locks on
        // graceful exits. Not fatal if the JVM aborts — RocksDB
        // recovers via its WAL on next open.
        Runtime.getRuntime().addShutdownHook(new Thread(KvStoreBackend::closeAll,
                "kv-store-backend-shutdown"));
    }

    private KvStoreBackend() {}

    /** Get (or open on first call) the KVStore for the given DMS Store. */
    public static KVStore forStore(Store store) throws StoreException {
        BaseFile rootPath = store.getRootPathPath();
        if (rootPath == null) {
            throw new StoreException("Store " + store.getName()
                    + " has no rootPath — KVStore backend needs a filesystem location");
        }
        return forRootPath(rootPath, store.getName());
    }

    /** Test/embedding overload — takes the rootPath + label directly so
     *  callers don't need a full Store (avoids the Hibernate init chain). */
    public static KVStore forRootPath(BaseFile rootPath, String label) {
        BaseFile kvDir = rootPath.getChild(KV_SUBDIR);
        String key = kvDir.getAbsolutePath();
        return INSTANCES.computeIfAbsent(key, k -> openRocksDb(k, label));
    }

    private static KVStore openRocksDb(String path, String storeName) {
        try {
            // Ensure parent exists; RocksDB createIfMissing handles its
            // own directory but the containing folder still has to be
            // makeable when the DMS is bootstrapped fresh.
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(path));
            DatabaseConfig cfg = DatabaseConfig.builder(path)
                    .createIfMissing(true)
                    .enableWAL(true)
                    .build();
            return new RocksDBStore(cfg);
        } catch (Exception e) {
            throw new RuntimeException("KvStoreBackend: cannot open RocksDB at " + path
                    + " for DMS Store " + storeName + ": " + e.getMessage(), e);
        }
    }

    /** Fully-qualified RocksDB key for a content filename. */
    private static byte[] key(String fileName) {
        return (KEY_PREFIX + fileName).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Copy every byte of {@code is} into the KVStore under
     * {@code content:<fileName>}. Returns the total byte count written
     * so the caller can populate {@code Content.contentSize}.
     */
    public static long put(Store store, String fileName, InputStream is)
            throws StoreException, IOException {
        return putRaw(forStore(store), fileName, is);
    }

    /** Test overload — accepts a caller-supplied KVStore directly. */
    static long putRaw(KVStore kv, String fileName, InputStream is)
            throws StoreException, IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        IOUtil.copyStream(is, buf);
        byte[] bytes = buf.toByteArray();
        var res = kv.put(key(fileName), bytes);
        if (!res.isSuccess()) {
            throw new StoreException("KvStoreBackend: put failed for "
                    + fileName + ": " + res.getError().orElse("unknown"));
        }
        return bytes.length;
    }

    /**
     * Open a stream for reading the content bytes back. Returns
     * {@code null} if the key isn't present so the DMS caller can
     * treat missing content the same way it does for File-backed
     * stores.
     */
    public static InputStream get(Store store, String fileName) throws StoreException {
        return getRaw(forStore(store), fileName);
    }

    /** Test overload. */
    static InputStream getRaw(KVStore kv, String fileName) throws StoreException {
        var res = kv.get(key(fileName));
        // RocksDBStore returns Result.failure("Key not found") for absent
        // keys — the DMS caller expects null (matches the File backend's
        // "no such file" behaviour). Only translate real IO failures
        // into StoreException.
        if (!res.isSuccess()) {
            String err = res.getError().orElse("");
            if (err.equalsIgnoreCase("Key not found")) return null;
            throw new StoreException("KvStoreBackend: get failed for "
                    + fileName + ": " + err);
        }
        Optional<byte[]> value = res.getValue();
        if (value.isEmpty() || value.get() == null) return null;
        return new ByteArrayInputStream(value.get());
    }

    /** Remove a content key. No-op when absent. */
    public static void delete(Store store, String fileName) throws StoreException {
        deleteRaw(forStore(store), fileName);
    }

    /** Test overload. */
    static void deleteRaw(KVStore kv, String fileName) throws StoreException {
        var res = kv.delete(key(fileName));
        if (!res.isSuccess()) {
            throw new StoreException("KvStoreBackend: delete failed for "
                    + fileName + ": " + res.getError().orElse("unknown"));
        }
    }

    /** Close every cached KVStore — invoked from the JVM shutdown hook.
     *  Idempotent; also usable from tests to guarantee handles release
     *  before the temp directory is cleaned up. */
    public static void closeAll() {
        for (var e : INSTANCES.entrySet()) {
            try { e.getValue().close(); }
            catch (Exception ignored) { /* best-effort */ }
        }
        INSTANCES.clear();
    }
}
