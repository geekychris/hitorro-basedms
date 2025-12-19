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
package com.hitorro.basedms.csvconsumers;

import com.hitorro.base.objects.Store;
import com.hitorro.basedms.Log;
import com.hitorro.basedms.db.CSVHibernateLoaderConsumer;
import com.hitorro.util.core.map.MapUtil;
import com.hitorro.util.io.StoreException;

public class StoreCSVConsumer extends CSVHibernateLoaderConsumer<Store> {
    public static final String NameColumn = "name";
    public static final String StoreTypeColumn = "storetype";
    public static final String RootPathColumn = "rootpath";
    public static final String DocRootColumn = "docroot";
    public static final String DefaultColumn = "default";
    public static final String DefaultPublic = "public";
    public static final String AllowsLinks = "allowslinks";
    private static final String[][] Keys = {{"name", "name"}};

    public void start() {

    }

    public Class getPersistingClass() {
        return Store.class;
    }

    public String[][] getKeyNameMap() {
        return Keys;
    }

    public boolean add(String[] row, Store store, boolean existsAlready) {
        String name = MapUtil.getColumnFromColumMap(NameColumn, this.m_headerMap, row);
        String storeType = MapUtil.getColumnFromColumMap(StoreTypeColumn, m_headerMap, row);
        String rootPath = MapUtil.getColumnFromColumMap(RootPathColumn, m_headerMap, row);
        String docRoot = MapUtil.getColumnFromColumMap(DocRootColumn, m_headerMap, row);

        boolean defaultValue = MapUtil.getBooleanColumnFromColumMap(DefaultColumn, m_headerMap, row);
        boolean publicValue = MapUtil.getBooleanColumnFromColumMap(DefaultPublic, m_headerMap, row);

        try {
            store.setName(name);
            store.setStoreType(storeType);
            store.setRootPath(rootPath);
            store.setDocRoot(docRoot);
            store.setDefaultStore(defaultValue);
            store.setIsPubliclyVisible(publicValue);
            store.validate();
            store.init();
            this.saveOrUpdate(existsAlready, store);

            return true;
        } catch (StoreException e) {
            Log.basedms.error("Unable to load store %s with error %s, %e",
                    name, e, e);
        }
        return false;
    }

    public void done() {

    }

}
