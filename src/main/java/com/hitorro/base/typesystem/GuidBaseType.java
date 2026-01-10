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
package com.hitorro.base.typesystem;

import com.hitorro.basedms.NamedLong;
import com.hitorro.basedms.db.HibernateService;
import com.hitorro.util.core.Env;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.TypeIntf;
import com.hitorro.util.typesystem.TypeManagerBase;
import com.hitorro.util.typesystem.VersionBaseType;
import com.hitorro.util.typesystem.annotation.FullTextAttributeMetaInfo;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;

import jakarta.persistence.*;


/**
 */
@MappedSuperclass
public abstract class GuidBaseType extends VersionBaseType<BaseSession> {
    public static NamedLong BaseTypeIdNamedLong = NamedLong.registerNamedLong("baseid", 1, 100, "unique id for base id and its subtypes");

    @Column(name = "guid", nullable = false)
    @org.hibernate.annotations.Index(name = "guid_idx")
    protected String guid = null;

    public static String computeGuid(TypeIntf type) {
        TypeClassMetaInfo meta = type.getTypeMeta();

        int id = Env.getGlobalId();
        long thisId = BaseTypeIdNamedLong.getNextValue();
        return Fmt.S("%s:%s:%s", meta.shortTypeName(), Integer.toHexString(id), Long.toHexString(thisId));
    }

    @FullTextAttributeMetaInfo(displayName = "guid",
            isFullTextIndexable = true, luceneIndexingFilters = "",
            luceneFieldName = "guid", stringLiteral = true, allField = false, stored = true)
    public String getGuid() {
        if (guid == null && HibernateService.s_isInitialized) {
            guid = computeGuid(TypeManagerBase.get().getTypeForBaseType(this));
        }
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public boolean hasGuid() {
        return true;
    }
}
