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
package com.hitorro.basedms.jdbcadapters;

import com.hitorro.basedms.session.DMSSession;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.iterator.database.JDBCFieldFetcherValueAdapter;
import com.hitorro.util.typesystem.HTSerializable;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * <p/>
 * Given a jdbc result row with 1 column which is the guid of a content object, return the reconstituted content
 * object.
 */
public class ContentAdapter<E extends HTSerializable> extends JDBCFieldFetcherValueAdapter<E> {
    private DMSSession m_session;
    private int m_count = 0;

    public ContentAdapter(DMSSession session) {
        m_session = session;
    }

    public E apply(ResultSet record) {
        m_session.rollback();
        try {
            String contentGuid = record.getString(1);
            return (E) m_session.getHTSerializableFromGUID(contentGuid);
        } catch (SQLException e) {
            Log.util.error("%s %e", e, e);
            return null;
        }
    }

    public void close() {

    }
}
