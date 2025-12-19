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

import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.BaseType;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;

import java.io.IOException;

@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.Extension,
        isView = false,
        isPersisted = true,
        schemaVersion = Extension.SerializationVersion)
public class Extension extends BaseType {
    public static final int SerializationVersion = 2;
    private String extension;
    private ContentType contentType;
    private boolean isPrefered;

    public boolean getIsPrefered() {
        return isPrefered;
    }

    public void setIsPrefered(boolean flag) {
        isPrefered = flag;
    }

    public String getFileExtension() {
        return extension;
    }

    public void setFileExtension(String fileExtension) {
        this.extension = fileExtension;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        contentType = contentType;
    }

    public void serialize(HTObjectOutputStream os)
            throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeBoolean(isPrefered);
        os.writeString(extension);
        os.writeVersionedObject(contentType);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);
        switch (version) {
            case 2:
                isPrefered = os.readBoolean();
            case 1:
                extension = os.readString();
                contentType = (ContentType) os.readVersionedObject();
        }
    }

    public int getSerializationVersion() {
        return SerializationVersion;
    }

    public boolean upgradeAllInstances(long currentSchemaVersion) {
        return currentSchemaVersion == 1 && SerializationVersion == 2;
    }
}
