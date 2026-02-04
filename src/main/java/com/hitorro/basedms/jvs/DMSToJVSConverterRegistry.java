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
import com.hitorro.basedms.jvs.converters.VersionableObjectConverter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe registry for DMS to JVS converters.
 * Supports inheritance-based lookup with caching.
 */
public class DMSToJVSConverterRegistry {
    
    private static final DMSToJVSConverterRegistry INSTANCE = new DMSToJVSConverterRegistry();
    
    // Exact class to converter mapping
    private final ConcurrentMap<Class<? extends VersionableObject>, DMSToJVSConverter<?>> converters;
    
    // Lookup cache for resolved converters (including inherited)
    private final ConcurrentMap<Class<? extends VersionableObject>, DMSToJVSConverter<?>> lookupCache;
    
    // Default fallback converter
    private final DMSToJVSConverter<VersionableObject> defaultConverter;
    
    private DMSToJVSConverterRegistry() {
        this.converters = new ConcurrentHashMap<>();
        this.lookupCache = new ConcurrentHashMap<>();
        this.defaultConverter = new VersionableObjectConverter();
        
        // Register default converter for base class
        registerConverter(VersionableObject.class, defaultConverter);
    }
    
    /**
     * Get the singleton instance.
     */
    public static DMSToJVSConverterRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * Register a converter for a specific DMS class.
     * 
     * @param dmsClass the DMS class
     * @param converter the converter to register
     * @param <T> the DMS type
     */
    public <T extends VersionableObject> void registerConverter(
            Class<T> dmsClass, 
            DMSToJVSConverter<T> converter) {
        converters.put(dmsClass, converter);
        // Clear lookup cache when new converter is registered
        lookupCache.clear();
    }
    
    /**
     * Get a converter for a specific DMS class.
     * Searches up the class hierarchy if exact match not found.
     * 
     * @param dmsClass the DMS class
     * @param <T> the DMS type
     * @return converter for the class
     */
    @SuppressWarnings("unchecked")
    public <T extends VersionableObject> DMSToJVSConverter<T> getConverter(Class<T> dmsClass) {
        // Check cache first
        DMSToJVSConverter<?> cached = lookupCache.get(dmsClass);
        if (cached != null) {
            return (DMSToJVSConverter<T>) cached;
        }
        
        // Look for exact match
        DMSToJVSConverter<?> converter = converters.get(dmsClass);
        if (converter != null) {
            lookupCache.put(dmsClass, converter);
            return (DMSToJVSConverter<T>) converter;
        }
        
        // Walk up class hierarchy
        Class<?> currentClass = dmsClass.getSuperclass();
        while (currentClass != null && VersionableObject.class.isAssignableFrom(currentClass)) {
            converter = converters.get(currentClass);
            if (converter != null) {
                // Cache the result
                lookupCache.put(dmsClass, converter);
                return (DMSToJVSConverter<T>) converter;
            }
            currentClass = currentClass.getSuperclass();
        }
        
        // Fall back to default converter
        lookupCache.put(dmsClass, defaultConverter);
        return (DMSToJVSConverter<T>) defaultConverter;
    }
    
    /**
     * Check if a converter is registered for a class (exact match only).
     * 
     * @param dmsClass the DMS class
     * @return true if exact converter exists
     */
    public boolean hasConverter(Class<? extends VersionableObject> dmsClass) {
        return converters.containsKey(dmsClass);
    }
    
    /**
     * Remove a converter for a specific class.
     * 
     * @param dmsClass the DMS class
     */
    public void removeConverter(Class<? extends VersionableObject> dmsClass) {
        converters.remove(dmsClass);
        lookupCache.clear();
    }
    
    /**
     * Clear all registered converters (except default).
     */
    public void clearConverters() {
        converters.clear();
        lookupCache.clear();
        // Re-register default
        registerConverter(VersionableObject.class, defaultConverter);
    }
    
    /**
     * Get the number of registered converters.
     */
    public int getConverterCount() {
        return converters.size();
    }
}
