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
package com.hitorro.basedms.rss;

import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * from com.sun.syndication.fetcher.impl.ResponseHandler
 */

public class ResponseHandler {

    public static final String defaultCharacterEncoding = "UTF-8";

    private static Pattern characterEncodingPattern = Pattern.compile("charset=([.[^; ]]*)");

    public static String getCharacterEncoding(URLConnection connection) {
        return getCharacterEncoding(connection.getContentType());
    }

    /**
     * <p>Gets the character encoding of a response. (Note that this is different to the content-encoding)</p>
     *
     * @param contentTypeHeader the value of the content-type HTTP header eg: text/html; charset=ISO-8859-4
     * @return the character encoding, eg: ISO-8859-4
     */
    public static String getCharacterEncoding(String contentTypeHeader) {
        if (contentTypeHeader == null) {
            return defaultCharacterEncoding;
        }

        Matcher m = characterEncodingPattern.matcher(contentTypeHeader);
        if (!m.find()) {
            return defaultCharacterEncoding;
        } else {
            return m.group(1);
        }
    }

}
