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
package com.hitorro.basedms.jvs;

import com.hitorro.basedms.jvs.content.ContentTextExtractor;
import com.hitorro.jsontypesystem.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * Context for DMS to JVS conversion providing access to options and resources.
 */
public class ConversionContext {
    
    private final ConversionOptions options;
    private final ContentTextExtractor contentExtractor;
    private final Map<String, Type> typeCache;
    
    private ConversionContext(Builder builder) {
        this.options = builder.options;
        this.contentExtractor = builder.contentExtractor;
        this.typeCache = builder.typeCache != null ? builder.typeCache : new HashMap<>();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static ConversionContext withDefaults() {
        return new Builder().build();
    }
    
    public ConversionOptions getOptions() {
        return options;
    }
    
    public ContentTextExtractor getContentExtractor() {
        return contentExtractor;
    }
    
    public Map<String, Type> getTypeCache() {
        return typeCache;
    }
    
    public static class Builder {
        private ConversionOptions options = ConversionOptions.defaults();
        private ContentTextExtractor contentExtractor;
        private Map<String, Type> typeCache;
        
        public Builder options(ConversionOptions options) {
            this.options = options;
            return this;
        }
        
        public Builder contentExtractor(ContentTextExtractor extractor) {
            this.contentExtractor = extractor;
            return this;
        }
        
        public Builder typeCache(Map<String, Type> cache) {
            this.typeCache = cache;
            return this;
        }
        
        public ConversionContext build() {
            // Set default content extractor if not provided
            if (contentExtractor == null) {
                contentExtractor = new com.hitorro.basedms.jvs.content.DefaultContentTextExtractor();
            }
            return new ConversionContext(this);
        }
    }
}
