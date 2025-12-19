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

import com.hitorro.basedms.cache.ContentTypeCache;
import com.hitorro.basedms.exceptions.CategoryException;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.HTSerializable;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;
import org.hibernate.Hibernate;

import java.io.IOException;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved.
 * <p/>
 * User: chris
 * <p/>
 * Method to set content on an object.  One can subclass this as long as you ensure you
 */
@TypeClassMetaInfo(shortTypeName = "ContentSetter",
        isView = false,
        isPersisted = false,
        schemaVersion = ContentSetter.SerializationVersion)
public class ContentSetter implements HTSerializable {

    public static final int SerializationVersion = 1;
    private String sysGuid;
    private String mimeType;
    private String storeName;
    private String tagDomain;
    private String tagValue;
    private String fileName;


    public String getSysGuid() {
        return sysGuid;
    }

    public void setSysGuid(String sysGuid) {
        this.sysGuid = sysGuid;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getTagDomain() {
        return tagDomain;
    }

    public void setTagDomain(String tagDomain) {
        this.tagDomain = tagDomain;
    }

    public String getTagValue() {
        return tagValue;
    }

    public void setTagValue(String tagValue) {
        this.tagValue = tagValue;
    }

    public boolean setFile(BaseFile file, BaseSession session, Content contentParent, boolean commit) throws IOException, StoreException, CategoryException {

        VersionableObject so = (VersionableObject) session.getObjectFromGuid(getSysGuid());
        boolean flag = Hibernate.isInitialized(so);
        if (so == null) {
            return false;
        }
        ContentType ct = ContentTypeCache.getCache().getContentTypeByMimeType(this.getMimeType());
        if (StringUtil.nullOrEmptyString(fileName)) {
            fileName = file.getName();
        }
        Content cont;
        if (contentParent != null) {
            long size = file.length();
            cont = contentParent.setContentRendition(session, ct, file, "");
        } else {
            cont = so.setContent(fileName, ct, file);
        }

        if (!StringUtil.nullOrEmptyString(getTagDomain())) {
            cont.addCategory(getTagDomain(), getTagValue());
        }

        if (commit) {
            session.commit();
            return true;
        }
        return false;
    }

    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        os.writeString(sysGuid);
        os.writeString(mimeType);
        os.writeString(storeName);
        os.writeString(tagDomain);
        os.writeString(tagValue);
        os.writeString(fileName);
    }

    public void deserialize(HTObjectInputStream os) throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        switch (version) {
            case 1:
                sysGuid = os.readString();
                mimeType = os.readString();
                storeName = os.readString();
                tagDomain = os.readString();
                tagValue = os.readString();
                fileName = os.readString();
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
