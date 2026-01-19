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
package com.hitorro.basedms.transformer;

import com.hitorro.util.core.map.MapUtil;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.io.csv.CSVReader;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**

 * Load ConvertionContexts from a csv file.
 */
public class ConvertionContextLoader {
    public static final String MimeFrom = "mimefrom";

    public static final String MimeTo = "mimeto";

    public static final String Transformer = "transformer";

    public static final String Method = "method";

    public static final String MethodArgs = "methodargs";
    private ConvertionContext context;
    private String header[];
    private Map<String, Integer> map;

    ConvertionContextLoader(ConvertionContext context) {
        this.context = context;
    }

    public void load(File f) throws IOException {
        CSVReader reader = new CSVReader(FileUtil.getBufferedFileInputStream(f));
        header = reader.getColumnNames();
        map = MapUtil.getMapColumnNameToIndexPosition(header, true);
        String[] row = reader.getNextRow();
        while (row != null) {
            line(row);
            row = reader.getNextRow();
        }
    }

    public void line(String[] line) {
        String from = MapUtil.getColumnFromColumMap(MimeFrom, map, line, true);
        String to = MapUtil.getColumnFromColumMap(MimeTo, map, line, true);
        String transformer = MapUtil.getColumnFromColumMap(Transformer, map, line, true);
        String method = MapUtil.getColumnFromColumMap(Method, map, line, true);
        String methodArgs = MapUtil.getColumnFromColumMap(MethodArgs, map, line, true);
        ConvertionEdge edge = new ConvertionEdge();
        edge.setSourceMimeType(from);
        edge.setTargetMimeType(to);
        edge.setTransformerName(transformer);
        edge.setTransformerMethod(method);
        edge.setMethodArgs(methodArgs);
        this.context.add(edge);
    }
}
