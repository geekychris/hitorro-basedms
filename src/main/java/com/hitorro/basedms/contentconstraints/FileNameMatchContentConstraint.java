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
 */
@TypeClassMetaInfo(shortTypeName = "FNContentConstraint",
        isView = false,
        isPersisted = false,
        schemaVersion = FileNameMatchContentConstraint.SerializationVersion)

public class FileNameMatchContentConstraint implements HTPredicate<Content>, HTSerializable {

    public static final int SerializationVersion = 1;
    private String name;
    private boolean ignoreCase;

    public FileNameMatchContentConstraint() {

    }

    public FileNameMatchContentConstraint(String s, boolean ignoreCase) {
        setName(s);
        this.setIgnoreCase(ignoreCase);
    }

    public boolean initFromMap(final JsonNode map) {
        // MUST IMPLEMENT
        HTAssert.assertThat(false, "FileNameMatchContentConstraint.initFromMap not implemented");
        return false;
    }

    public void initForPass() {

    }

    public String toString() {
        return Fmt.S("FileNameMatch(%s,ignoreCase=%s)", name, ignoreCase);
    }

    public boolean test(Content c) {
        if (isIgnoreCase()) {
            return getName().equalsIgnoreCase(c.getOriginalFileName());
        } else {
            return getName().equals(c.getOriginalFileName());
        }
    }


    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        os.writeString(getName());
        os.writeBoolean(isIgnoreCase());
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        switch (version) {
            case 1:
                setName(os.readString());
                setIgnoreCase(os.readBoolean());
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }
}