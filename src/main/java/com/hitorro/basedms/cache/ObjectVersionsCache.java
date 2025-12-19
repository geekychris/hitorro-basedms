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
package com.hitorro.basedms.cache;

import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.objects.ObjectVersions;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.BaseSessionFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Oct 18, 2006 Time: 8:50:36 PM
 */
public class ObjectVersionsCache {
    private Map<String, ObjectVersions> m_versions = new HashMap<String, ObjectVersions>();

    @SuppressWarnings("unchecked")
    public void load() {
        BaseSession session = BaseSessionFactory.getFactory().getSession();
        try {
            List<ObjectVersions> stores = new ArrayList<ObjectVersions>();
            session.getObjects(ObjectVersions.class, "", stores);
            for (ObjectVersions store : stores) {
                m_versions.put(store.getName().toLowerCase(), store);
            }
        } finally {
            DMSSessionFactory.closeSession(session);
        }

    }

    public ObjectVersions get(String name) {
        return m_versions.get(name.toLowerCase());
    }
}