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

import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.basedms.session.HibernateQueryResultObjectAdapter;
import com.hitorro.util.core.Log;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.BaseType;
import com.hitorro.util.typesystem.TypeManager;
import com.hitorro.util.typesystem.btadapter.BaseAdapterItemCache;
import com.hitorro.util.typesystem.btadapter.BaseTypeAdapter;
import org.hibernate.type.Type;

import java.util.Iterator;

/**
 * <p/>
 */
public class SORetrievalAdapter<E extends BaseType, OUT> implements HibernateQueryResultObjectAdapter {
    private BaseSession session;
    private String adapterName;
    private boolean flush = true;
    private int count = 0;
    private BaseAdapterItemCache<OUT> cache;

    public SORetrievalAdapter(String aN, BaseAdapterItemCache<OUT> cache) {
        adapterName = aN;
        this.cache = cache;
    }

    public void flushAfterOnNext(boolean flag) {
        flush = flag;
    }

    public OUT map(Iterator iter, Type[] types, String[] aliases) {
        if (session == null) {
            session = DMSSessionFactory.getFactory().getSession();
        }

        while (iter.hasNext()) {
            String guid = (String) iter.next();

            if (flush) {
                session.rollback();
            }

            if (cache != null) {
                return cache.get(guid);
            }
            E result = (E) session.getObjectFromGuid(guid);
            com.hitorro.util.typesystem.Type type = TypeManager.getTypeManager().getTypeFromGuid(guid);
            if (type != null) {
                BaseTypeAdapter<E, OUT> bta = type.getBTAdapter(adapterName);
                if (bta != null) {
                    if (validate(result)) {
                        OUT obj = bta.getObject(result);

                        return obj;
                    }
                } else {
                    Log.util.error("Unable to find base type adapter for %s %s", guid, adapterName);
                }
            } else {
                Log.util.error("Unable to retrieve type for %s", guid);
            }
        }
        return null;
    }

    public boolean validate(E obj) {
        return true;
    }

    public void close() {
        DMSSessionFactory.getFactory().rollbackCloseSession(session);
    }
}

