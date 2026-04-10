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

import com.hitorro.util.core.Console;
import com.hitorro.util.core.iterator.BaseStaxIterator;
import com.hitorro.util.typesystem.Bag;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;


public class XMLBagIterator extends BaseStaxIterator<Bag> {

    public XMLBagIterator(InputStream is, String encoding) throws XMLStreamException, UnsupportedEncodingException {
        super(is, encoding);
    }

    @Override
    public Bag readNext() {
        StringBuilder sb = new StringBuilder();
        String elementName = null;
        int depth = 0;
        Bag b = null;
        // get the type code
        try {
            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamReader.START_DOCUMENT:
                        break;
                    case XMLStreamReader.END_DOCUMENT:
                        break;
                    case XMLStreamReader.START_ELEMENT:
                        depth++;
                        sb.setLength(0);

                        elementName = reader.getLocalName();
                        if (depth == 1) {
                            b = Bag.getBagForType(elementName);
                            int attrCount = reader.getAttributeCount();
                            for (int i = 0; i < attrCount; i++) {
                                b.setValue(b, reader.getAttributeLocalName(i), reader.getAttributeValue(i));
                            }
                            if (b == null) {
                                //XXX LOG
                                return null;
                            }
                        }
                        break;
                    case XMLStreamReader.END_ELEMENT:
                        if (depth == 2) {
                            b.setValue(b, elementName, sb.toString());
                        }
                        if (depth == 1) {
                            return b;
                        }
                        depth--;
                        break;
                    case XMLStreamReader.ATTRIBUTE:
                        Console.println("Attribute");
                        break;
                    case XMLStreamReader.CHARACTERS:
                        String s = reader.getText();
                        sb.append(s);
                        break;
                    case XMLStreamReader.CDATA:
                        Console.println("CDATA");
                        break;

                }
            }

            // String type = reader.getElementText();
            //Bag b = Bag.getBagForType(type);

            //return null;
            return null;

        } catch (XMLStreamException e1) {
            // LOG SOMETHING???
            return null;
        }
    }
}