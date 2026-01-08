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

import com.hitorro.base.objects.Content;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.iterator.database.JDBCFieldFetcherValueAdapter;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.HTSerializable;
import com.hitorro.util.typesystem.HTSerializableUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * <p/>
 * Using a jdbc result, that has 1 column that is the guid of a content object that has HTSerialized object as its
 * content, deserialize the payload and return it.
 */
public class HTSerializableFromContentAdapter<E extends HTSerializable> extends JDBCFieldFetcherValueAdapter<E> {
    private DMSSession m_session;
    private int m_count = 0;

    public HTSerializableFromContentAdapter(DMSSession session) {
        m_session = session;
    }

    public E apply(ResultSet record) {

        m_session.rollback();
        try {
            String contentGuid = record.getString(1);
            Content cont = (Content) m_session.getHTSerializableFromGUID(contentGuid);
            if (cont == null) {
                return null;
            }
            try {
                InputStream is = cont.getContent();
                E e = (E) HTSerializableUtil.readHTSerializableFromBuffer(is, null);
                return e;
            } catch (StoreException e) {
                Log.util.error("%s %e", e, e);
            } catch (FileNotFoundException e) {
                Log.util.error("%s %e", e, e);
            } catch (IOException e) {
                Log.util.error("%s %e", e, e);
            }

        } catch (SQLException e) {
            Log.util.error("%s %e", e, e);
            return null;
        }
        return null;
    }

    public void close() {
        if (m_session != null) {
            DMSSessionFactory.getFactory().rollbackClose(m_session);
        }
    }
}
