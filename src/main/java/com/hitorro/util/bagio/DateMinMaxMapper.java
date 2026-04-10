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

import com.hitorro.util.core.date.DateResolution;
import com.hitorro.util.core.iterator.mappers.BaseMapper;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.typesystem.Bag;

import java.text.ParseException;
import java.util.Date;


public class DateMinMaxMapper extends BaseMapper<Bag, Bag> {
    private Date min;
    private Date max;
    private String minPath = null;
    private String maxPath = null;

    public DateMinMaxMapper(String minPath, String maxPath) {
        if (!StringUtil.nullOrEmptyString(minPath)) {
            this.minPath = minPath;
        }
        if (!StringUtil.nullOrEmptyString(maxPath)) {
            this.maxPath = maxPath;
        }
        reset();
    }

    public void reset() {
        try {
            min = DateResolution.Year.parse("3000");
            max = DateResolution.Year.parse("1979");
        } catch (ParseException e) {
        }
    }

    public Date getMin() {
        return min;
    }

    public Date getMax() {
        return max;
    }

    @Override
    public Bag apply(final Bag e) {
        if (minPath != null) {
            Object o = e.getValue(e, minPath);
            if (o != null && o instanceof Date) {
                Date l = (Date) o;
                if (l.before(min)) {
                    min = l;
                }
            }
        }
        if (maxPath != null) {
            Object o = e.getValue(e, maxPath);
            if (o != null && o instanceof Date) {
                Date l = (Date) o;
                if (l.after(max)) {
                    max = l;
                }
            }
        }
        return e;
    }
}