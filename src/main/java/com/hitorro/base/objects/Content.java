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
import com.hitorro.basedms.cache.ContentTypeCache;
import com.hitorro.basedms.contentpropertyextractor.ContentExtractorContext;
import com.hitorro.basedms.exceptions.CategoryException;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.basefile.fs.file.FileFile;
import com.hitorro.util.core.Console;
import com.hitorro.util.core.Platform;
import com.hitorro.util.core.UTCDateUtil;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.core.valuemap.DomainValueIntf;
import com.hitorro.util.core.valuemap.ValueMap;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.io.IOUtil;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.OnTrigger;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * A content object
 *
 * @author chris
 */
@com.hitorro.util.typesystem.annotation.TypeClassMetaInfo(shortTypeName = com.hitorro.util.typesystem.annotation.TypeClassMetaInfo.Content,
        onTriggers = {@com.hitorro.util.typesystem.annotation.ImplClassMeta(className = com.hitorro.basedms.BaseTypeOnTriggerGeneric.class, trigger = OnTrigger.TriggerType.OnNew),
                @com.hitorro.util.typesystem.annotation.ImplClassMeta(className = com.hitorro.basedms.BaseTypeOnTriggerGeneric.class, trigger = OnTrigger.TriggerType.BeforeSave),
                @com.hitorro.util.typesystem.annotation.ImplClassMeta(className = com.hitorro.basedms.BaseTypeOnTriggerGeneric.class, trigger = OnTrigger.TriggerType.BeforeDelete),
                @com.hitorro.util.typesystem.annotation.ImplClassMeta(className = com.hitorro.basedms.BaseTypeOnTriggerGeneric.class, trigger = OnTrigger.TriggerType.BeforePersist),
                @com.hitorro.util.typesystem.annotation.ImplClassMeta(className = com.hitorro.basedms.BaseTypeOnTriggerGeneric.class, trigger = OnTrigger.TriggerType.OnLoad)},
        isView = false,
        isPersisted = true,
        schemaVersion = Content.SerializationVersion)
@com.hitorro.util.typesystem.annotation.UiTypeProperties(name = "Content",
        views = {@com.hitorro.util.typesystem.annotation.ViewClassReference(name = com.hitorro.util.typesystem.annotation.ViewClassReference.ListView, viewClass = Content.ContentListView.class),
                @com.hitorro.util.typesystem.annotation.ViewClassReference(name = com.hitorro.util.typesystem.annotation.ViewClassReference.EditView, viewClass = Content.ContentEditView.class),
                @com.hitorro.util.typesystem.annotation.ViewClassReference(name = com.hitorro.util.typesystem.annotation.ViewClassReference.InspectView, viewClass = Content.ContentInspectView.class)})
public class Content extends GuidBaseType implements com.hitorro.basedms.CategoryBaseInterface, com.hitorro.basedms.ContentProperties {
    public static final int SerializationVersion = 1;
    private static com.hitorro.basedms.NamedLong ContentIdNamedLong = com.hitorro.basedms.NamedLong.registerNamedLong("fscontentid", 1, 100, "unique content file name ids");
    protected Set<DomainValueIntf> categories = new HashSet<DomainValueIntf>();
    protected int referenceCount = 0;
    // transients.
    private transient BaseFile internalFile = null;
    private transient File linkFile = null;
    private Blob blob;
    private String storeName;
    private String originalFileName;
    // filename where it currently resides
    private String fileName;
    private Date createDate = new Date();
    private String contentTypeLiteral;
    private Set<Content> renditions = new HashSet<Content>();
    private Content parentRendition;
    private String resolutionAux = new String();
    private int width = 0;
    private int height = 0;
    private int bitRate = 0;
    private int durationSeconds = 0;
    private String codec;
    private long contentSize = 0;

    private Set<VersionableObject> versionableObjects = new HashSet<VersionableObject>();

    public Content() {
        if (BaseDMSService.s_initialized) {
            // only do basic initialization if we know that the dms service is initialized.
            init();
        }
    }

