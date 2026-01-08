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
package com.hitorro.base.objects.csvconsumers;

import com.hitorro.base.objects.Client;
import com.hitorro.basedms.Log;
import com.hitorro.basedms.db.CSVHibernateLoaderConsumer;
import com.hitorro.basedms.exceptions.CategoryException;
import com.hitorro.util.core.map.MapUtil;
import com.hitorro.util.core.string.StringUtil;


public class ClientCSVConsumer extends CSVHibernateLoaderConsumer<Client> {
    public static final String UniqueNameColumn = "uniquename";
    public static final String NameColumn = "name";
    public static final String CategoryColumn = "categories";
    public static final String Category = "htcategory";
    private static final String[][] Keys = {{"name", "name"}};

    public void start() {
    }

    public Class getPersistingClass() {
        return Client.class;
    }

    public String[][] getKeyNameMap() {
        return Keys;
    }

    public boolean add(String[] row, Client client, boolean existsAlready) {
        String uniqueName = MapUtil.getColumnFromColumMap(UniqueNameColumn, m_headerMap, row);
        String name = MapUtil.getColumnFromColumMap(NameColumn, m_headerMap, row);
        String catsRaw = MapUtil.getColumnFromColumMap(CategoryColumn, m_headerMap, row);
        String Seperator = ",";

        client.setUniqueName(uniqueName);
        client.setName(name);
        client.setAdapterSource(adapterSource);
        String cats[] = StringUtil.tokenizeFromSingleChar(catsRaw, Seperator, true);
        for (String cat : cats) {
            try {
                client.addCategory(Category, cat);
            } catch (CategoryException e) {
                Log.basedms.error("Unable to add category %s to client %s", cat, name);
                return false;
            }
        }
        saveOrUpdate(existsAlready, client);
        return true;
    }

    public void done() {
    }
}

