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
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.VersionBaseType;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;

import jakarta.persistence.*;
import java.io.IOException;

/**
 * Persisted Name Long Class
 * <p/>
 * Do not access this directly.  Use the NamedLong accessors.
 *
 * @author chris
 */
@Entity
@Table(name = "NamedLongEntry")
@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.NamedLong,
        isView = false,
        isPersisted = true,
        schemaVersion = NamedLongEntry.SerializationVersion,
        softLinkField = "name",
        guidAccessor = GuidAccessor.class)
public class NamedLongEntry extends VersionBaseType {
    public static final int SerializationVersion = 1;
    
    @Column(name = "value")
    private long value;
    
    @Column(name = "incrementor")
    private long incrementor;
    
    @Column(name = "description")
    private String description;

    @Column(name = "name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long val) {
        value = val;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String desc) {
        description = desc;
    }

    public long getIncrementor() {
        return incrementor;
    }

    public void setIncrementor(long incrementor) {
        this.incrementor = incrementor;
    }

    public void serialize(HTObjectOutputStream os)
            throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeLong(value);
        os.writeLong(incrementor);
        os.writeString(description);
        os.writeString(name);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);
        switch (version) {
            case 1:
                value = os.readLong();
                incrementor = os.readLong();
                description = os.readString();
                name = os.readString();
        }
    }

    /**
     * @return
     */
    public int getSerializationVersion() {
        return SerializationVersion;
    }
}
