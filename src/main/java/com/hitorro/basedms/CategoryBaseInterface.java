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
package com.hitorro.basedms;

import com.hitorro.base.objects.Category;
import com.hitorro.basedms.exceptions.CategoryException;
import com.hitorro.util.core.valuemap.DomainValueIntf;
import com.hitorro.util.core.valuemap.ValueMap;

import java.util.Set;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Nov 7, 2006 Time: 7:44:30 AM
 * <p/>
 * Interface defining the base methods we need to access categories.
 */
public interface CategoryBaseInterface {
    Set<DomainValueIntf> getCategories();

    void setCategories(Set<DomainValueIntf> cat);

    /**
     * put a dv pair to the category listFiles.  This will get more complex to prevent dupes and appropriate behaviour for
     * labels.
     *
     * @param domain
     * @param value
     */
    void addCategory(String domain, String value) throws CategoryException;

    void processUnique(ValueMap<Category> cats, String domain, String value);

    /**
     * Evaluates if the domain value pair exists for this system object.
     *
     * @param domain
     * @param value
     * @return true if exists.
     */
    boolean getCategoryValueExists(String domain, String value);

    /**
     * Remove a category if it exists.
     *
     * @param domain
     * @param value
     * @return true if a category was removed.
     */
    boolean removeCategory(String domain, String value);
}
