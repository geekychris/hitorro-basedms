/*
 * Copyright (c) 2006-2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hitorro.basedms;

import com.hitorro.base.objects.Store;
import com.hitorro.base.objects.StoreType;
import com.hitorro.basedms.cache.StoreCache;

/**
 * Set of utilities for File Store objects.
 *
 * @author chris
 */
public class StoreUtil {
    private static StoreCache s_storeCache = null;

    public synchronized static Store getDefaultStore() {
        ensureStoreCache();
        if (s_storeCache == null) {
            return null;
        }
        return s_storeCache.getDefaultStore();
    }

    public synchronized static Store getStore(String name) {
        ensureStoreCache();
        if (s_storeCache == null) {
            return null;
        }
        return s_storeCache.getStore(name);
    }

    public synchronized static Store getStoreByGuid(String name) {
        ensureStoreCache();
        if (s_storeCache == null) {
            return null;
        }
        return s_storeCache.getStoreByGuid(name);
    }

    public synchronized static Store getBlobStore() {
        ensureStoreCache();
        if (s_storeCache == null) {
            return null;
        }
        return s_storeCache.getStore(StoreType.Blob.name());
    }

    public synchronized static Store getLinkStore() {
        ensureStoreCache();
        if (s_storeCache == null) {
            return null;
        }
        return s_storeCache.getStore(StoreType.Link.name());
    }

    private static void ensureStoreCache() {
        if (s_storeCache == null) {
            s_storeCache = new StoreCache();
            s_storeCache.load();
        }
    }
}

