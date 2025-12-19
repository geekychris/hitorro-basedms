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

import com.hitorro.language.IsoLanguage;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <p/>
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Oct 7, 2005 Time: 9:28:50 AM
 */
public class IndexerFieldAdapter {
    public static final String DEFAULT_SEARCH_FIELD = "content";
    public static final DateTools.Resolution IndexingDateResolution = DateTools.Resolution.HOUR;

    public static final Field.Store getStore(boolean stored) {
        if (stored) {
            return Field.Store.YES;
        }
        return Field.Store.NO;
    }

    public static final IFT getIndexEnum(boolean indexed, boolean stored, boolean literal) {
        IFT ift = new IFT();
        if (!literal) {
            ift.setTokenized();
        }
        if (stored) {
            ift.setStored();
        }
        if (indexed) {

            ift.setIndexDoc();
        }
        return ift;
    }

    public void index(boolean isDate, Object valueRaw, boolean stored, boolean indexed, List<IndexableField> addMe,
                      String field, boolean literal, boolean addToDefaultField, IsoLanguage language) {
        String value;
        if (isDate) {
            value = DateTools.dateToString((Date) valueRaw, IndexingDateResolution);
        } else {
            value = valueRaw.toString();
        }

        if (stored || indexed) {
            //TODO Update
            addMe.add(new Field(field, value,
                    getIndexEnum(indexed, stored, literal)));
        }

        if (addToDefaultField) {

            //TODO Update
            addMe.add(new Field(DEFAULT_SEARCH_FIELD,
                    value, getIndexEnum(true, false, false)));
        }
    }

}

class IFT implements IndexableFieldType {
    private boolean stored;
    private boolean tokenized;
    private IndexOptions io = IndexOptions.NONE;

    public void setStored() {
        stored = true;
    }

    public void setTokenized() {
        tokenized = true;
    }

    public void setIndexDoc() {
        io = IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS;
    }

    @Override
    public boolean stored() {
        return stored;
    }

    @Override
    public boolean tokenized() {
        return false;
    }

    @Override
    public boolean storeTermVectors() {
        return false;
    }

    @Override
    public boolean storeTermVectorOffsets() {
        return false;
    }

    @Override
    public boolean storeTermVectorPositions() {
        return false;
    }

    @Override
    public boolean storeTermVectorPayloads() {
        return false;
    }

    @Override
    public boolean omitNorms() {
        return false;
    }

    @Override
    public IndexOptions indexOptions() {
        return io;
    }

    @Override
    public DocValuesType docValuesType() {
        return null;
    }

    @Override
    public int pointDimensionCount() {
        return 0;
    }


    public int pointDataDimensionCount() {
        return 0;
    }

    @Override
    public int pointIndexDimensionCount() {
        return 0;
    }


    @Override
    public int pointNumBytes() {
        return 0;
    }

    @Override
    public int vectorDimension() {
        return 0;
    }

    @Override
    public VectorEncoding vectorEncoding() {
        return null;
    }

    @Override
    public VectorSimilarityFunction vectorSimilarityFunction() {
        return null;
    }

    @Override
    public Map<String, String> getAttributes() {
        return null;
    }

}