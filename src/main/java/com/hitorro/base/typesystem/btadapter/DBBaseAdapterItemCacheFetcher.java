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
package com.hitorro.base.typesystem.btadapter;

import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.Log;
import com.hitorro.util.keystore.KeyStore;
import com.hitorro.util.typesystem.btadapter.BaseAdapterItemCacheFetcher;
import com.hitorro.util.typesystem.btadapter.BaseTypeAdapter;

import java.io.IOException;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Oct 24, 2005 Time: 4:43:34 PM
 */
public class DBBaseAdapterItemCacheFetcher<T> extends BaseAdapterItemCacheFetcher<T> {
    /**
     * Utility function to fetch an object
     *
     * @param guid
     * @param adapterName
     * @param store
     * @return
     */

    public T getAndMap(String guid, String adapterName, KeyStore<T> store, com.hitorro.util.typesystem.ProxyAdapter<T> proxy) {
        if (store != null) {
            try {
                T obj = store.get(guid);
                if (obj != null) {
                    return obj;
                }
            } catch (IOException e) {
                Log.util.error("Exception %s %e", e, e);
            }
        }
        com.hitorro.util.typesystem.BaseSession session = DMSSessionFactory.getFactory().getSession();
        try {
            com.hitorro.util.typesystem.BaseType result = (com.hitorro.util.typesystem.BaseType) session.getObjectFromGuid(guid);
            com.hitorro.util.typesystem.Type type = com.hitorro.util.typesystem.TypeManager.getTypeManager().getTypeFromGuid(guid);
            if (type != null) {
                BaseTypeAdapter<com.hitorro.util.typesystem.BaseType, T> bta = type.getBTAdapter(adapterName);
                if (bta != null) {
                    T obj = null;
                    if (proxy == null) {
                        obj = bta.getObject(result);
                    } else {
                        obj = proxy.proxy(bta.getObject(result));
                    }

                    if (store != null) {
                        try {
                            store.put(guid, obj);
                        } catch (IOException e) {
                            Log.util.error("Exception %s %e", e, e);
                        }
                    }
                    return obj;
                } else {
                    Log.util.error("Unable to find base type adapter for %s %s", guid, adapterName);
                }
            } else {
                Log.util.error("Unable to retrieve type for %s", guid);
            }
        } finally {
            DMSSessionFactory.getFactory().rollbackClose(session);
        }
        return null;
    }
}