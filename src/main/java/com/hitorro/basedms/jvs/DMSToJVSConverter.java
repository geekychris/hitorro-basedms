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

/**
 * Strategy interface for converting DMS objects to JVS format.
 * 
 * @param <T> DMS type extending VersionableObject
 */
public interface DMSToJVSConverter<T extends VersionableObject> {
    
    /**
     * Convert a DMS object to JVS format.
     * 
     * @param dmsObject the DMS object to convert
     * @param context conversion context with options and resources
     * @return JVS representation of the DMS object
     * @throws ConversionException if conversion fails
     */
    JVS convert(T dmsObject, ConversionContext context) throws ConversionException;
    
    /**
     * Get the DMS class this converter handles.
     * 
     * @return DMS class
     */
    Class<T> getTargetClass();
}
