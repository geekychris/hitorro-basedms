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
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.core.iterator.sinks.Sink;
import com.hitorro.util.json.keys.BasefileProperty;
import com.hitorro.util.typesystem.Bag;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.io.IOException;
import java.io.Writer;


public class XML2BagWriter implements Sink<Bag> {
    public static BasefileProperty BaseFileKey = new BasefileProperty("outfile", "output file using basefile syntax");

    public static final String Bags = "bags";
    private XMLStreamWriter xwriter;
    private Writer writer;

    public XML2BagWriter(Writer writer) {
        this.writer = writer;
        xwriter = newXmlWriter(writer);

    }

    @Override
    public boolean init(JsonNode node) {
        BaseFile bf = BaseFileKey.apply(node);
        bf.mkParentDir();
        try {
            writer = bf.getPrintWriter();
        } catch (IOException e) {
            return false;
        }
        xwriter = newXmlWriter(writer);
        return false;
    }

    @Override
    public boolean start() {
        try {
            xwriter.writeStartDocument();
            xwriter.writeStartElement(Bags);
        } catch (XMLStreamException e) {
            //XXX LOG
            return false;
        }
        return true;
    }

    @Override
    public boolean add(final Bag b) {
        String type = b.getType().getName();
        try {
            xwriter.writeStartElement(type);
            String fields[] = b.getFieldNames();
            for (String field : fields) {
                Object o = b.getValue(b, field);
                if (o != null) {
                    xwriter.writeStartElement(field);
                    xwriter.writeCharacters(o.toString());
                    xwriter.writeEndElement();
                }
            }

            xwriter.writeEndElement();
        } catch (XMLStreamException e) {
            //XXX LOG
            return false;
        }
        return false;
    }

    @Override
    public boolean stop() {
        try {
            xwriter.writeEndElement();
            xwriter.writeEndDocument();
            xwriter.flush();
            writer.flush();
            writer.close();
        } catch (XMLStreamException e) {
            //XXX logging
            return false;
        } catch (IOException e) {
            // XXX logging
            return false;
        }
        return true;
    }

    @Override
    public void close() throws IOException {
        stop();
    }

    private static XMLStreamWriter newXmlWriter(Writer writer) {
        try {
            return XMLOutputFactory.newInstance().createXMLStreamWriter(writer);
        } catch (XMLStreamException e) {
            //XXX LOG
            return null;
        }
    }
}