    /**
     * Attempt to populate the content object with any attributes we can figure out from the file.
     *
     * @return
     */
    public boolean fluff() {
        Set<Content> renditions = this.getRenditions();
        for (Content c : renditions) {
            c.fluff();
        }
        return ContentExtractorContext.getContext().fluff(this.getContentType().getMimeType(), this, getContentFile());
    }

    /**
     * Delete the content of this content file.  Only should be used if this content object has not yet been saved.
     */
    public boolean delete() throws IOException {
        Store s = getStore();
        if (s.getStoreTypeType() == StoreType.File) {
            return getStoreFile(s).delete();
        }
        return false;
    }

    public int getReferenceCount() {
        return referenceCount;
    }

    public void setReferenceCount(int val) {
        referenceCount = val;
    }


    public void incrementRefCount() {
        referenceCount++;
    }


    public void decrementRefCount() {
        referenceCount--;
    }

    public void delete(DMSSession session) {
        for (Content c : renditions) {
            c.delete(session);
        }
        super.delete(session);
    }

    private void init() {
        this.setStoreName(com.hitorro.basedms.StoreUtil.getDefaultStore().getSoftGuid());
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public long getContentSize() {
        return contentSize;
    }

    public void setContentSize(long contentSize) {
        this.contentSize = contentSize;
    }

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int dur) {
        durationSeconds = dur;
    }

    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "Duration (mm:ss)", displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextFieldDisplay, order = 50)
    public String getDurationMinutes() {
        return UTCDateUtil.secondsIntToString(getDurationSeconds(), false);
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Resolution of the content if that is known (such as 48kbs for cd quality audio, 320x240)
     *
     * @return
     */
    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "Resolution", displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextFieldDisplay, order = 60)
    public String getResolutionAux() {
        return resolutionAux;
    }

    public void setResolutionAux(String resolution) {
        resolutionAux = resolution;
    }


    public boolean hasStringValue() {
        ContentType ct = getContentType();
        String mimet = ct.getMimeType();
        return mimet.indexOf("text") >= 0 || mimet.indexOf("html") >= 0;
    }

    public String getStringValue() {
        if (!hasStringValue()) {
            // this content doesn't content text
            return null;
        }
        try {
            InputStream is = getContent();
            if (is == null) {
                return null;
            }
            InputStreamReader reader = new InputStreamReader(is, StringUtil.UTF8Encoding);
            StringBuilder builder = new StringBuilder();
            int blen = 1024;
            char buffer[] = new char[blen];
            while (true) {
                int read = reader.read(buffer, 0, blen);
                if (read > 0) {
                    builder.append(buffer, 0, read);
                }
                if (read < blen) {
                    break;
                }
            }
            reader.close();

            return builder.toString();

        } catch (StoreException se) {
            com.hitorro.basedms.Log.basedms.error("Getting content ", se);
            return null;
        } catch (IOException ioe) {
            com.hitorro.basedms.Log.basedms.error("Getting content ", ioe);
            return null;
        }

    }

    ///////////////////////// OPEN LINKS /////////////////////////////////

    /**
     * Determines if this content object can be linked to the open world.
     *
     * @return
     */
    public boolean canBeLinked() {
        return getStore().getIsPubliclyVisible();
    }


    /**
     * Contains a link in the open link directory.
     *
     * @return
     */
    public boolean hasLink() {
        return getLinkFile().exists();
    }

    /**
     * Currently only works if the file is on a regular file system and not HDFS.
     *
     * @return
     * @throws IOException
     */
    public boolean createLink() throws IOException {
        File linkFile = getLinkFile();

        BaseFile sf = getStoreFile();
        if (!sf.isLocal()) {
            com.hitorro.basedms.Log.filesystem.error("Attempted to soft link non local file %s to %s", sf, linkFile);
            return false;
        }

        File sfFile = ((FileFile) sf).getJavaFile();
        if (!FileUtil.ensureParentDirectories(linkFile, true)) {
            return false;
        }
        return Platform.getPlatform().softLink(sfFile, getLinkFile());
    }

    public boolean removeLink() {
        File f = getLinkFile();
        if (f.exists()) {
            return f.delete();
        }
        return false;
    }


    public File getLinkFile() {
        if (linkFile == null) {
            linkFile = new File(getStore().getOpenResourceLocation(), fileName);
        }
        return linkFile;
    }


    public Set<Content> getRenditions() {
        return renditions;
    }

    public void setRenditions(Set<Content> renditions) {
        this.renditions = renditions;
    }


    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "Uploaded", displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextFieldDisplay, order = 10)
    public Date getCreationDate() {
        return createDate;
    }

    protected void setCreationDate(Date date) {
        createDate = date;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String m_fileName) {
        this.fileName = m_fileName;
    }

    /**
     * Get the file as an absolute url defined using docroot + filename we may not want to do this long term as this
     * exposes our content.
     *
     * @return
     */
    public String getExternalURL() {
        Store s = this.getStore();
        StoreType st = s.getStoreTypeType();
        if (st == null) {
            return null;
        }
        switch (st) {
            case Blob:
                return null;

            case File:
            case Unmanaged:
                String root = s.getDocRoot();
                String fn = this.getOriginalFileName();
                return Fmt.S("%s%s", root, fn);
            default:
                return this.getOriginalFileName();
        }
    }

    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "Original Name", displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextFieldDisplay, order = 5)
    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = StringUtil.truncateToLength(originalFileName, 1024);
    }

    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "Mime Type", displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextFieldDisplay, order = 30)
    public ContentType getContentType() {
        ContentTypeCache cache = ContentTypeCache.getCache();
        return cache.getContentTypeByGuid(getContentTypeLiteral());
    }

    public void setContentType(ContentType ct) {
        if (ct == null) {
            Console.println("Null content in setContentType");
        }
        setContentTypeLiteral(ct.getSoftGuid());
    }

    String getContentTypeLiteral() {
        return contentTypeLiteral;
    }

    void setContentTypeLiteral(String s) {
        this.contentTypeLiteral = s;
    }

    public Blob getBlobContent() {
        return blob;
    }

    public void setBlobContent(Blob blob) {
        this.blob = blob;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String store) {
        storeName = store;
    }

    public Store getStore() {
        return com.hitorro.basedms.StoreUtil.getStoreByGuid(storeName);
    }

    /**
     * Convenience method to set the store name
     *
     * @param store
     */
    public void setStore(Store store) {
        setStoreName(store.getName());
    }

    public boolean setContent(String originalFileName, BaseFile file, ContentType ct)
            throws StoreException, IOException {
        return setContent(originalFileName, file.getDataInputStream(), ct);
    }

    public boolean setContent(String originalFileName, File file, ContentType ct)
            throws StoreException, IOException {
        return setContent(originalFileName, FileUtil.getBufferedFileInputStream(file), ct);
    }

    public Content setContentRendition(BaseSession session,
                                       ContentType type,
                                       BaseFile file,
                                       String resolutionAux)
            throws IOException, StoreException {
        Content c = new Content();
        c.setResolutionAux(resolutionAux);
        this.getRenditions().add(c);
        c.setContent(file.getName(), file, type);
        session.persist(c);
        return c;
    }

    public Content setContentRendition(BaseSession session,
                                       ContentType type,
                                       File file,
                                       String resolutionAux)
            throws IOException, StoreException {
        Content c = new Content();
        c.setResolutionAux(resolutionAux);
        this.getRenditions().add(c);
        c.setContent(file.getName(), file, type);
        session.persist(c);
        return c;
    }


    public Content setContent(DMSSession session,
                              ContentType type,
                              InputStream is,
                              String resolution)
            throws IOException, StoreException {
        Content c = new Content();
        this.getRenditions().add(c);
        c.setContent(this.getOriginalFileName(), is, type);
        session.persist(c);
        return c;
    }

    /**
     * write content to a file
     *
     * @param f
     * @return
     * @throws StoreException
     * @throws IOException
     * @throws FileNotFoundException
     */
    public boolean getContent(File f)
            throws FileNotFoundException, IOException, StoreException {
        OutputStream os = FileUtil.getBufferedFileOutputStream(f, false);
        IOUtil.copyStream(getContent(), os);
        os.flush();
        os.close();
        return true;
    }

    public boolean getContent(BaseFile f)
            throws IOException, StoreException {
        OutputStream os = f.getDataOutputStream();
        IOUtil.copyStream(getContent(), os);
        os.flush();
        os.close();
        return true;
    }

    public BaseFile getContentFile() {
        Store store = this.getStore();
        StoreType st = store.getStoreTypeType();
        if (st.isFileStore() || st.isUnmanagedFileStore()) {
            return getStoreFile(store);
        } else {
            // cant get a file handle as its not on disk
            return null;
        }
    }

    /**
     * Get the size of the content.  If the flag is true, it will actually ask the backing storage for its size and not
     * rely upon the objects pre-computed value
     *
     * @param fromStore
     * @return
     */
    public long getContentSize(boolean fromStore) {
        if (fromStore) {
            Store s = this.getStore();
            StoreType st = s.getStoreTypeType();
            switch (st) {
                case Blob:

                    try {
                        return blob.length();
                    } catch (SQLException e) {
                    }
                    break;
                case File:

                    BaseFile f = getStoreFile(s);
                    if (f.exists()) {
                        return f.length();
                    }
                    break;
            }
        } else {
            return getContentSize();
        }
        return 0;
    }


    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "Size (kb)", displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextFieldDisplay, order = 40)
    public long getContentSizeFromStoreKB() {
        long bytes = getContentSize(true);

        return bytes / 1024;
    }


    /**
     * Get a stream which represents the content the content object contains.
     *
     * @return
     * @throws StoreException
     * @throws FileNotFoundException
     */
    public InputStream getContent()
            throws StoreException, IOException {
        Store store = com.hitorro.basedms.StoreUtil.getStoreByGuid(storeName);
        if (store == null) {
            throw new StoreException("Store not defined for content");
        }

        if (store.getOffline() == true) {
            throw new StoreException(Fmt.S("Store %s offline", store.getName()));
        }

        StoreType st = store.getStoreTypeType();
        if (st == null) {
            com.hitorro.basedms.Log.basedms.error("Content object %s, does not have a store object", this.getId());
            return null;
        }
        switch (st) {
            case Link:
                try {
                    URL url = new URL(getOriginalFileName());
                    return url.openStream();
                } catch (MalformedURLException e) {
                    throw new StoreException(e.getMessage());
                } catch (IOException e) {
                    throw new StoreException(e.getMessage());
                }

            case File:
            case Unmanaged:
                Store s = this.getStore();
                BaseFile file = getStoreFile(s);
                if (!BaseFile.notNullAndExists(file)) {
                    return null;
                }
                return file.getDataInputStream();

            case Blob:
                try {
                    return this.getBlobContent().getBinaryStream();
                } catch (SQLException e) {
                    throw new StoreException(e.getMessage());
                }
        }
        return null;
    }


    private final BaseFile getStoreFile() {
        return getStoreFile(getStore());
    }

    private final BaseFile getStoreFile(Store s) {
        if (internalFile == null) {
            internalFile = s.getRootPathPath().getChild(this.getFileName());
        }
        return internalFile;
    }

    public boolean setContent(String originalFileName, InputStream is, ContentType ct)
            throws StoreException, IOException {
        Store store = setContentBasicParts(originalFileName, ct);

        return setContentAux(store, originalFileName, is);
    }

    private Store setContentBasicParts(String originalFileName, ContentType ct) throws StoreException {
        this.setOriginalFileName(originalFileName);
        this.setContentType(ct);
        Store store = getStore();
        if (store == null) {
            throw new StoreException("Store not defined for content");
        }
        if (store.getOffline() == true) {
            throw new StoreException(Fmt.S("Store %s offline", store.getName()));
        }
        return store;
    }

    private boolean setContentAux(Store store, String originalFileName, InputStream is)
            throws StoreException, IOException {
        StoreType st = store.getStoreTypeType();
        switch (st) {
            case Link:
                throw new StoreException("Cannot set content for a content object with store type of Link");
            case Unmanaged:
                throw new StoreException("Cannot set content for a content object with store type of UnmanagedLink");
            case File:
                long tempID = 0;
                try {
                    tempID = ContentIdNamedLong.getNextValue();
                } catch (Exception e) {
                    com.hitorro.basedms.Log.basedms.error("%s %e", e, e);
                }
                String fn = FileUtil.idToHexPath(tempID);
                if (store.getIsPubliclyVisible()) {
                    // public files need to have a good extension so they can be understood by the client
                    String ext = FileUtil.getFileExtension(originalFileName);
                    if (!StringUtil.nullOrEmptyOrBlankString(ext)) {
                        fn = Fmt.S("%s.%s", fn, ext);
                    }
                }
                this.setFileName(fn);
                BaseFile destFile = store.getRootPathPath().getChild(fn);

                if (destFile.mkParentDir()) {
                    OutputStream os = destFile.getDataOutputStream();

                    IOUtil.copyStream(is, os);
                    os.flush();
                    os.close();
                    contentSize = destFile.length();
                    return true;
                } else {
                    throw new StoreException(Fmt.S("Unable to write file %s", destFile.getAbsolutePath()));
                }
            case Blob:

                //TODO UPDATE
                //Blob blob = Hibernate.createBlob(is);
                Blob blob = null;
                this.setBlobContent(blob);
                try {
                    contentSize = blob.length();
                } catch (SQLException e) {
                    throw new StoreException(Fmt.S("Unable to get blob size %s %e", e, e));
                }
                return true;
        }
        return false;
    }

    /**
     * @param os
     */

    /**
     * private Blob m_blob;
     *
     * @param os
     */
    public void serialize(HTObjectOutputStream os)
            throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        boolean writeContent = os.includeContent();

        os.writeString(storeName);
        os.writeString(originalFileName);
        os.writeString(fileName);
        os.writeDate(createDate);
        os.writeString(contentTypeLiteral);
        os.writeSetOfBaseType(renditions);
        os.writeVersionedObject(parentRendition);
        os.writeSetOfBaseType(versionableObjects);
        os.writeSetOfDomainValue(categories);
        os.writeInt(width);
        os.writeInt(height);
        os.writeInt(bitRate);
        os.writeInt(durationSeconds);
        os.writeString(codec);
        os.writeInt(referenceCount);

        // now lets write the content....or not
        if (writeContent) {
            Store s = this.getStore();
            StoreType st = s.getStoreTypeType();
            long length = getContentSize(true);
            os.writeLong(length);

            if (length > 0) {
                switch (st) {
                    case Blob:
                    case File:
                        InputStream is = getContent();


                        if (is != null) {
                            os.writeInputStream(is);
                        } else {
                            // XXX TODO remove
                            boolean foo = true;
                        }

                        break;
                }
            }
        } else {
            // we dont write out the content
            os.writeLong(0);
        }
    }

    public void deserialize(HTObjectInputStream is)
            throws IOException, ClassNotFoundException, StoreException {
        int version = is.readInt();
        super.deserialize(is);
        switch (version) {
            case 1:
                storeName = is.readString();
                originalFileName = is.readString();
                fileName = is.readString();
                createDate = is.readDate();
                contentTypeLiteral = is.readString();
                is.readSetOfHTSerializable(renditions);
                parentRendition = (Content) is.readVersionedObject();
                is.readSetOfHTSerializable(versionableObjects);
                is.readSetOfDomainValue(categories);
                width = is.readInt();
                height = is.readInt();
                bitRate = is.readInt();
                durationSeconds = is.readInt();
                codec = is.readString();
                referenceCount = is.readInt();
                // now lets read the content .... or not
                long bytes = is.readLong();

                if (bytes > 0) {
                    if (is.ignoreInlineContent()) {
                        is.skipNBytes(bytes);
                    } else {
                        Store st = getStore();
                        if (st == null) {
                            Store defaultStore = com.hitorro.basedms.StoreUtil.getDefaultStore();
                            com.hitorro.basedms.Log.basedms.warn("Deserializer Unable to retrieve store %s, switching to %s",
                                    getStore(), defaultStore.getSoftGuid());
                            st = defaultStore;
                            setStore(st);
                        }
                        InputStream wrapper = is.getLimitedInputStream(bytes);
                        this.setContentAux(st, this.originalFileName, wrapper);
                    }
                }

        }


    }

    public int getSerializationVersion() {
        return SerializationVersion;
    }

    // category stuff

    public Set<DomainValueIntf> getCategories() {
        return categories;
    }

    public void setCategories(Set<DomainValueIntf> cat) {
        categories = cat;
    }

    public void addCategory(String domain, String value) throws CategoryException {
        com.hitorro.basedms.CategoryBaseUtil.addCategory(domain, value, this);
    }

    public void processUnique(ValueMap<Category> cats, String domain, String value) {
        //do nothing.
    }

    /**
     * Evaluates if the domain value pair exists for this system object.
     *
     * @param domain
     * @param value
     * @return true if exists.
     */
    public boolean getCategoryValueExists(String domain, String value) {
        return com.hitorro.basedms.CategoryBaseUtil.getCategoryValueExists(domain, value, this);
    }

    public boolean getCategoryDomainExists(String domain) {
        return com.hitorro.basedms.CategoryBaseUtil.getCategoryDomainExists(domain, this);
    }

    /**
     * Remove a category if it exists.
     *
     * @param domain
     * @param value
     * @return true if a category was removed.
     */
    public boolean removeCategory(String domain, String value) {
        return com.hitorro.basedms.CategoryBaseUtil.removeCategory(domain, value, this);
    }

    public Set<VersionableObject> getVersionableObjects() {
        return versionableObjects;
    }

    public void setVersionableObjects(Set<VersionableObject> versionableObjects) {
        this.versionableObjects = versionableObjects;
    }

    public Content getParentRendition() {
        return parentRendition;
    }

    public void setParentRendition(Content parentRendition) {
        this.parentRendition = parentRendition;
    }


    /**
     * View class enumerating which fields to show when editing
     */
    @com.hitorro.util.typesystem.annotation.TypeClassMetaInfo(shortTypeName = "ContentListView",
            isView = true,
            isPersisted = false,
            schemaVersion = SerializationVersion)
    public abstract static class ContentListView {
        public abstract Date getCreationDate();


        public abstract String getOriginalFileName();


        public abstract String getContentSizeFromStoreKB();


        public abstract String getContentType();


        public abstract int getDurationMinutes();


        public abstract String getResolutionAux();
    }

    /**
     * View class enumerating which fields to show when editing
     */
    @com.hitorro.util.typesystem.annotation.TypeClassMetaInfo(shortTypeName = "ContentEditView",
            isView = true,
            isPersisted = false,
            schemaVersion = SerializationVersion)
    public abstract static class ContentEditView {
        public abstract Date getCreationDate();


        public abstract String getOriginalFileName();


        public abstract String getContentSizeFromStoreKB();


        public abstract String getContentType();


        public abstract int getDurationMinutes();


        public abstract String getResolutionAux();
    }

    /**
     * View class enumerating which fields to show when viewing
     */
    @com.hitorro.util.typesystem.annotation.TypeClassMetaInfo(shortTypeName = "ContentInspectView",
            isView = true,
            isPersisted = false,
            schemaVersion = SerializationVersion)
    public abstract static class ContentInspectView {
        public abstract Date getCreationDate();


        public abstract String getOriginalFileName();


        public abstract String getContentSizeFromStoreKB();


        public abstract String getContentType();


        public abstract int getDurationMinutes();


        public abstract String getResolutionAux();
    }
}
