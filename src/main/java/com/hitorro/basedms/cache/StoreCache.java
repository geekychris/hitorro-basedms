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
package com.hitorro.basedms.cache;

import com.hitorro.base.objects.Store;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.BaseSessionFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Oct 31, 2006 Time: 10:02:03 AM
 */
public class StoreCache {
    private Store defaultStore = null;
    private Map<String, Store> stores = new HashMap<String, Store>();
    private Map<String, Store> storesByGuid = new HashMap<String, Store>();

    public Store getStoreByGuid(String guid) {
        return storesByGuid.get(guid);
    }

    @SuppressWarnings("unchecked")
    public void load() {
        BaseSession session = BaseSessionFactory.getFactory().getSession();
        try {
            List<Store> tStores = new ArrayList<Store>();
            session.getObjects(Store.class, "", tStores);
            for (Store store : tStores) {
                stores.put(store.getName(), store);
                storesByGuid.put(store.getSoftGuid(), store);
                store.init();
            }
            defaultStore = (Store) session.getSingleObject(Store.class, " where defaultStore=true");
        } finally {
            DMSSessionFactory.closeSession(session);
        }

    }

    public Store getStore(String name) {
        return stores.get(name.toLowerCase());
    }

    public Store getDefaultStore() {
        return defaultStore;
    }
}
