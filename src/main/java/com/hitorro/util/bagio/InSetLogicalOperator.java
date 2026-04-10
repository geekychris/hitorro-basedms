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
package com.hitorro.util.bagio;


import com.fasterxml.jackson.databind.JsonNode;
import gnu.trove.set.hash.TLongHashSet;
import com.hitorro.util.core.ArrayUtil;
import com.hitorro.util.core.opers.HTPredicate;


public class InSetLogicalOperator implements HTPredicate<Long> {
    private TLongHashSet set;

    public InSetLogicalOperator(TLongHashSet set) {
        this.set = set;
    }

    public void setFromString(String s, String seperator, boolean clear) {
        setFromLongs(ArrayUtil.getLongArrayFromSeperatedString(s, seperator), clear);
    }

    public void setFromLongs(long arr[], boolean clear) {
        if (set == null) {
            set = new TLongHashSet();
        }
        if (clear) {
            set.clear();
        }
        set.addAll(arr);
    }


    public void setSet(TLongHashSet set) {
        this.set = set;
    }

    @Override
    public void initForPass() {

    }

    @Override
    public boolean test(final Long aLong) {
        return set.contains(aLong);
    }

    @Override
    public boolean initFromMap(final JsonNode map) {
        return true;
    }
}
