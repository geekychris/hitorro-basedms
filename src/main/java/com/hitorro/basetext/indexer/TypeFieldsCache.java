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
package com.hitorro.basetext.indexer;

import com.hitorro.util.core.events.cache.HashCache;
import com.hitorro.util.core.iterator.mappers.BaseMapper;
import com.hitorro.util.core.opers.HTPredicate;
import com.hitorro.util.typesystem.TypeFieldIntf;
import com.hitorro.util.typesystem.TypeIntf;
import com.hitorro.util.typesystem.constraint.IsFullTextable;

import java.util.List;


/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Oct 17, 2006 Time: 6:56:04 PM
 */
public class TypeFieldsCache extends BaseMapper<TypeIntf, List<TypeFieldIntf>> {
    public static String EventName = "TypeFieldsCache";

    private static final HashCache<TypeIntf, List<TypeFieldIntf>> s_cache = new HashCache<TypeIntf, List<TypeFieldIntf>>(EventName, new TypeFieldsCache());
    public HTPredicate<TypeFieldIntf> m_indexConstraint = new IsFullTextable();

    public TypeFieldsCache() {
    }

    public static final HashCache<TypeIntf, List<TypeFieldIntf>> getCache() {
        return s_cache;
    }

    public List<TypeFieldIntf> apply(TypeIntf key) {
        return key.getTypeFieldsByConstraint(m_indexConstraint);
    }
}