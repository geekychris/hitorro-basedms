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

import com.hitorro.base.objects.VersionableObject;
import com.hitorro.jsontypesystem.JVS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for DMSToJVSMapper facade.
 * 
 * NOTE: Tests that use the converter are disabled because they require
 * the full JVS properties environment to be configured.
 */
public class DMSToJVSMapperTest {
    
    @BeforeEach
    public void setUp() {
        // Clear any custom converters
        DMSToJVSConverterRegistry.getInstance().clearConverters();
    }
    
    @Test
    public void testConvertNullObject() {
        assertThrows(ConversionException.class, () -> {
            DMSToJVSMapper.convert(null);
        }, "Should throw exception for null object");
    }
    
    @Test
    @Disabled("Requires JVS properties environment - run in integration test context")
    public void testConvertSingleObject() throws ConversionException {
        VersionableObject obj = new VersionableObject();
        obj.setGuid("test-guid");
        obj.setCreator("testUser");
        
        JVS jvs = DMSToJVSMapper.convert(obj);
        
        assertNotNull(jvs);
        assertEquals("test-guid", jvs.getString("id.did"));
        assertEquals("testUser", jvs.getString("metadata.creator"));
    }
    
    @Test
    @Disabled("Requires JVS properties environment - run in integration test context")
    public void testConvertWithOptions() throws ConversionException {
        VersionableObject obj = new VersionableObject();
        obj.setGuid("test-guid");
        
        ConversionOptions options = ConversionOptions.builder()
                .includeCategories(false)
                .includeContent(false)
                .build();
        
        JVS jvs = DMSToJVSMapper.convert(obj, options);
        
        assertNotNull(jvs);
        assertEquals("test-guid", jvs.getString("id.did"));
    }
    
    @Test
    @Disabled("Requires JVS properties environment - run in integration test context")
    public void testConvertWithContext() throws ConversionException {
        VersionableObject obj = new VersionableObject();
        obj.setGuid("test-guid");
        
        ConversionContext context = ConversionContext.builder()
                .options(ConversionOptions.defaults())
                .build();
        
        JVS jvs = DMSToJVSMapper.convert(obj, context);
        
        assertNotNull(jvs);
        assertEquals("test-guid", jvs.getString("id.did"));
    }
    
    @Test
    public void testConvertBatchEmpty() throws ConversionException {
        List<VersionableObject> emptyList = new ArrayList<>();
        
        List<JVS> result = DMSToJVSMapper.convertBatch(emptyList);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    public void testConvertBatchNull() throws ConversionException {
        List<JVS> result = DMSToJVSMapper.convertBatch(null);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    @Disabled("Requires JVS properties environment - run in integration test context")
    public void testConvertBatchMultipleObjects() throws ConversionException {
        VersionableObject obj1 = new VersionableObject();
        obj1.setGuid("guid-1");
        obj1.setCreator("user1");
        
        VersionableObject obj2 = new VersionableObject();
        obj2.setGuid("guid-2");
        obj2.setCreator("user2");
        
        VersionableObject obj3 = new VersionableObject();
        obj3.setGuid("guid-3");
        obj3.setCreator("user3");
        
        List<VersionableObject> objects = Arrays.asList(obj1, obj2, obj3);
        
        List<JVS> result = DMSToJVSMapper.convertBatch(objects);
        
        assertNotNull(result);
        assertEquals(3, result.size());
        
        assertEquals("guid-1", result.get(0).getString("id.did"));
        assertEquals("guid-2", result.get(1).getString("id.did"));
        assertEquals("guid-3", result.get(2).getString("id.did"));
        
        assertEquals("user1", result.get(0).getString("metadata.creator"));
        assertEquals("user2", result.get(1).getString("metadata.creator"));
        assertEquals("user3", result.get(2).getString("metadata.creator"));
    }
    
    @Test
    @Disabled("Requires JVS properties environment - run in integration test context")
    public void testConvertBatchWithOptions() throws ConversionException {
        VersionableObject obj1 = new VersionableObject();
        obj1.setGuid("guid-1");
        
        VersionableObject obj2 = new VersionableObject();
        obj2.setGuid("guid-2");
        
        List<VersionableObject> objects = Arrays.asList(obj1, obj2);
        
        ConversionOptions options = ConversionOptions.builder()
                .includeCategories(false)
                .build();
        
        List<JVS> result = DMSToJVSMapper.convertBatch(objects, options);
        
        assertNotNull(result);
        assertEquals(2, result.size());
    }
    
    @Test
    @Disabled("Requires JVS properties environment - run in integration test context")
    public void testConvertBatchWithContext() throws ConversionException {
        VersionableObject obj1 = new VersionableObject();
        obj1.setGuid("guid-1");
        
        VersionableObject obj2 = new VersionableObject();
        obj2.setGuid("guid-2");
        
        List<VersionableObject> objects = Arrays.asList(obj1, obj2);
        
        ConversionContext context = ConversionContext.withDefaults();
        
        List<JVS> result = DMSToJVSMapper.convertBatch(objects, context);
        
        assertNotNull(result);
        assertEquals(2, result.size());
    }
    
    @Test
    public void testRegisterCustomConverter() {
        TestConverter converter = new TestConverter();
        
        DMSToJVSMapper.registerConverter(TestVersionableObject.class, converter);
        
        assertTrue(DMSToJVSMapper.hasConverter(TestVersionableObject.class));
    }
    
    @Test
    public void testHasConverterForUnregistered() {
        assertFalse(DMSToJVSMapper.hasConverter(TestVersionableObject.class));
    }
    
    @Test
    public void testHasConverterForBaseClass() {
        assertTrue(DMSToJVSMapper.hasConverter(VersionableObject.class),
                "Default converter should be registered");
    }
    
    @Test
    public void testConvertWithCustomConverter() throws ConversionException {
        TestConverter converter = new TestConverter();
        DMSToJVSMapper.registerConverter(TestVersionableObject.class, converter);
        
        TestVersionableObject obj = new TestVersionableObject();
        obj.setGuid("test-guid");
        
        JVS jvs = DMSToJVSMapper.convert(obj);
        
        assertNotNull(jvs);
        // Custom converter should have been used
    }
    
    // Test classes
    
    static class TestVersionableObject extends VersionableObject {
    }
    
    static class TestConverter implements DMSToJVSConverter<TestVersionableObject> {
        
        @Override
        public JVS convert(TestVersionableObject dmsObject, ConversionContext context) 
                throws ConversionException {
            JVS jvs = new JVS();
            try {
                jvs.set("id.did", dmsObject.getGuid());
                jvs.set("id.domain", "test");
                jvs.set("customField", "customValue");
            } catch (Exception e) {
                throw new ConversionException("Conversion failed", e);
            }
            return jvs;
        }
        
        @Override
        public Class<TestVersionableObject> getTargetClass() {
            return TestVersionableObject.class;
        }
    }
}
