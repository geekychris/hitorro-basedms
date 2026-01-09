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
package com.hitorro.basedms;

import com.hitorro.basedms.session.DMSSession;
import com.hitorro.util.core.string.StringUtil;
import org.hibernate.query.Query;

import java.util.Iterator;

/**
 * <p/>
 * Assumes you are executing a query returning a GUID.  From that we will fetch the
 */
public class DMSQueryIterator<T> implements Iterator<T> {
    private Query m_query;

    private T m_curr;
    private DMSSession m_session;
    private Iterator<String> guidIter;

    public DMSQueryIterator(Query q, DMSSession session) {
        m_query = q;
        m_session = session;
        guidIter = m_query.stream().iterator();
        m_curr = getAux();
    }

    public boolean hasNext() {
        m_curr = getAux();
        return m_curr != null;
    }

    public T next() {
        T returnMe = m_curr;
        m_curr = null;
        return returnMe;
    }

    public void remove() {
        //not implemented
    }

    private T getAux() {
        if (guidIter.hasNext()) {
            String guid = guidIter.next();
            m_session.rollback();
            if (!StringUtil.nullOrEmptyString(guid)) {
                return (T) m_session.getHTSerializableFromGUID(guid);
            }
        }
        return null;
    }
}
