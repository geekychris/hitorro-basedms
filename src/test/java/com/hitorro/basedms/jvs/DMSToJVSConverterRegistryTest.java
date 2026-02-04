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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for DMSToJVSConverterRegistry.
 */
public class DMSToJVSConverterRegistryTest {
    
    private DMSToJVSConverterRegistry registry;
    
    @BeforeEach
    public void setUp() {
        registry = DMSToJVSConverterRegistry.getInstance();
        // Clear any custom converters from previous tests
        registry.clearConverters();
    }
    
    @Test
    public void testSingletonInstance() {
        DMSToJVSConverterRegistry registry1 = DMSToJVSConverterRegistry.getInstance();
        DMSToJVSConverterRegistry registry2 = DMSToJVSConverterRegistry.getInstance();
        
        assertSame(registry1, registry2, "Registry should be a singleton");
    }
    
    @Test
    public void testDefaultConverterRegistered() {
        assertTrue(registry.hasConverter(VersionableObject.class), 
                "Default converter should be registered for VersionableObject");
    }
    
    @Test
    public void testGetDefaultConverter() {
        DMSToJVSConverter<VersionableObject> converter = 
                registry.getConverter(VersionableObject.class);
        
        assertNotNull(converter, "Should return default converter");
        assertEquals(VersionableObject.class, converter.getTargetClass());
    }
    
    @Test
    public void testRegisterCustomConverter() {
        TestConverter testConverter = new TestConverter();
        registry.registerConverter(TestVersionableObject.class, testConverter);
        
        assertTrue(registry.hasConverter(TestVersionableObject.class), 
                "Custom converter should be registered");
        
        DMSToJVSConverter<TestVersionableObject> retrieved = 
                registry.getConverter(TestVersionableObject.class);
        
        assertSame(testConverter, retrieved, "Should return registered converter");
    }
    
    @Test
    public void testInheritanceBasedLookup() {
        // Register converter for subclass
        TestConverter testConverter = new TestConverter();
        registry.registerConverter(TestVersionableObject.class, testConverter);
        
        // Get converter for further subclass
        DMSToJVSConverter<TestSubclass> converter = 
                registry.getConverter(TestSubclass.class);
        
        assertSame(testConverter, converter, 
                "Should find parent class converter via inheritance");
    }
    
    @Test
    public void testFallbackToDefaultConverter() {
        // Get converter for unregistered subclass
        DMSToJVSConverter<TestVersionableObject> converter = 
                registry.getConverter(TestVersionableObject.class);
        
        assertNotNull(converter, "Should fall back to default converter");
        assertEquals(VersionableObject.class, converter.getTargetClass());
    }
    
    @Test
    public void testRemoveConverter() {
        TestConverter testConverter = new TestConverter();
        registry.registerConverter(TestVersionableObject.class, testConverter);
        
        assertTrue(registry.hasConverter(TestVersionableObject.class));
        
        registry.removeConverter(TestVersionableObject.class);
        
        assertFalse(registry.hasConverter(TestVersionableObject.class), 
                "Converter should be removed");
    }
    
    @Test
    public void testConverterCount() {
        int initialCount = registry.getConverterCount();
        
        TestConverter testConverter = new TestConverter();
        registry.registerConverter(TestVersionableObject.class, testConverter);
        
        assertEquals(initialCount + 1, registry.getConverterCount(), 
                "Converter count should increase");
    }
    
    @Test
    public void testClearConverters() {
        TestConverter testConverter = new TestConverter();
        registry.registerConverter(TestVersionableObject.class, testConverter);
        
        registry.clearConverters();
        
        // Should still have default converter
        assertTrue(registry.hasConverter(VersionableObject.class));
        // Custom converter should be removed
        assertFalse(registry.hasConverter(TestVersionableObject.class));
    }
    
    // Test classes
    
    static class TestVersionableObject extends VersionableObject {
    }
    
    static class TestSubclass extends TestVersionableObject {
    }
    
    static class TestConverter implements DMSToJVSConverter<TestVersionableObject> {
        
        @Override
        public JVS convert(TestVersionableObject dmsObject, ConversionContext context) 
                throws ConversionException {
            return new JVS();
        }
        
        @Override
        public Class<TestVersionableObject> getTargetClass() {
            return TestVersionableObject.class;
        }
    }
}
