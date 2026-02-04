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

import com.hitorro.basedms.jvs.content.DefaultContentTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for DefaultContentTextExtractor.
 */
public class DefaultContentTextExtractorTest {
    
    private DefaultContentTextExtractor extractor;
    
    @BeforeEach
    public void setUp() {
        extractor = new DefaultContentTextExtractor();
    }
    
    @Test
    public void testIsIndexableForTextPlain() {
        assertTrue(extractor.isIndexable("text/plain"), "text/plain should be indexable");
    }
    
    @Test
    public void testIsIndexableForTextHtml() {
        assertTrue(extractor.isIndexable("text/html"), "text/html should be indexable");
    }
    
    @Test
    public void testIsIndexableForApplicationJson() {
        assertTrue(extractor.isIndexable("application/json"), "application/json should be indexable");
    }
    
    @Test
    public void testIsIndexableForApplicationXml() {
        assertTrue(extractor.isIndexable("application/xml"), "application/xml should be indexable");
    }
    
    @Test
    public void testIsIndexableForTextWithSubtype() {
        assertTrue(extractor.isIndexable("text/csv"), "text/csv should be indexable");
        assertTrue(extractor.isIndexable("text/markdown"), "text/markdown should be indexable");
        assertTrue(extractor.isIndexable("text/javascript"), "text/javascript should be indexable");
    }
    
    @Test
    public void testIsNotIndexableForBinary() {
        assertFalse(extractor.isIndexable("application/pdf"), "application/pdf should not be indexable");
        assertFalse(extractor.isIndexable("image/png"), "image/png should not be indexable");
        assertFalse(extractor.isIndexable("video/mp4"), "video/mp4 should not be indexable");
        assertFalse(extractor.isIndexable("audio/mp3"), "audio/mp3 should not be indexable");
    }
    
    @Test
    public void testIsIndexableForNull() {
        assertFalse(extractor.isIndexable(null), "null should not be indexable");
    }
    
    @Test
    public void testIsIndexableCaseInsensitive() {
        assertTrue(extractor.isIndexable("TEXT/PLAIN"), "Should be case insensitive");
        assertTrue(extractor.isIndexable("Text/Html"), "Should be case insensitive");
        assertTrue(extractor.isIndexable("APPLICATION/JSON"), "Should be case insensitive");
    }
    
    @Test
    public void testCustomIndexableTypes() {
        Set<String> customTypes = new HashSet<>();
        customTypes.add("application/custom");
        customTypes.add("text/special");
        
        DefaultContentTextExtractor customExtractor = new DefaultContentTextExtractor(customTypes);
        
        assertTrue(customExtractor.isIndexable("application/custom"), 
                "Custom type should be indexable");
        assertTrue(customExtractor.isIndexable("text/special"), 
                "Custom type should be indexable");
        assertFalse(customExtractor.isIndexable("text/plain"), 
                "Default types should not be included");
    }
    
    @Test
    public void testAddIndexableContentType() {
        extractor.addIndexableContentType("application/custom");
        
        assertTrue(extractor.isIndexable("application/custom"), 
                "Added type should be indexable");
    }
    
    @Test
    public void testRemoveIndexableContentType() {
        assertTrue(extractor.isIndexable("text/plain"));
        
        extractor.removeIndexableContentType("text/plain");
        
        assertFalse(extractor.isIndexable("text/plain"), 
                "Removed type should not be indexable");
    }
    
    @Test
    public void testExtractTextFromNull() {
        String text = extractor.extractText(null);
        assertNull(text, "Should return null for null content");
    }
}
