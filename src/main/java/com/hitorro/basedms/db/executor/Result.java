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
 * <p/>
 * Result of executing a SQL Statement
 * <p/>
 * Results can include: => Single Update Count (e.g. result of INSERT/UPDATE/DELETE/CREATE/DROP) => Single List of
 * String results (e.g. result of a single SELECT) => Multiple Lists of String results (e.g. result of a Stored
 * Procedure that returns multiple ResultSets)
 */
public class Result {

    public static final String COLUMN_ROWS_AFFECTED = "Rows Affected";

    private String _statement = null;
    private List<ResultTable> _results = null;

    public Result() {
    }

    public Result(String statement) {
        _statement = statement;
    }

    /**
     * Add a ResultTable to this listFiles of results (instantiate listFiles on first add)
     *
     * @param table ResultTable containing listFiles of string results
     */
    public void addResultTable(ResultTable table) {
        if (_results == null) {
            _results = new ArrayList<ResultTable>();
        }
        _results.add(table);
    }

    /**
     * @return String SQL Statement executed that generated these results
     */
    public String getStatement() {
        return _statement;
    }

    public void setStatement(String statement) {
        _statement = statement;
    }

    public List<ResultTable> getResults() {
        return _results;
    }

    public void setResults(List<ResultTable> results) {
        _results = results;
    }

}
