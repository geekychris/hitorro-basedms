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


import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.core.iterator.mappers.SettableMapper;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.typesystem.Bag;


public class BF2TwitterBagSetting extends SettableMapper<BaseFile, Bag, Bag> {
    private String fileField;
    private String fetchDateField;
    private String fetchDate;
    private String fv;

    public BF2TwitterBagSetting(String fileField, String fetchDateField) {
        this.fileField = fileField;
        this.fetchDateField = fetchDateField;
    }

    public void setElem(BaseFile e) {
        this.setElem = e;
        if (e == null) {
            fv = null;
        } else {
            fv = e.getAbsolutePath();
            String parts[] = StringUtil.tokenizeFromSingleChar(e.getName(), "-");
            if (parts != null && parts.length > 0) {
                long l = StringUtil.getLongNumberFromText(parts[0]);
                fetchDate = Long.toString(l);
            } else {
                fetchDate = "0";
            }
        }
    }

    @Override
    public Bag apply(final Bag bag) {
        if (bag == null) {
            return bag;
        }
        if (fv != null) {
            bag.setFromString(bag, fileField, fv, true);
            bag.setFromString(bag, fetchDateField, fetchDate, true);
        }
        return bag;
    }
}
