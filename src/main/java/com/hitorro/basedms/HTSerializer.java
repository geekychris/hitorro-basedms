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
package com.hitorro.basedms;

import com.hitorro.basedms.session.DMSSession;
import com.hitorro.util.core.Log;
import com.hitorro.util.io.StoreException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * <p/>
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Oct 16, 2005 Time: 1:43:08 PM
 */
public class HTSerializer {
    private static final long serialVersionUID = 4765780215928860139L;
    private transient DMSSession session = null;

    public HTSerializer(DMSSession sess) {
        session = sess;
    }

    public HTSerializer() {
    }

    /**
     * Serialize the content of an object into a byte array.
     *
     * @param obj Object to serialize
     * @return a byte array representing the object's state
     */
    public byte[] serialize(Object obj)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        com.hitorro.util.typesystem.HTObjectOutputStream os = new com.hitorro.util.typesystem.HTObjectOutputStreamImpl(session, baos, com.hitorro.util.typesystem.TypeManager.getTypeManager(), false);
        try {
            os.writeVersionedObject((com.hitorro.util.typesystem.HTSerializable) obj);
        } catch (StoreException e) {
            throw new IOException(e.getMessage());
        }
        os.flush();
        baos.close();
        return baos.toByteArray();
    }


    /**
     * Deserialize the content of an object from a byte array.
     *
     * @param serialized Byte array representation of the object
     * @return deserialized object
     */
    public Object deserialize(byte[] serialized)
            throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(serialized);


        com.hitorro.util.typesystem.HTObjectInputStream is = new com.hitorro.util.typesystem.HTObjectInputStreamImpl(bais,
                com.hitorro.util.typesystem.TypeManager.getTypeManager(),
                session);
        try {
            return is.readVersionedObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        } catch (StoreException e) {
            Log.util.error("Exception %s %e", e, e);
        }
        return null;
    }
}
