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

import com.hitorro.base.objects.Permission;
import com.hitorro.basedms.db.CSVHibernateLoaderConsumer;
import com.hitorro.util.core.map.MapUtil;

/**
 * Load permissions from a CSV file.
 */
public class PermissionCSVConsumer extends CSVHibernateLoaderConsumer<Permission> {
    public static final String NameColumn = "name";
    public static final String DescriptionColumn = "description";
    private static final String[][] Keys = {{NameColumn, "name"}};

    public void start() {
    }

    public Class getPersistingClass() {
        return Permission.class;
    }

    public String[][] getKeyNameMap() {
        return Keys;
    }

    public boolean add(String[] row, Permission permission, boolean existsAlready) {
        String name = MapUtil.getColumnFromColumMap(NameColumn, m_headerMap, row);
        String desc = MapUtil.getColumnFromColumMap(DescriptionColumn, m_headerMap, row);

        permission.setName(name);
        permission.setDescription(desc);
        this.saveOrUpdate(existsAlready, permission);
        return true;
    }

    public void done() {
    }


}
