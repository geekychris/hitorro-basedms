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
package com.hitorro.jsontypesystem.dynamic.mappers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.hitorro.language.Iso639Table;
import com.hitorro.language.IsoLanguage;
import com.hitorro.util.core.iterator.mappers.JsonInitableMapper;

public class Json2HTMLScrubbedJson extends JsonInitableMapper<JsonNode, JsonNode> {
    public static Json2HTMLScrubbedJson stripper = new Json2HTMLScrubbedJson();
    private ThreadLocal<com.hitorro.jsontypesystem.dynamic.mappers.HTMLParserMapper> parserTLS = new ThreadLocal<com.hitorro.jsontypesystem.dynamic.mappers.HTMLParserMapper>();

    public JsonNode apply(final JsonNode js) {
        return apply(js, Iso639Table.english);
    }

    public JsonNode apply(final JsonNode js, IsoLanguage lang) {

        if (js == null || js.isNull()) {
            return js;
        }
        String t = js.asText();
        if (t == null) {
            return null;
        }
        return JsonNodeFactory.instance.textNode(getParser().apply(t));
    }

    private com.hitorro.jsontypesystem.dynamic.mappers.HTMLParserMapper getParser() {
        com.hitorro.jsontypesystem.dynamic.mappers.HTMLParserMapper p = parserTLS.get();
        if (p == null) {
            p = new com.hitorro.jsontypesystem.dynamic.mappers.HTMLParserMapper(' ');
            parserTLS.set(p);
        }
        return p;
    }

}
