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
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.HTSerializable;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;

import java.io.IOException;
import java.util.*;

/**
 * <p/>
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Oct 22, 2005 Time: 6:40:37 PM
 * <p/>
 * Request object for fetching objects by guid or other constraint and then potentially converting them through a base
 * adapter. (first user of this was an RSSItemInterface fetcher).
 */
@TypeClassMetaInfo(shortTypeName = "OF",
        isView = false,
        isPersisted = false,
        schemaVersion = ObjectFetcher.SerializationVersion)
public class ObjectFetcher implements HTSerializable {
    public static final int SerializationVersion = 1;

    private List<String> guid = new ArrayList<String>();
    private String adapterMethod;
    private Map<String, String> methodArgs = new HashMap<String, String>();
    private boolean includeContent = false;
    private boolean useCacheIfAvailable = true;
    private List<HTSerializable> results = new ArrayList<HTSerializable>();

    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(SerializationVersion);
        os.writeString(adapterMethod);
        os.writeListOfString(guid);
        os.writeListOfHTSerializable(results);
        os.writeBoolean(includeContent);
        os.writeBoolean(useCacheIfAvailable);
        os.writeInt(methodArgs.size());
        Set<Map.Entry<String, String>> set =
                methodArgs.entrySet();
        for (Map.Entry<String, String> entry : set) {
            os.writeString(entry.getKey());
            os.writeString(entry.getValue());
        }
    }

    public void deserialize(HTObjectInputStream os) throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        adapterMethod = os.readString();
        guid = os.readListOfStrings();
        results = os.readListOfHTSerializable();
        includeContent = os.readBoolean();
        useCacheIfAvailable = os.readBoolean();
        int argSize = os.readInt();
        methodArgs.clear();
        for (int i = 0; i < argSize; i++) {
            String key = os.readString();
            String value = os.readString();
            methodArgs.put(key, value);
        }
    }

    public void addToResult(HTSerializable pts) {
        results.add(pts);
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

    public List<String> getGuid() {
        return guid;
    }

    public void setGuid(List<String> guid) {
        this.guid = guid;
    }

    public String getAdapterMethod() {
        return adapterMethod;
    }

    public void setAdapterMethod(String adapterMethod) {
        this.adapterMethod = adapterMethod;
    }

    public Map<String, String> getMethodArgs() {
        return methodArgs;
    }

    public void setMethodArgs(Map<String, String> methodArgs) {
        this.methodArgs = methodArgs;
    }

    public boolean isIncludeContent() {
        return includeContent;
    }

    public void setIncludeContent(boolean includeContent) {
        this.includeContent = includeContent;
    }

    public boolean isUseCacheIfAvailable() {
        return useCacheIfAvailable;
    }

    public void setUseCacheIfAvailable(boolean useCacheIfAvailable) {
        this.useCacheIfAvailable = useCacheIfAvailable;
    }

    public List<HTSerializable> getResults() {
        return results;
    }

    public void setResults(List<HTSerializable> results) {
        this.results = results;
    }
}
