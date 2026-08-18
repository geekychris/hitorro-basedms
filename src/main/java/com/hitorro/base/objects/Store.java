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
import com.hitorro.jsontypesystem.propreaders.JVSProperties;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.basefile.fs.BaseFileSystem;
import com.hitorro.util.core.Env;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.BaseType;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.Type;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;

import jakarta.persistence.*;

import java.io.File;
import java.io.IOException;

@Entity
@Table(name = "Store")
@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.Store,
        isView = false,
        isPersisted = true,
        schemaVersion = Store.SerializationVersion,
        softLinkField = "name",
        guidAccessor = GuidAccessor.class)
public class Store extends BaseType {
    public static final int SerializationVersion = 1;
    
    @Transient
    private transient File openResourceLocation = null;

    @Column(name = "defaultStore")
    private boolean defaultStore = false;
    
    @Column(name = "storeType")
    private String storeType;
    
    @Column(name = "rootPath")
    private String rootPath;
    
    @Column(name = "docRoot")
    private String docRoot;
    
    @Column(name = "name")
    private String name;
    
    @Transient
    private BaseFile rootDir;
    
    @Transient
    private String rootDirAsString;
    
    @Column(name = "isPubliclyVisible")
    private boolean isPubliclyVisible;
    
    @Column(name = "offline")
    private boolean offline = false;
    
    // not persisted
    @Transient
    private StoreType type;

    public Store() {
    }

    public boolean getOffline() {
        return offline;
    }

    public void setOffline(boolean flag) {
        offline = flag;
    }

    public boolean getIsPubliclyVisible() {
        return isPubliclyVisible;
    }

    public void setIsPubliclyVisible(boolean flag) {
        isPubliclyVisible = flag;
    }

    public String toString() {
        return Fmt.S("{\"store\"=\"%s\", \"type\"=\"%s\", \"root\"=\"%s\"}", name, storeType, rootDir);
    }

    /**
     * Only valid if we have a Store type of  File
     *
     * @return
     */
    public BaseFile getRootPathPath() {
        return rootDir;
    }

    public String getSoftGuid(Type type) {
        TypeClassMetaInfo meta = type.getTypeMeta();
        return Fmt.S("%s:s:%s", meta.shortTypeName(), getName());
    }

    public boolean validate() throws StoreException {
        if (StoreType.get(storeType) == null) {
            throw new StoreException(Fmt.S("Unknown store type", storeType));
        }
        return true;
    }

    public StoreType getStoreTypeType() {
        return type;
    }

    /**
     * File system needs to be qualified with protocol type.
     */
    @PostLoad
    public void init() {
        type = StoreType.get(storeType);
        // KVStore needs the same rootDir resolution — its RocksDB
        // directory lives under <rootDir>/rocksdb/ via KvStoreBackend.
        if (type.isFileStore() || type.isUnmanagedFileStore() || type.isKvStore()) {
            // JVSProperties may be null in test / embedded contexts —
            // fall through to the raw rootPath then.
            var jvsProps = JVSProperties.getProperties();
            rootDirAsString = jvsProps != null
                    ? jvsProps.resolveJsonVariable(rootPath)
                    : rootPath;
            try {
                rootDir = BaseFileSystem.getBaseFileFromPath(rootDirAsString);
            } catch (IOException e) {
                Log.basedms.error("Unable to create root path for file system %s, path %s with error %s %e",
                        name, rootDirAsString, e, e);
                return;
            }
            if (!rootDir.mkdir()) {
                Log.basedms.error("Unable to create root path for file system %s, path %s",
                        name, rootDirAsString);
            }
        }
        // create a reference in the openresource
        if (this.isPubliclyVisible) {
            File f = getOpenResourceLocation();
            FileUtil.ensureDirectoryExists(f);
        }
    }


    public File getOpenResourceLocation() {
        if (openResourceLocation == null) {
            openResourceLocation = new File(Env.getOpenResourceDir(), Fmt.S("stores/%s", this.name));
        }
        return openResourceLocation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getDefaultStore() {
        return defaultStore;
    }

    public void setDefaultStore(boolean defaultVal) {
        defaultStore = defaultVal;
    }

    public String getStoreType() {
        return storeType;
    }

    public void setStoreType(String store) {
        storeType = store;
    }

    public String getDocRoot() {
        return docRoot;
    }

    public void setDocRoot(String path) {
        docRoot = path;
    }

    /**
     * Shouldnt be visible to the outside world as its untranslated.
     *
     * @return
     */
    String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String path) {
        rootPath = path;
    }

    public String getRootDir() {
        return rootDirAsString;
    }


    public void serialize(HTObjectOutputStream os)
            throws IOException {
        os.writeBoolean(defaultStore);
        os.writeBoolean(isPubliclyVisible);
        os.writeString(storeType);
        os.writeString(rootPath);
        os.writeString(docRoot);
        os.writeString(name);
    }

    public void deserialize(HTObjectInputStream os, int version)
            throws IOException, ClassNotFoundException {
        switch (version) {
            case 1:
                defaultStore = os.readBoolean();
                isPubliclyVisible = os.readBoolean();
                storeType = os.readString();
                rootPath = os.readString();
                docRoot = os.readString();
                name = os.readString();
        }
        init();
    }

    public int getSerializationVersion() {
        return SerializationVersion;
    }
}
