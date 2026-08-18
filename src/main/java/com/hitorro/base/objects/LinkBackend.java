/*
 * Copyright (c) 2006-2026 Chris Collins
 */
package com.hitorro.base.objects;

import com.hitorro.util.io.StoreException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * {@link StoreType#Link} — bytes fetched at read time from a URL
 * stored in {@code content.originalFileName}. Read-only.
 */
final class LinkBackend implements StoreBackend {

    @Override public StoreType type() { return StoreType.Link; }

    @Override
    public InputStream read(Store store, Content content) throws StoreException {
        try {
            return new URL(content.getOriginalFileName()).openStream();
        } catch (MalformedURLException e) {
            throw new StoreException(e.getMessage());
        } catch (IOException e) {
            throw new StoreException(e.getMessage());
        }
    }

    @Override
    public boolean write(Store store, Content content, String originalFileName,
                         InputStream is, long assignedId) throws StoreException {
        throw new StoreException("Cannot set content for a content object with store type of Link");
    }
}
