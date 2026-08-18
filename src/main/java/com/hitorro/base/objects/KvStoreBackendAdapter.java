/*
 * Copyright (c) 2006-2026 Chris Collins
 */
package com.hitorro.base.objects;

import com.hitorro.util.io.StoreException;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link StoreType#KVStore} — thin adapter around the existing
 * {@link KvStoreBackend} static API so the RocksDB code stays as-is
 * (with all its tests + operator docs) while participating in the
 * new {@link StoreBackend} registry.
 *
 * <p>When {@code hitorro-basedms-kvstore} eventually splits into
 * its own module, this class + {@link KvStoreBackend} +
 * {@link StoreMigrator}* move together; the enum stays in basedms
 * core but the {@link StoreBackendRegistry} registration migrates
 * to a ServiceLoader entry the new module ships.</p>
 */
final class KvStoreBackendAdapter implements StoreBackend {

    @Override public StoreType type() { return StoreType.KVStore; }

    @Override
    public InputStream read(Store store, Content content) throws StoreException {
        return KvStoreBackend.get(store, content.getFileName());
    }

    @Override
    public boolean write(Store store, Content content, String originalFileName,
                         InputStream is, long assignedId) throws StoreException, IOException {
        KvStoreBackend.WriteResult r = KvStoreBackend.writeContent(
                store, originalFileName, assignedId, is);
        content.setFileName(r.fileName());
        content.setContentSize(r.size());
        return true;
    }
}
