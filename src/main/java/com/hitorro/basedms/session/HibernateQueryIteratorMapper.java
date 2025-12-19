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
package com.hitorro.basedms.session;

import com.hitorro.util.core.iterator.AbstractIterator;
import com.hitorro.util.core.iterator.CloseableIterator;
import org.hibernate.query.Query;
import org.hibernate.type.Type;

import java.util.Iterator;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Mar 7, 2005 Time: 4:20:44 PM
 */
public class HibernateQueryIteratorMapper<E> extends AbstractIterator<E> {
    private HibernateQueryResultObjectAdapter<E> m_adapter;
    private Type[] m_types;
    private String[] m_returnAliases;
    private Iterator m_iter;

    public HibernateQueryIteratorMapper(HibernateQueryResultObjectAdapter adapter, Query query) {
        m_adapter = adapter;

        m_iter = query.stream().iterator();
        //XXX TODO BUSTED
        //   m_types = query.getReturnTypes();
        //  m_returnAliases = query.getReturnAliases();
    }

    public boolean hasNext() {
        return m_iter.hasNext();
    }

    public E next() {
        return m_adapter.map(m_iter, m_types, m_returnAliases);
    }

    public void remove() {

    }

    public void close() throws Exception {
        if (m_iter instanceof CloseableIterator) {
            CloseableIterator ci = (CloseableIterator) m_iter;
            ci.close();
        }
    }
}


