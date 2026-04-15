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
import com.hitorro.language.Iso639Table;
import com.hitorro.language.LanguageId;
import com.hitorro.util.json.keys.BooleanProperty;
import com.hitorro.util.json.keys.StringProperty;
import com.hitorro.util.typesystem.valuesource.ValueMapMapper;
import com.hitorro.util.typesystem.valuesource.ValueSourceForClass;

/**
 * Value me that given text will classify the language
 */
public class LanguageIdMapper extends ValueMapMapper {
    public static StringProperty SrcLangKey = new StringProperty("srclang", "field that contains an iso639 code from the data provider", null);
    public static BooleanProperty SrcLangTakesPriority = new BooleanProperty("srclangpriority", "language from provider takes priority", false);
    protected ThreadLocal<Iso639Table> threadData = new ThreadLocal();
    private boolean srcLangTakesPriority;
    private String srcLang;

    public boolean compute(String requestedField, ValueSourceForClass e) {
        String srcLangString = null;
        if (srcLang != null) {
            Object s = e.getValue(e, srcLang);
            if (s != null) {
                srcLangString = s.toString();
                // Make sure it looks like our normalized iso codes
                srcLangString = Iso639Table.getInstance().getNorm(srcLangString);
                if (srcLangTakesPriority && srcLangString != null) {
                    // we ignore whatever else we are using the original
                    e.setValue(e, requestedField, srcLangString);
                    return true;
                }
            }
        }
        Object c = e.getValue(e, fromField);
        String assignMe = srcLangString;
        if (c != null) {
            // now lets attempt classification
            String classLang = LanguageId.getInstance().getLanguage639(c.toString());
            if (classLang != null) {
                // don't have content and we have src lang so use that...kinda weird with no content though
                assignMe = classLang;
            }
        }
        if (assignMe != null) {
            e.setValue(e, requestedField, assignMe);
        }
        return true;
    }

    public boolean init(JsonNode map) {
        super.init(map);
        srcLang = SrcLangKey.apply(map);
        srcLangTakesPriority = SrcLangTakesPriority.apply(map);
        return true;
    }

    private Iso639Table getTable() {
        Iso639Table tab = threadData.get();
        if (tab == null) {
            tab = Iso639Table.getInstance();
            threadData.set(tab);
        }
        return tab;
    }

}