/*
 * Copyright (c) 2006-2026 Chris Collins
 */
package com.hitorro.base.objects;

import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.io.StoreException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

/**
 * {@link StoreType#Blob} — bytes live in a JDBC Blob column via
 * Hibernate. Write is currently a stub matching the historical
 * (pre-refactor) TODO in Content.java — a full Hibernate.createBlob
 * path lands with the JPA lifecycle rework.
 */
final class BlobBackend implements StoreBackend {

    @Override public StoreType type() { return StoreType.Blob; }

    @Override
    public InputStream read(Store store, Content content) throws StoreException {
        try {
            return content.getBlobContent().getBinaryStream();
        } catch (SQLException e) {
            throw new StoreException(e.getMessage());
        }
    }

    @Override
    public boolean write(Store store, Content content, String originalFileName,
                         InputStream is, long assignedId) throws StoreException, IOException {
        // TODO — the pre-refactor code stubbed this out too; keeping
        // the identical shape so behaviour doesn't drift while a full
        // Hibernate.createBlob(is) rewrite is out of scope.
        Blob blob = null;
        content.setBlobContent(blob);
        try {
            content.setContentSize(blob.length());
        } catch (SQLException e) {
            throw new StoreException(Fmt.S("Unable to get blob size %s %e", e, e));
        }
        return true;
    }
}
