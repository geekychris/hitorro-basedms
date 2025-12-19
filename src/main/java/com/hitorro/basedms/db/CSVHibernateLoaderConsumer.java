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

import com.hitorro.basedms.session.DMSSession;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.map.MapUtil;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTSerializable;
import com.hitorro.util.typesystem.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * Mechanism to allow an implementor to load a series of objects from a flat csv file.
 * <p/>
 * This is a very primitive object loader mechanism.
 *
 * @author chris
 */
public abstract class CSVHibernateLoaderConsumer<E extends HTSerializable> {
    protected Type m_type;

    protected BaseSession m_session;

    protected Map<String, String> m_parts = new HashMap<String, String>();

    protected Map<String, String> m_keyNameMap;

    protected Map<String, Integer> m_headerMap;
    protected String adapterSource;

    public void setAdapterSource(String source) {
        adapterSource = source;
    }

    public void setHeaderMap(Map<String, Integer> headerMap) {
        m_headerMap = headerMap;
    }

    public E getNewInstance() {
        try {
            return (E) this.getPersistingClass().newInstance();
        } catch (InstantiationException e) {
            Log.util.error("%s %e", e, e);
        } catch (IllegalAccessException e) {
            Log.util.error("%s %e", e, e);
        }
        return null;
    }


    /**
     * Mapping of csv column name to name found in the object.  Only need to listFiles those that are used in the compound
     * key.
     *
     * @return
     */
    public abstract String[][] getKeyNameMap();

    /**
     * Get the dms session that this reader is using.
     *
     * @return the session
     */
    public BaseSession getSession() {
        return m_session;
    }

    public void setSession(BaseSession session) {
        m_session = session;
    }

    /**
     * Set the type that is being read.
     *
     * @param type
     */
    public void setType(Type type) {
        m_type = type;
    }

    /**
     * Get an object from the db using the compound key lookup mechanism.
     *
     * @param row
     * @return
     */
    public HTSerializable getExistingObjectIfExists(String[] row) {
        m_parts.clear();
        for (String key[] : getKeyNameMap()) {
            String val = MapUtil.getColumnFromColumMap(key[0], m_headerMap, row);
            if (!StringUtil.nullOrEmptyString(val)) {
                m_parts.put(key[1], val);
            }
        }

        return ((DMSSession) m_session).getGuidReference(m_type, m_parts);
    }

    protected void saveOrUpdate(boolean existsAlready, E c) {
        if (existsAlready) {
            m_session.saveOrUpdate(c);

        } else {
            m_session.persist(c);
        }
    }


    public abstract void start();

    public abstract Class getPersistingClass();

    public abstract boolean add(String[] row, E existingObject, boolean existsAlready);

    public abstract void done();
}
