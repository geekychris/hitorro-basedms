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
package com.hitorro.basedms.contentconstraints;

import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.base.objects.Content;
import com.hitorro.util.core.HTAssert;
import com.hitorro.util.core.opers.HTPredicate;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.HTSerializable;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;

import java.io.IOException;

/**
 * <p/>
 * Look for a content that has a tag.  If Value is null then it looks for a domain only
 */
@TypeClassMetaInfo(shortTypeName = "TagContentConstraint",
        isView = false,
        isPersisted = false,
        schemaVersion = TagConstraint.SerializationVersion)

public class TagConstraint implements HTPredicate<Content>, HTSerializable {
    public static final int SerializationVersion = 1;
    private String m_domain;
    private String m_value;

    public TagConstraint() {

    }

    public TagConstraint(String domain, String value) {
        m_domain = domain.toLowerCase();
        if (value != null) {
            m_value = value.toLowerCase();
        }
    }

    public boolean initFromMap(final JsonNode map) {
        // MUST IMPLEMENT
        HTAssert.assertThat(false, "TagConstraint.initFromMap not implemented");
        return false;
    }

    public void initForPass() {

    }

    public String toString() {
        if (m_value == null) {
            return Fmt.S("Tag(%s)", m_domain);
        }
        return Fmt.S("Tag(%s, %s)", m_domain, m_value);
    }

    public boolean test(Content c) {
        if (m_value == null) {
            return c.getCategoryDomainExists(m_domain);
        }
        return c.getCategoryValueExists(m_domain, m_value);
    }

    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        os.writeString(m_domain);
        os.writeString(m_value);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        switch (version) {
            case 1:
                m_domain = os.readString();
                m_value = os.readString();
        }
    }

    public int getSerializationVersion() {
        return SerializationVersion;
    }

    public boolean isPersisted() {
        return false;
    }

    public boolean hasGuid() {
        return false;
    }

    public boolean hasSoftGuid() {
        return false;
    }
}