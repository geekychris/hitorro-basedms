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
package com.hitorro.basedms.transformer.squeeze;

import com.hitorro.util.core.string.StringUtil;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class SqueezeXMLOutputParser extends DefaultHandler {
    public static final String CompressionProgress = "CompressionProgress";
    public static final String Filename = "Filename";
    public static final String PercentComplete = "PercentComplete";

    private SqueezeService service;


    public SqueezeXMLOutputParser(SqueezeService service) {
        this.service = service;
    }


    public void startElement(String namespaceUri,
                             String localName,
                             String qualifiedName,
                             Attributes attributes) {
        if (CompressionProgress.equals(qualifiedName)) {
            String fname = null;
            String percentage = null;
            int length = attributes.getLength();
            for (int i = 0; i < length; i++) {
                String name = attributes.getQName(i);
                if (name.equals(Filename)) {
                    fname = attributes.getValue(i);
                } else if (name.equals(PercentComplete)) {
                    percentage = attributes.getValue(i);
                }
            }
            if (!StringUtil.nullOrEmptyOrBlankString(fname) && !StringUtil.nullOrEmptyOrBlankString(percentage)) {
                service.setProcessingFile(fname, percentage);
            }
        }
    }


    public void endElement(java.lang.String string, java.lang.String string1, java.lang.String string2) {

    }


    public void characters(char[] chars, int i, int i1) {

    }
}
