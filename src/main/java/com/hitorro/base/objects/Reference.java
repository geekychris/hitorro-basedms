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
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.VersionBaseType;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <p/>
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Sep 19, 2005 Time: 1:02:07 PM
 */
@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.Reference,
        isView = false,
        isPersisted = true,
        schemaVersion = Reference.SerializationVersion,
        softLinkField = "fromGuid",
        guidAccessor = GuidAccessor.class)
public class Reference extends VersionBaseType {
    public static final String ShareReference = "share";

    public static final int SerializationVersion = 1;
    private String fromGuid;
    private long fromHash;

    private String toGuid;
    private long toHash;
    private Date date;
    private String refType;
    private String auxData;

    /**
     * Assert that the reference doesnt exist already.
     *
     * @param from
     * @param to
     * @param type
     * @return
     */
    public static int assertReference(String from, String to, String type) {
        BaseSession session = null;
        try {
            session = DMSSessionFactory.getFactory().getSession();
            List<Reference> list = getReferences(from, to, type, session);
            if (list == null) {
                return 0;
            }

            int size = list.size();
            if (size > 0) {
                Log.util.error("Reference already exists: %s %s %s / Count=$s", from, to, type, size);
            }
            return size;
        } finally {
            DMSSessionFactory.getFactory().rollbackCloseSession(session);
        }
    }

    public static List<Reference> getReferences(String from, String to, String type, BaseSession session) {
        List<Reference> list = new ArrayList<Reference>();
        String s = Fmt.S("where fromGuid='%s' and toGuid='%s' ", from, to, type);
        session.getObjects(Reference.class, s, list);
        return list;
    }

    public static Reference createReference(VersionableObject from, VersionableObject to, String type, String aux, BaseSession session) {
        Reference ref = new Reference();
        ref.setFromGuid(from.getGuid());
        ref.setFromHash(from.getIdentityHash());
        ref.setToGuid(to.getGuid());
        ref.setToHash(to.getIdentityHash());
        ref.setDate(new Date());
        ref.setRefType(type);
        ref.setAuxData(aux);
        if (session != null) {
            session.persist(ref);
        }
        return ref;
    }

    public String getFromGuid() {
        return fromGuid;
    }

    public void setFromGuid(String fromGuid) {
        this.fromGuid = fromGuid;
    }

    public long getFromHash() {
        return fromHash;
    }

    public void setFromHash(long fromHash) {
        this.fromHash = fromHash;
    }

    public String getToGuid() {
        return toGuid;
    }

    public void setToGuid(String toGuid) {
        this.toGuid = toGuid;
    }

    public long getToHash() {
        return toHash;
    }

    public void setToHash(long toHash) {
        this.toHash = toHash;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getRefType() {
        return refType;
    }

    public void setRefType(String refType) {
        this.refType = refType;
    }

    public String getAuxData() {
        return auxData;
    }

    public void setAuxData(String auxData) {
        this.auxData = auxData;
    }

    public int getSerializationVersion() {
        return SerializationVersion;
    }

    public void serialize(HTObjectOutputStream os)
            throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeString(fromGuid);
        os.writeString(toGuid);
        os.writeLong(fromHash);
        os.writeLong(toHash);
        os.writeDate(date);
        os.writeString(refType);
        os.writeString(auxData);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);
        switch (version) {
            case 1:
                fromGuid = os.readString();
                toGuid = os.readString();
                fromHash = os.readLong();
                toHash = os.readLong();
                date = os.readDate();
                refType = os.readString();
                auxData = os.readString();
        }
    }
}
