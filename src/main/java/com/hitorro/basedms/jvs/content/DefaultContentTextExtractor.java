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
package com.hitorro.basedms.jvs.content;

import com.hitorro.base.objects.Content;
import com.hitorro.basedms.cache.ContentTypeCache;

import java.util.HashSet;
import java.util.Set;

/**
 * Default implementation of ContentTextExtractor that handles common text-based content types.
 */
public class DefaultContentTextExtractor implements ContentTextExtractor {
    
    private final Set<String> indexableContentTypes;
    
    public DefaultContentTextExtractor() {
        this.indexableContentTypes = new HashSet<>();
        // Initialize default indexable content types
        indexableContentTypes.add("text/plain");
        indexableContentTypes.add("text/html");
        indexableContentTypes.add("text/xml");
        indexableContentTypes.add("application/xml");
        indexableContentTypes.add("application/json");
        indexableContentTypes.add("text/csv");
        indexableContentTypes.add("text/markdown");
    }
    
    public DefaultContentTextExtractor(Set<String> customIndexableTypes) {
        this.indexableContentTypes = new HashSet<>(customIndexableTypes);
    }
    
    @Override
    public String extractText(Content content) {
        if (content == null) {
            return null;
        }
        
        // Get content type
        String mimeType = getMimeType(content);
        if (mimeType == null || !isIndexable(mimeType)) {
            return null;
        }
        
        // Use Content's built-in text extraction if it supports it
        if (content.hasStringValue()) {
            return content.getStringValue();
        }
        
        return null;
    }
    
    @Override
    public boolean isIndexable(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        
        // Check exact match first
        if (indexableContentTypes.contains(mimeType.toLowerCase())) {
            return true;
        }
        
        // Check prefix match for text/* types
        String lowerMime = mimeType.toLowerCase();
        if (lowerMime.startsWith("text/")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get MIME type from Content object.
     */
    private String getMimeType(Content content) {
        try {
            if (content.getContentType() != null) {
                return content.getContentType().getMimeType();
            }
        } catch (Exception e) {
            // Fall back to literal if content type lookup fails
        }
        
        // Try getting content type and extracting mime type from it
        // Note: getContentTypeLiteral() is package-private, so we use getContentType() instead
        
        return null;
    }
    
    /**
     * Add a custom indexable content type.
     */
    public void addIndexableContentType(String mimeType) {
        indexableContentTypes.add(mimeType.toLowerCase());
    }
    
    /**
     * Remove a content type from indexable list.
     */
    public void removeIndexableContentType(String mimeType) {
        indexableContentTypes.remove(mimeType.toLowerCase());
    }
}
