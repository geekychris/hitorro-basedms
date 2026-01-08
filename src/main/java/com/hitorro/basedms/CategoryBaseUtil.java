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
import com.hitorro.basedms.cache.DomainValueCache;
import com.hitorro.basedms.exceptions.CategoryException;
import com.hitorro.basedms.exceptions.InvalidValueCategoryException;
import com.hitorro.basedms.exceptions.UnknownDomainCategoryException;
import com.hitorro.util.core.valuemap.DomainValueIntf;
import com.hitorro.util.core.valuemap.ValueMap;
import com.hitorro.util.objects.DomainValue;

import java.util.Set;

/**
 */
public abstract class CategoryBaseUtil {
    /**
     * put a dv pair to the category listFiles.  This will get more complex to prevent dupes and appropriate behaviour for
     * labels.
     *
     * @param domain
     * @param value
     */
    public static final void addCategory(String domain, String value, CategoryBaseInterface intf) throws CategoryException {
        domain = domain.toLowerCase();
        value = value.toLowerCase();
        ValueMap<Category> cats = DomainValueCache.getCache().get(domain);
        if (cats == null) {
            throw new UnknownDomainCategoryException(domain);
        }

        if (!cats.validate(value)) {
            throw new InvalidValueCategoryException(domain, value);
        }
        Set<DomainValueIntf> categories = intf.getCategories();
        for (DomainValueIntf dv : categories) {
            if (dv.getDomain().equals(domain) && dv.getValue().equals(value)) {
                // we found the element already.
                return;
            }
        }
        intf.processUnique(cats, domain, value);

        categories.add(new DomainValue(domain, value));
    }

    /**
     * Evaluates if the domain value pair exists for this system object.
     *
     * @param domain
     * @param value
     * @return true if exists.
     */
    public static final boolean getCategoryValueExists(String domain, String value, CategoryBaseInterface intf) {
        return getDomainValueFromCategory(domain, value, intf) != null;
    }

    /**
     * Remove a category if it exists.
     *
     * @param domain
     * @param value
     * @return true if a category was removed.
     */
    public static final boolean removeCategory(String domain, String value, CategoryBaseInterface intf) {
        domain = domain.toLowerCase();
        value = value.toLowerCase();
        DomainValueIntf dv = getDomainValueFromCategory(domain, value, intf);
        if (dv != null) {
            intf.getCategories().remove(dv);
        }
        return false;
    }

    public static final boolean getCategoryDomainExists(String domain, CategoryBaseInterface intf) {
        for (DomainValueIntf dv : intf.getCategories()) {
            if (dv.getDomain().equals(domain)) {
                // we found the element already.
                return true;
            }
        }
        return false;
    }

    private static final DomainValueIntf getDomainValueFromCategory(String domain, String value, CategoryBaseInterface intf) {
        for (DomainValueIntf dv : intf.getCategories()) {
            if (dv.getDomain().equals(domain) && dv.getValue().equals(value)) {
                // we found the element already.
                return dv;
            }
        }
        return null;
    }
}
