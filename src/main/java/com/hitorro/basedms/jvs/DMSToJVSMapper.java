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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Main facade for converting DMS objects to JVS format.
 * Provides simple static methods for conversion.
 */
public class DMSToJVSMapper {
    
    /**
     * Convert a single DMS object to JVS using default options.
     * 
     * @param dmsObject the DMS object to convert
     * @param <T> the DMS type
     * @return JVS representation
     * @throws ConversionException if conversion fails
     */
    public static <T extends VersionableObject> JVS convert(T dmsObject) throws ConversionException {
        return convert(dmsObject, ConversionOptions.defaults());
    }
    
    /**
     * Convert a single DMS object to JVS with custom options.
     * 
     * @param dmsObject the DMS object to convert
     * @param options conversion options
     * @param <T> the DMS type
     * @return JVS representation
     * @throws ConversionException if conversion fails
     */
    public static <T extends VersionableObject> JVS convert(T dmsObject, ConversionOptions options) 
            throws ConversionException {
        ConversionContext context = ConversionContext.builder()
                .options(options)
                .build();
        return convert(dmsObject, context);
    }
    
    /**
     * Convert a single DMS object to JVS with custom context.
     * 
     * @param dmsObject the DMS object to convert
     * @param context conversion context
     * @param <T> the DMS type
     * @return JVS representation
     * @throws ConversionException if conversion fails
     */
    @SuppressWarnings("unchecked")
    public static <T extends VersionableObject> JVS convert(T dmsObject, ConversionContext context) 
            throws ConversionException {
        if (dmsObject == null) {
            throw new ConversionException("Cannot convert null DMS object");
        }
        
        // Get converter from registry
        DMSToJVSConverter<T> converter = DMSToJVSConverterRegistry.getInstance()
                .getConverter((Class<T>) dmsObject.getClass());
        
        // Perform conversion
        return converter.convert(dmsObject, context);
    }
    
    /**
     * Convert a collection of DMS objects to JVS using default options.
     * 
     * @param dmsObjects collection of DMS objects
     * @param <T> the DMS type
     * @return list of JVS representations
     * @throws ConversionException if any conversion fails
     */
    public static <T extends VersionableObject> List<JVS> convertBatch(Collection<T> dmsObjects) 
            throws ConversionException {
        return convertBatch(dmsObjects, ConversionOptions.defaults());
    }
    
    /**
     * Convert a collection of DMS objects to JVS with custom options.
     * 
     * @param dmsObjects collection of DMS objects
     * @param options conversion options
     * @param <T> the DMS type
     * @return list of JVS representations
     * @throws ConversionException if any conversion fails
     */
    public static <T extends VersionableObject> List<JVS> convertBatch(
            Collection<T> dmsObjects, 
            ConversionOptions options) throws ConversionException {
        ConversionContext context = ConversionContext.builder()
                .options(options)
                .build();
        return convertBatch(dmsObjects, context);
    }
    
    /**
     * Convert a collection of DMS objects to JVS with custom context.
     * Uses a shared context for all conversions for efficiency.
     * 
     * @param dmsObjects collection of DMS objects
     * @param context conversion context
     * @param <T> the DMS type
     * @return list of JVS representations
     * @throws ConversionException if any conversion fails
     */
    public static <T extends VersionableObject> List<JVS> convertBatch(
            Collection<T> dmsObjects, 
            ConversionContext context) throws ConversionException {
        if (dmsObjects == null || dmsObjects.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<JVS> results = new ArrayList<>(dmsObjects.size());
        
        for (T dmsObject : dmsObjects) {
            JVS jvs = convert(dmsObject, context);
            results.add(jvs);
        }
        
        return results;
    }
    
    /**
     * Register a custom converter for a DMS class.
     * 
     * @param dmsClass the DMS class
     * @param converter the converter to register
     * @param <T> the DMS type
     */
    public static <T extends VersionableObject> void registerConverter(
            Class<T> dmsClass, 
            DMSToJVSConverter<T> converter) {
        DMSToJVSConverterRegistry.getInstance().registerConverter(dmsClass, converter);
    }
    
    /**
     * Check if a converter is registered for a DMS class.
     * 
     * @param dmsClass the DMS class
     * @return true if a converter is registered
     */
    public static boolean hasConverter(Class<? extends VersionableObject> dmsClass) {
        return DMSToJVSConverterRegistry.getInstance().hasConverter(dmsClass);
    }
}
