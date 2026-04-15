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

import com.hitorro.language.IsoLanguage;
import com.hitorro.util.core.events.cache.HashCache;
import com.hitorro.util.core.iterator.mappers.BaseMapper;
import com.hitorro.util.core.opers.AlwaysTrueOperator;
import com.hitorro.util.typesystem.Type;
import com.hitorro.util.typesystem.TypeField;
import com.hitorro.util.typesystem.TypeFieldIntf;
import com.hitorro.util.typesystem.TypeManager;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.util.*;

/**
 */
public class AllTypesAnalyzerCache extends BaseMapper<IsoLanguage, Analyzer> {
    public static final String EventName = "AllTypesAnalyzerCache";
    private static final HashCache<IsoLanguage, Analyzer> s_cache = new HashCache<>(EventName, new AllTypesAnalyzerCache(true));
    private static final HashCache<IsoLanguage, Analyzer> s_indexcache = new HashCache<>(EventName, new AllTypesAnalyzerCache(false));
    protected static PerFieldAnalyzerWrapper flyweight = new PerFieldAnalyzerWrapper(new StandardAnalyzer());
    protected boolean search = true;

    public AllTypesAnalyzerCache(boolean search) {
        this.search = search;
    }

    public static HashCache<IsoLanguage, Analyzer> getCache(boolean search) {
        if (search) {
            return s_cache;
        } else {
            return s_indexcache;
        }
    }

    public static HashCache<IsoLanguage, Analyzer> getCache() {
        return s_cache;
    }

    public Analyzer apply(IsoLanguage language) {
        List<Type> types = TypeManager.getTypeManager().getTypesMatchingConstraint(new AlwaysTrueOperator());

        Set<TypeFieldIntf> set = new HashSet();
        for (Type t : types) {
            List<TypeFieldIntf> list = TypeFieldsCache.getCache().get(t);
            if (list != null) {
                for (TypeFieldIntf tf : list) {
                    set.add(tf);
                }
            }
        }

        // deduped set now gets turned into composite analyzer.
        Map<String, Analyzer> map = new HashMap();
        for (TypeFieldIntf tf : set) {
            if (search) {
                map.put(tf.getFullTextMeta().luceneFieldName(), ((TypeField) tf).getSearchAnalyzer(language));
            } else {
                map.put(tf.getFullTextMeta().luceneFieldName(), ((TypeField) tf).getIndexingAnalyzer(language));
            }
        }
        PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), map);

        return analyzer;
    }
}
