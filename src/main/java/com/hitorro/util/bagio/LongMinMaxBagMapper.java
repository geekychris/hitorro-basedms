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

import com.hitorro.util.core.iterator.mappers.BaseMapper;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.typesystem.Bag;


public class LongMinMaxBagMapper extends BaseMapper<Bag, Bag> {
    private long min = Long.MAX_VALUE;
    private long max = Long.MIN_VALUE;
    private String minPath = null;
    private String maxPath = null;

    public LongMinMaxBagMapper(String minPath, String maxPath) {
        if (!StringUtil.nullOrEmptyString(minPath)) {
            this.minPath = minPath;
        }
        if (!StringUtil.nullOrEmptyString(maxPath)) {
            this.maxPath = maxPath;
        }
    }

    public void reset() {
        min = Long.MAX_VALUE;
        max = Long.MIN_VALUE;
    }

    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
    }

    @Override
    public Bag apply(final Bag e) {
        if (minPath != null) {
            Object o = e.getValue(e, minPath);
            if (o != null && o instanceof Long) {
                long l = (Long) o;
                if (l < min) {
                    min = l;
                }
            }
        }
        if (maxPath != null) {
            Object o = e.getValue(e, maxPath);
            if (o != null && o instanceof Long) {
                long l = (Long) o;
                if (l > max) {
                    max = l;
                }
            }
        }
        return e;
    }
}