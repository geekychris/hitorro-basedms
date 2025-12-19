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
package com.hitorro.base.objects;

import com.hitorro.base.typesystem.accessors.GuidAccessor;
import com.hitorro.basedms.Log;
import com.hitorro.util.core.classes.ClassUtil;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.core.valuemap.ValueMap;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.BaseType;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;

import java.io.IOException;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Nov 3, 2006 Time: 9:45:02 AM
 */

@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.DomainInfo,
        isView = false,
        isPersisted = true,
        softLinkField = "domain",
        schemaVersion = DomainInfo.SerializationVersion,
        guidAccessor = GuidAccessor.class)
public class DomainInfo extends BaseType {
    public static final int SerializationVersion = 1;
    private String domain;
    private String displayName;
    private String description;
    private String valueMapImpl;

    public DomainInfo() {

    }

    public DomainInfo(String domain, String displayName, String description, String impl) {
        setDomain(domain);
        setDisplayName(displayName);
        setValueMapImpl(impl);
        setDescription(description);
    }

    public ValueMap getValueMapInstance() {
        if (StringUtil.nullOrEmptyOrBlankString(valueMapImpl)) {
            return null;
        }
        Object o = ClassUtil.getInstanceSwallowError(valueMapImpl, ValueMap.class);
        if (o == null) {
            Log.basedms.error("Unable to construct valuemap %s, it either does not exist as a type or is not a subclass of ValueMap",
                    valueMapImpl);
        }
        return (ValueMap) o;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String s) {
        displayName = s;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String s) {
        description = s;
    }

    public String getValueMapImpl() {
        return valueMapImpl;
    }

    public void setValueMapImpl(String s) {
        valueMapImpl = s;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        domain = StringUtil.lowerCaseIfNotNull(domain);
    }

    public void serialize(HTObjectOutputStream os)
            throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeString(domain);
        os.writeString(displayName);
        os.writeString(description);
        os.writeString(valueMapImpl);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);
        switch (version) {
            case 1:
                domain = os.readString();
                displayName = os.readString();
                description = os.readString();
                valueMapImpl = os.readString();
        }
    }

    public int getSerializationVersion() {
        return SerializationVersion;
    }

    public boolean hasGuid() {
        return false;
    }
}
