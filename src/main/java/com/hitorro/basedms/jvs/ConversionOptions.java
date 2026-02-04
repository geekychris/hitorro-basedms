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

/**
 * Options for controlling DMS to JVS conversion behavior.
 */
public class ConversionOptions {
    
    private final boolean includeCategories;
    private final boolean includeContent;
    private final boolean extractTextContent;
    private final boolean includeVersionReferences;
    private final boolean includeContainerReferences;
    
    private ConversionOptions(Builder builder) {
        this.includeCategories = builder.includeCategories;
        this.includeContent = builder.includeContent;
        this.extractTextContent = builder.extractTextContent;
        this.includeVersionReferences = builder.includeVersionReferences;
        this.includeContainerReferences = builder.includeContainerReferences;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static ConversionOptions defaults() {
        return new Builder().build();
    }
    
    public boolean isIncludeCategories() {
        return includeCategories;
    }
    
    public boolean isIncludeContent() {
        return includeContent;
    }
    
    public boolean isExtractTextContent() {
        return extractTextContent;
    }
    
    public boolean isIncludeVersionReferences() {
        return includeVersionReferences;
    }
    
    public boolean isIncludeContainerReferences() {
        return includeContainerReferences;
    }
    
    public static class Builder {
        private boolean includeCategories = true;
        private boolean includeContent = true;
        private boolean extractTextContent = true;
        private boolean includeVersionReferences = true;
        private boolean includeContainerReferences = true;
        
        public Builder includeCategories(boolean include) {
            this.includeCategories = include;
            return this;
        }
        
        public Builder includeContent(boolean include) {
            this.includeContent = include;
            return this;
        }
        
        public Builder extractTextContent(boolean extract) {
            this.extractTextContent = extract;
            return this;
        }
        
        public Builder includeVersionReferences(boolean include) {
            this.includeVersionReferences = include;
            return this;
        }
        
        public Builder includeContainerReferences(boolean include) {
            this.includeContainerReferences = include;
            return this;
        }
        
        public ConversionOptions build() {
            return new ConversionOptions(this);
        }
    }
}
