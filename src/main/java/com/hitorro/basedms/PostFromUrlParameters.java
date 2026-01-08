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
package com.hitorro.basedms;

import com.hitorro.basedms.queue.GroupId;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.job.JobParameters;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;
import com.hitorro.util.typesystem.annotation.UiProperties;
import com.hitorro.util.urlparser.URLUtil;

import java.io.IOException;

/**
 */

@TypeClassMetaInfo(shortTypeName = "PostFromUrlParameters",
        isView = false,
        isPersisted = true,
        schemaVersion = PostFromUrlParameters.SerializationVersion)
public class PostFromUrlParameters extends JobParameters implements GroupId {
    public static final int SerializationVersion = 1;
    private String m_readUrl;
    private int m_retries;
    private int m_type;
    private int m_degree;
    private String m_parentGuid;
    private String m_containerGuid;
    private long m_authoredDate;
    private String m_guid;
    private transient String m_site = null;

    public PostFromUrlParameters() {
        m_readUrl = "";
    }

    public Object getGroupId() {
        if (m_site == null) {
            m_site = URLUtil.getSiteFromURL(m_readUrl);
        }
        return m_site;
    }

    @UiProperties(displayName = "Read URL", displayType = UiProperties.TextFieldDisplay)
    public String getReadUrl() {
        return m_readUrl;
    }

    public void setReadUrl(String url) {
        m_readUrl = url;
    }

    public String getGuid() {
        return m_guid;
    }

    public void setGuid(String guid) {
        m_guid = guid;
    }

    public int getRetries() {
        return m_retries;
    }

    public void setRetries(int retries) {
        m_retries = retries;
    }

    public int getType() {
        return m_type;
    }

    public void setType(int parentType) {
        m_type = parentType;
    }

    public int getDegree() {
        return m_degree;
    }

    public void setDegree(int parentDegree) {
        m_degree = parentDegree;
    }

    public String getParentGuid() {
        return m_parentGuid;
    }

    public void setParentGuid(String guid) {
        m_parentGuid = guid;
    }

    public String getContainerGuid() {
        return m_containerGuid;
    }

    public void setContainerGuid(String guid) {
        m_containerGuid = guid;
    }

    public long getAuthoredDate() {
        return m_authoredDate;
    }

    public void setAuthoredDate(long time) {
        m_authoredDate = time;
    }

    // ----------------- HTSerializable

    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeString(m_readUrl);
        os.writeInt(m_retries);
        os.writeInt(m_type);
        os.writeInt(m_degree);
        os.writeString(m_parentGuid);
        os.writeString(m_containerGuid);
        os.writeLong(m_authoredDate);
        os.writeString(m_guid);
    }

    public void deserialize(HTObjectInputStream os) throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);
        switch (version) {
            case 1:
                m_readUrl = os.readString();
                m_retries = os.readInt();
                m_type = os.readInt();
                m_degree = os.readInt();
                m_parentGuid = os.readString();
                m_containerGuid = os.readString();
                m_authoredDate = os.readLong();
                m_guid = os.readString();
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

    public String getJobName() {
        return PostFromUrlFetcherAppJob.PostFromUrlFetcherAppJob;
    }
}

