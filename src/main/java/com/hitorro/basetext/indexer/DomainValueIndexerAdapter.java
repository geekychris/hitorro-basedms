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
package com.hitorro.basetext.indexer;

import com.hitorro.util.objects.DomainValue;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import java.util.Collection;
import java.util.Iterator;

/**
 * <p/>
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Oct 7, 2005 Time: 9:29:25 AM
 * <p/>
 * Given a listFiles of DomainValue objects, index them as numerous string literals.  Any DV field we dont put in the main
 * content (we ignore the flag!)
 */
public class DomainValueIndexerAdapter extends IndexerFieldAdapter {
    public static final Class DVAdapter = DomainValueIndexerAdapter.class;

    public void index(boolean isDate, Object valueRaw, boolean stored, boolean indexed, Document doc, String field, boolean literal, boolean addToDefaultField) {
        Collection<DomainValue> col = (Collection<DomainValue>) valueRaw;

        Iterator<DomainValue> iter = col.iterator();
        while (iter.hasNext()) {
            DomainValue dv = iter.next();
            String tuple = dv.getAsIndexTuple();
            if (stored || indexed) {
                doc.add(new Field(field, tuple,
                        IndexerFieldAdapter.getIndexEnum(indexed, stored, literal)));
            }
        }
    }
}
