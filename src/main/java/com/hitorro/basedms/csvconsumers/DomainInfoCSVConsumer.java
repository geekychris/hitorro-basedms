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

import com.hitorro.base.objects.DomainInfo;
import com.hitorro.basedms.db.CSVHibernateLoaderConsumer;
import com.hitorro.util.core.map.MapUtil;

/**
 */

public class DomainInfoCSVConsumer extends CSVHibernateLoaderConsumer<DomainInfo> {
    public static final String DomainColumn = "domain";
    public static final String DisplayNameColumn = "displayname";
    public static final String DescriptionColumn = "description";
    public static final String ImplColumn = "valuemapimpl";
    private static final String[][] Keys = {{"domain", "domain"}};

    public Class getPersistingClass() {
        return DomainInfo.class;
    }

    public void start() {
    }

    public String[][] getKeyNameMap() {
        return Keys;
    }

    public boolean add(String[] row, DomainInfo c, boolean existsAlready) {
        String domain = MapUtil.getColumnFromColumMap(DomainColumn, m_headerMap, row);
        String displayName = MapUtil.getColumnFromColumMap(DisplayNameColumn, m_headerMap, row);
        String description = MapUtil.getColumnFromColumMap(DescriptionColumn, m_headerMap, row);
        String impl = MapUtil.getColumnFromColumMap(ImplColumn, m_headerMap, row);

        c.setDomain(domain);
        c.setDisplayName(displayName);
        c.setDescription(description);
        c.setValueMapImpl(impl);
        saveOrUpdate(existsAlready, c);
        return true;
    }

    public void done() {
    }

}
