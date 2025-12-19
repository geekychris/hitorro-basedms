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
package com.hitorro.basedms.db;

import com.hitorro.util.core.classes.ClassFactory;
import com.hitorro.util.core.map.MapUtil;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.io.csv.CSVReader;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTSerializable;
import com.hitorro.util.typesystem.Type;
import com.hitorro.util.typesystem.TypeManager;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Materialize a bunch of hibernate objects from a csv file
 *
 * @author chris
 */
public class CSVHibernateLoader {
    private static ClassFactory<Class, CSVHibernateLoaderConsumer> s_classFactory =
            new ClassFactory<Class, CSVHibernateLoaderConsumer>();

    static {
        //not something I like doing, some time we should switch this over to module initialization.

    }

    private String m_header[];

    public synchronized static void addAdapter(Class<Object> hibernateStorageClass, Class<CSVHibernateLoaderConsumer> adapterClass) {
        s_classFactory.add(hibernateStorageClass, adapterClass);
    }

    public boolean load(BaseSession session, File file, CSVHibernateLoaderConsumer consumer)
            throws IOException {
        CSVReader reader = new CSVReader(FileUtil.getBufferedFileInputStream(file));
        m_header = reader.getColumnNames();
        Map<String, Integer> map = MapUtil.getMapColumnNameToIndexPosition(m_header, true);
        String[] row = reader.getNextRow();

        Class persistingClass = consumer.getPersistingClass();
        consumer.setSession(session);

        Type type = TypeManager.getTypeManager().getTypeForClass(persistingClass);
        consumer.setType(type);
        consumer.setHeaderMap(map);
        consumer.setAdapterSource(file.getName());
        // create the key mapping so that we can compute existing rows.
        while (row != null) {
            if (row.length >= 1 && row[0] != null && !row[0].startsWith("#")) {
                HTSerializable pts = consumer.getExistingObjectIfExists(row);
                if (pts == null) {
                    consumer.add(row, consumer.getNewInstance(), false);
                } else {
                    consumer.add(row, pts, true);
                }
            }

            row = reader.getNextRow();
        }

        reader.close();

        return true;
    }
}
