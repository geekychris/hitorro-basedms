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

import com.hitorro.base.objects.Category;
import com.hitorro.base.objects.DomainInfo;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.events.cache.HashCache;
import com.hitorro.util.core.iterator.mappers.BaseMapper;
import com.hitorro.util.core.valuemap.FlatValueMap;
import com.hitorro.util.core.valuemap.ValueMap;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.BaseSessionFactory;
import org.hibernate.query.Query;

import java.util.Iterator;

/**
 */
public class DomainValueCache extends BaseMapper<String, ValueMap<Category>> {

    private static final ValueMap<Category> NullFlyweight = new FlatValueMap<Category>();
    private static final HashCache<String, ValueMap<Category>> s_cache = new HashCache("dvcache", NullFlyweight, new DomainValueCache());
    private static final String CatQuery = "from " + Category.class.getCanonicalName() + " where domain= :id";


    /**
     *
     */
    public DomainValueCache() {
    }

    public static HashCache<String, ValueMap<Category>> getCache() {
        return s_cache;
    }

    public ValueMap<Category> apply(String key) {
        DomainInfo di = DomainInfoCache.getCache().get(key);
        if (di == null) {
            return null;
        }
        ValueMap vm = di.getValueMapInstance();
        if (vm == null) {
            return null;
        }
        BaseSession sess = BaseSessionFactory.getFactory().getSession();
        try {
            // get all category objects matching
            Query q = (Query) sess.createQuery(CatQuery);
            q.setParameter("id", key);
            Iterator<Category> iter = q.stream().iterator();
            vm.setDomain(key);
            while (iter.hasNext()) {
                Category c = iter.next();

                vm.setValue(c, c.getValue());
            }
            return vm;
        } finally {
            DMSSessionFactory.closeSession(sess);
        }
    }

    public String eventName() {
        return "DomainValueCache";
    }
}
