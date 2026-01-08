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
package com.hitorro.basedms.contentpropertyextractor;

import com.hitorro.basedms.ContentProperties;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.core.Log;

import java.util.HashMap;
import java.util.Map;


public class ContentExtractorContext {
    private static ContentExtractorContext s_context = new ContentExtractorContext();

    private Map<String, ContentPropertiesExtractor> map = new HashMap<String, ContentPropertiesExtractor>();

    public static ContentExtractorContext getContext() {
        return s_context;
    }

    public void addExtractor(ContentPropertiesExtractor extractor) {
        map.put(extractor.getMimeType(), extractor);
    }

    public ContentPropertiesExtractor getExtractor(String mimeType) {
        return map.get(mimeType);
    }

    public boolean fluff(String mimeType, ContentProperties cp, BaseFile file) {
        if (!BaseFile.notNullAndExists(file)) {
            ContentPropertiesExtractor extractor = getExtractor(mimeType);
            if (extractor == null) {
                return false;
            }
            return extractor.extract(cp, file);
        }
        Log.util.error("File does not exist for %s ", cp);
        return false;
    }
}
