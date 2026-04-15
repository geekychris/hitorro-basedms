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
import gnu.trove.map.hash.TLongObjectHashMap;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.basefile.fs.sinks.LongObjectBaseFileStateSink;
import com.hitorro.util.basefile.tools.BaseFileUtil;
import com.hitorro.util.json.keys.BasefileProperty;
import com.hitorro.util.json.keys.IntegerProperty;
import com.hitorro.util.json.keys.StringProperty;
import com.hitorro.util.typesystem.Bag;

import java.io.IOException;

/**
 * Given two fields of a bag(long, Object) we apply these to long, string apply
 */
public class BagLongObjectBaseFileStateSink extends LongObjectBaseFileStateSink<Bag, String> {
    public static BasefileProperty BaseFileKey = new BasefileProperty("outputfile", "output file using basefile syntax");
    public static IntegerProperty SizeKey = new IntegerProperty("mapsize", "size of the apply", 1000000);
    public static StringProperty LongField = new StringProperty("longfield", "field which represents the long value", null);

    public static StringProperty ValueField = new StringProperty("valuefield", "field which represents the value field", null);
    private String longField;
    private String valueField;

    public BagLongObjectBaseFileStateSink(BaseFile outputFile, int size, String longField, String valueField) {
        super(outputFile, size);
        this.longField = longField;
        this.valueField = valueField;
    }

    public BagLongObjectBaseFileStateSink() {

    }

    @Override
    public boolean init(JsonNode node) {
        this.setBaseFile(BaseFileKey.apply(node));
        map = new TLongObjectHashMap(SizeKey.apply(node));
        longField = LongField.apply(node);
        valueField = ValueField.apply(node);
        return true;
    }

    @Override
    public boolean add(final Bag o) {
        Object l = o.getValue(o, longField);
        if (l == null || !(l instanceof Long)) {
            // skip this guy
            return true;
        }
        Object v = o.getValue(o, valueField);
        if (v == null) {
            return true;
        }
        this.map.put(((Long) l).longValue(), v.toString());
        return true;
    }

    @Override
    public boolean stop() throws IOException {
        return BaseFileUtil.writeTLongStringHashMap(outputFile, map);
    }
}
