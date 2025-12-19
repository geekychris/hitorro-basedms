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
package com.hitorro.basedms.db.executor;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: Chris Date: Nov 16, 2006 Time: 12:52:09 PM
 * <p/>
 * Table of results consisting of a List of Strings
 * <p/>
 * Note: Columnar data is represented by a single tab-delimited String
 * <p/>
 * TODO: Support Cells (indexed by Row & Column)
 */
public class ResultTable {

    private List<String> _rows = null;
    private List<String> _columns = null;


    public List<String> getColumns() {
        return _columns;
    }

    public void setColumns(List<String> columns) {
        _columns = columns;
    }

    public void addColumn(String column) {
        if (_columns == null) {
            _columns = new ArrayList<String>();
        }
        _columns.add(column);
    }

    public List<String> getRows() {
        return _rows;
    }

    public void setRows(List<String> rows) {
        _rows = rows;
    }

    public void addRow(String row) {
        if (_rows == null) {
            _rows = new ArrayList<String>();
        }
        _rows.add(row);
    }

}
