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
import com.hitorro.util.typesystem.Bag;


public class Bag2PrimitiveTMapper<S> extends BaseMapper<Bag, Bag> {
    private BaseMapper<S, S> sideMapper;
    private String field;

    public Bag2PrimitiveTMapper(String field, BaseMapper<S, S> sideMapper) {
        this.sideMapper = sideMapper;
        this.field = field;
    }

    @Override
    public Bag apply(final Bag e) {
        S o = (S) e.getValue(e, field);
        if (o != null) {
            sideMapper.apply(o);
        }
        return e;
    }
}


