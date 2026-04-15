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

import com.hitorro.base.objects.DomainInfo;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.events.cache.HashCache;
import com.hitorro.util.core.iterator.mappers.BaseMapper;
import com.hitorro.util.typesystem.BaseSession;

/**
 */
public class DomainInfoCache extends BaseMapper<String, DomainInfo> {
    public static final String EventName = "DomainInfo Cache";
    private static final DomainInfo NullFlyweight = new DomainInfo();
    private static final HashCache<String, DomainInfo> s_cache = new HashCache<String, DomainInfo>(EventName, NullFlyweight, new DomainInfoCache());


    private static final String[][] Keys = {{"domain", "domain"}, {"value", "value"}};

    /**
     * Constructor
     */
    public DomainInfoCache() {
    }

    public static HashCache<String, DomainInfo> getCache() {
        return s_cache;
    }

    public String[][] getKeyNameMap() {
        return Keys;
    }

    public DomainInfo apply(String key) {
        BaseSession session = null;
        try {
            session = DMSSessionFactory.getFactory().getSession();
            return (DomainInfo) session.getBySoftReference(DomainInfo.class, key);
        } finally {
            DMSSessionFactory.getFactory().rollbackClose(session);
        }
    }
}
