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
package com.hitorro.basedms.cache;

import com.hitorro.base.objects.ContentType;
import com.hitorro.base.objects.Extension;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.ListUtil;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.typesystem.BaseSession;

import java.util.*;

public class ContentTypeCache {
    public static final String DefaultType = "text/plain";
    private static ContentTypeCache s_cache;
    private HashMap<String, ContentType> m_map = new HashMap<String, ContentType>();
    private HashMap<String, ContentType> m_mapGuid = new HashMap<String, ContentType>();
    private List<ContentType> m_ct = new ArrayList<ContentType>();

    public static synchronized ContentTypeCache getCache() {
        if (s_cache == null) {
            ContentType.createMimeTypes();
            s_cache = new ContentTypeCache();
            s_cache.retreiveRows();
        }
        return s_cache;
    }

    public ContentType getContentTypeByMimeType(String mimeType) {
        return m_map.get(mimeType.toLowerCase());
    }

    public ContentType getContentTypeByGuid(String guid) {
        return m_mapGuid.get(guid);
    }


    /**
     * Grab the first extension isInitialized for a mime type if the mime type exists and has file extensions
     *
     * @param mimeType
     * @return
     */
    public String getFileNameExtensionForContentType(String mimeType) {
        ContentType ct = m_map.get(mimeType.toLowerCase());
        if (ct == null) {
            return null;
        }
        Set<Extension> extensions = ct.getExtensions();
        Extension e = null;
        if (extensions != null && extensions.size() > 0) {
            Iterator<Extension> iter = extensions.iterator();

            while (iter.hasNext()) {
                e = iter.next();
                if (e.getIsPrefered()) {
                    return e.getFileExtension();
                }
            }
        }
        // last resort
        if (e != null) {
            return e.getFileExtension();
        }
        return null;
    }

    public ContentType getTypeFromFileWithDefault(String fileName) {
        String ext = FileUtil.getFileExtension(fileName);
        if (StringUtil.nullOrEmptyString(ext)) {
            return getDefault();
        }
        List<ContentType> types = getContentByExtension(ext);
        if (ListUtil.nullOrEmpty(types)) {
            return getDefault();
        }
        return types.get(0);
    }

    public ContentType getDefault() {
        return getContentTypeByMimeType(DefaultType);
    }

    /**
     * Query the cache of all content types for a specific file extension.
     *
     * @param extension
     * @return
     */
    public List<ContentType> getContentByExtension(String extension) {
        List<ContentType> ctl = new ArrayList<ContentType>();
        for (ContentType ct : m_ct) {
            if (ct.hasExtension(extension)) {
                ctl.add(ct);
            }
        }
        return ctl;
    }

    private void retreiveRows() {
        BaseSession session = DMSSessionFactory.getFactory().getSession();
        try {
            Iterator iter = session.getIteratorFromQuery("from " + ContentType.class.getCanonicalName());
            while (iter.hasNext()) {
                ContentType ct = (ContentType) iter.next();
                m_ct.add(ct);
                m_map.put(ct.getMimeType(), ct);
                m_mapGuid.put(ct.getSoftGuid(), ct);
            }
        } finally {
            DMSSessionFactory.getFactory().rollbackCloseSession(session);
        }
    }
}
