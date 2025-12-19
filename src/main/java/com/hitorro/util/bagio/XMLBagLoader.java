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

import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.typesystem.Bag;
import com.hitorro.util.xml.SAXUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Read bags from an xml file.  Of the form:
 * <p/>
 * <bags> <bagname> <field1></field1>
 * <p/>
 * <field2></field2> </bagname>
 * <p/>
 * </bags>
 */
public class XMLBagLoader {
    public List<Bag> getBags(File f, boolean includeUnknown)
            throws IOException, ParserConfigurationException, SAXException {
        BagContainer container = new BagContainer();
        getBags(container, f, includeUnknown);
        return container.bags;
    }

    public void getBags(final BagContainer container, final File f, boolean includeUnknown)
            throws IOException, ParserConfigurationException, SAXException {
        ReadBagHandler handler = new ReadBagHandler(container, includeUnknown);
        SAXUtil.readSax(f, handler);
    }

    public List<Bag> getBags(InputStream is, boolean includeUnknown)
            throws IOException, ParserConfigurationException, SAXException {
        BagContainer container = new BagContainer();
        getBags(container, is, includeUnknown);
        return container.bags;
    }

    public void getBags(final BagContainer container, final InputStream is, boolean includeUnknown)
            throws IOException, SAXException, ParserConfigurationException {
        ReadBagHandler handler = new ReadBagHandler(container, includeUnknown);
        SAXUtil.readSax(is, handler);
    }
}

class BagContainer implements BagCallback {
    public List<Bag> bags = new ArrayList();

    public void addBag(Bag bag) {
        bags.add(bag);
    }
}

class ReadBagHandler extends DefaultHandler {
    public static final String ConfigRoot = "bags";
    private boolean readPastStart = false;
    private boolean inBag = false;
    private boolean inField = false;
    private String bagName = null;
    private StringBuilder builder = new StringBuilder();
    private Bag bag = null;
    private BagContainer container;
    private boolean includeUnknown;

    public ReadBagHandler(BagContainer container, boolean includeUnknown) {
        this.container = container;
        this.includeUnknown = includeUnknown;
    }

    /**
     * When you see a start tag, print it out and then increase indentation by two spaces. If the element has
     * attributes, place them in parens after the element name.
     */
    public void startElement(String namespaceUri,
                             String localName,
                             String qualifiedName,
                             Attributes attributes) {
        if (!readPastStart) {
            qualifiedName = qualifiedName.toLowerCase();
            if (qualifiedName.equals(ConfigRoot)) {

                readPastStart = true;
                // we ignore the top level
                return;
            }
        }
        if (!inBag) {
            bag = Bag.getBagForType(qualifiedName);
            bagName = qualifiedName;
            inBag = true;
            for (int i = 0; i < attributes.getLength(); i++) {
                String n = attributes.getQName(i);
                String v = attributes.getValue(i);
                setField(n, v);
            }
            return;
        }
        inField = true;
        builder.setLength(0);

    }

    private void setField(final String n, final String v) {
        bag.setFromString(bag, n, v, includeUnknown);
    }

    /**
     * When you see the end tag, print it out and decrease indentation level by 2.
     */

    public void endElement(String namespaceUri,
                           String localName,
                           String qualifiedName) {

        if (!inBag) {
            return;
        }
        qualifiedName = qualifiedName.toLowerCase();
        if (qualifiedName.equals(ConfigRoot)) {
            // we ignore the top level
            return;
        }

        if (inField) {
            if (builder.length() != 0) {
                String s = builder.toString().trim();

                if (!StringUtil.nullOrEmptyString(s)) {
                    setField(qualifiedName, s);
                }
                builder.setLength(0);
            }
            inField = false;
            return;
        }
        inBag = false;
        container.addBag(bag);
        bag = null;
    }


    public void characters(char[] chars,
                           int startIndex,
                           int endIndex) {
        builder.append(chars, startIndex, endIndex);
    }
}


