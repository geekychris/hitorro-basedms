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

import com.hitorro.base.typesystem.GuidBaseType;
import com.hitorro.base.typesystem.accessors.GuidAccessor;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.Env;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;

import jakarta.persistence.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Entity
@Table(name = "contenttype")
@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.ContentType,
        isView = false,
        isPersisted = true,
        schemaVersion = ContentType.SerializationVersion,
        softLinkField = "mimeType",
        guidAccessor = GuidAccessor.class)
public class ContentType extends GuidBaseType {
    public static final int SerializationVersion = 1;
    public static final String MimeTypeUnknown = "unknown/unknown";
    public static final String MimeTypeFlash = "application/x-shockwave-flash";
    public static final String MimeTypeMP3 = "audio/mpeg";
    public static final String MimeTypeMP4 = "video/mp4";
    public static final String MimeTypeQuickTime = "video/quicktime";
    public static final String MimeTypeWMV = "video/x-ms-wmv";
    public static final String MimeTypeJpeg = "image/jpeg";
    public static final String MimeTypePng = "image/png";
    public static final String MimeTypeTiff = "image/tiff";
    public static final String MimeJavaSerializedObject = "application/java-serialized-object";
    public static final String MimeTypeAll = "all/all";

    @Column(name = "mimeType", nullable = false)
    private String mimeType;
    
    @OneToMany(mappedBy = "contentType", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Extension> fileExtensions;

    public static boolean importMimeTypes(File mimeTypeFile, BaseSession session) throws FileNotFoundException {
        // first ensure that the mime types have been loaded into the system.
        boolean complete = false;

        Iterator<String> lrt = FileUtil.getLineReaderIteratorFromFile(mimeTypeFile);

        if (lrt != null) {
            while (lrt.hasNext()) {
                String row = lrt.next();
                if (row.startsWith("#")) {
                    continue;
                }
                // do not remove space
                String tokens[] = StringUtil.tokenizeFromSingleChar(row, "\t ", true);
                if (tokens != null && tokens.length > 0) {
                    ContentType ct = new ContentType();
                    ct.setMimeType(tokens[0]);
                    if (tokens.length > 1) {
                        Set<Extension> ext = new HashSet<Extension>();
                        for (int i = 1; i < tokens.length; i++) {
                            Extension e = new Extension();
                            e.setFileExtension(tokens[i]);
                            ext.add(e);
                            if (i == 1) {
                                e.setIsPrefered(true);
                            } else {
                                e.setIsPrefered(false);
                            }
                            e.setContentType(ct);
                        }
                        ct.setExtensions(ext);
                    }

                    session.persist(ct);
                }
            }

        }
        session.commit();
        complete = true;

        return complete;
    }

    public static void createMimeTypes() {
        BaseSession session = DMSSessionFactory.getFactory().getSession();

        try {
            if (session.getTableRowCount(ContentType.class.getCanonicalName()) == 0) {
                File mimeFile = new File(Env.getBin(), "data/mimetype.txt");
                try {
                    ContentType.importMimeTypes(mimeFile, session);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            DMSSessionFactory.closeSession(session);
        }

    }

    public String toString() {
        return mimeType;
    }

    /**
     * See if this content type has an extension that matches the query.
     *
     * @param extension
     * @return true if this content type has this extension
     */
    public boolean hasExtension(String extension) {
        extension = extension.toLowerCase();
        for (Extension e : fileExtensions) {
            String ext = e.getFileExtension();
            if (!StringUtil.nullOrEmptyString(ext) && ext.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    public Set<Extension> getExtensions() {
        return fileExtensions;
    }

    public void setExtensions(Set<Extension> fileExtensions) {
        this.fileExtensions = fileExtensions;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeString(mimeType);
        os.writeSetOfBaseType(fileExtensions);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);
        switch (version) {
            case 1:
                mimeType = os.readString();
                fileExtensions = new HashSet<Extension>();
                os.readSetOfHTSerializable(fileExtensions);
        }
    }

    public int getSerializationVersion() {
        return SerializationVersion;
    }
}
