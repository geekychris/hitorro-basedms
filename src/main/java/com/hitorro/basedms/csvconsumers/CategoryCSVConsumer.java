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

import com.hitorro.base.objects.Category;
import com.hitorro.basedms.db.CSVHibernateLoaderConsumer;
import com.hitorro.util.core.map.MapUtil;
import com.hitorro.util.core.string.StringUtil;

import java.util.HashMap;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Nov 3, 2006 Time: 1:08:08 PM One should load
 * one domain at a time with this integrator ELSE you end up with false parent linkage.  This loader assumes parents are
 * part of the integration event.
 */
public class CategoryCSVConsumer extends CSVHibernateLoaderConsumer<Category> {
    public static final String DomainColumn = "domain";
    public static final String ValueColumn = "value";
    public static final String DisplayNameColumn = "displayname";
    public static final String DescriptionColumn = "description";
    public static final String ParentColumn = "parent";
    public static final String externalIdColumn = "externalid";
    private static final String[][] Keys = {{"domain", "domain"}, {"value", "value"}};
    private HashMap<String, Category> m_cats = new HashMap();

    public String[][] getKeyNameMap() {
        return Keys;
    }

    public void start() {
    }

    public Class getPersistingClass() {
        return Category.class;
    }

    public boolean add(String[] row, Category c, boolean existsAlready) {
        String domain = MapUtil.getColumnFromColumMap(DomainColumn, m_headerMap, row);
        String value = MapUtil.getColumnFromColumMap(ValueColumn, m_headerMap, row);
        String displayName = MapUtil.getColumnFromColumMap(DisplayNameColumn, m_headerMap, row);
        String description = MapUtil.getColumnFromColumMap(DescriptionColumn, m_headerMap, row);
        String externalId = MapUtil.getColumnFromColumMap(externalIdColumn, m_headerMap, row);
        String parentValue = MapUtil.getColumnFromColumMap(ParentColumn, m_headerMap, row);
        Category parent = null;

        if (!StringUtil.nullOrEmptyOrBlankString(parentValue)) {
            parent = m_cats.get(parentValue);
        }

        c.setDomain(domain);
        c.setValue(value);
        c.setDisplayName(displayName);
        c.setDescription(description);
        c.setParent(parent);
        c.setExternalId(externalId);
        c.setAdapterSource(adapterSource);

        if (parent != null) {
            parent.getChildren().add(c);
        }
        m_cats.put(value, c);
        saveOrUpdate(existsAlready, c);
        return true;
    }

    public void done() {
    }

}
