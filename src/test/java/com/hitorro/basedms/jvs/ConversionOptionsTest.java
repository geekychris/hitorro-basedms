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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for ConversionOptions.
 */
public class ConversionOptionsTest {
    
    @Test
    public void testDefaultOptions() {
        ConversionOptions options = ConversionOptions.defaults();
        
        assertTrue(options.isIncludeCategories(), "Categories should be included by default");
        assertTrue(options.isIncludeContent(), "Content should be included by default");
        assertTrue(options.isExtractTextContent(), "Text extraction should be enabled by default");
        assertTrue(options.isIncludeVersionReferences(), "Version references should be included by default");
        assertTrue(options.isIncludeContainerReferences(), "Container references should be included by default");
    }
    
    @Test
    public void testBuilderWithAllFalse() {
        ConversionOptions options = ConversionOptions.builder()
                .includeCategories(false)
                .includeContent(false)
                .extractTextContent(false)
                .includeVersionReferences(false)
                .includeContainerReferences(false)
                .build();
        
        assertFalse(options.isIncludeCategories());
        assertFalse(options.isIncludeContent());
        assertFalse(options.isExtractTextContent());
        assertFalse(options.isIncludeVersionReferences());
        assertFalse(options.isIncludeContainerReferences());
    }
    
    @Test
    public void testBuilderPartialConfiguration() {
        ConversionOptions options = ConversionOptions.builder()
                .includeCategories(false)
                .extractTextContent(false)
                .build();
        
        assertFalse(options.isIncludeCategories());
        assertTrue(options.isIncludeContent(), "Should keep default value");
        assertFalse(options.isExtractTextContent());
        assertTrue(options.isIncludeVersionReferences(), "Should keep default value");
        assertTrue(options.isIncludeContainerReferences(), "Should keep default value");
    }
    
    @Test
    public void testBuilderChaining() {
        ConversionOptions options = ConversionOptions.builder()
                .includeCategories(true)
                .includeContent(true)
                .extractTextContent(true)
                .includeVersionReferences(false)
                .includeContainerReferences(false)
                .build();
        
        assertTrue(options.isIncludeCategories());
        assertTrue(options.isIncludeContent());
        assertTrue(options.isExtractTextContent());
        assertFalse(options.isIncludeVersionReferences());
        assertFalse(options.isIncludeContainerReferences());
    }
    
    @Test
    public void testMinimalOptions() {
        ConversionOptions options = ConversionOptions.builder()
                .includeCategories(false)
                .includeContent(false)
                .includeVersionReferences(false)
                .includeContainerReferences(false)
                .build();
        
        // Only core fields would be included (id, times, metadata)
        assertFalse(options.isIncludeCategories());
        assertFalse(options.isIncludeContent());
        assertFalse(options.isIncludeVersionReferences());
        assertFalse(options.isIncludeContainerReferences());
    }
}
