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
import com.hitorro.util.core.iterator.mappers.BaseMapper;
import com.hitorro.util.core.opers.HTPredicate;
import com.hitorro.util.typesystem.Bag;

/**
 * HTPredicate container.  Allows filtering on a bags field by pulling the field value, optionally mapping it and then
 * calling the real logical operator to determine the test.
 */
public class BagFieldLogicalOperator<I, O> implements HTPredicate<Bag> {
    private String field;
    private HTPredicate<O> lo;
    private BaseMapper<I, O> mapper;

    public BagFieldLogicalOperator(String field, BaseMapper<I, O> mapper, HTPredicate<O> lo) {
        this.field = field;
        this.lo = lo;
        this.mapper = mapper;
    }

    @Override
    public void initForPass() {

    }

    @Override
    public boolean test(final Bag bag) {
        Object o = bag.getValue(bag, field);
        if (o != null) {
            if (mapper != null) {
                return lo.test(mapper.apply((I) o));
            }
            return lo.test((O) o);
        }
        return false;
    }

    @Override
    public boolean initFromMap(final JsonNode map) {
        return false;
    }
}
