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
package com.hitorro.base.typesystem;

import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.util.commandandcontrol.ano.CommandArgument;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.json.keys.BooleanProperty;
import com.hitorro.util.json.keys.StringProperty;

import java.io.File;
import java.io.IOException;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Oct 24, 2006 Time: 10:52:29 PM
 */
@CommandDef(command = "dms.loadserialization", description = "Load objects into the system from a serialized file.")
public class Load extends com.hitorro.util.commandandcontrol.Command {
    @CommandArgument(required = true)
    private StringProperty FileName = new StringProperty("file", "file to read serialization from", "");
    @CommandArgument(required = true)
    private BooleanProperty LoadContent = new BooleanProperty("includecontent", "load the content in from disk", true);

    public boolean execute(String rawValue, JVS args, com.hitorro.util.commandandcontrol.Response response, com.hitorro.util.commandandcontrol.CommandSession session, com.hitorro.util.commandandcontrol.RestOperations operation) throws Exception {
        boolean results = true;
        String fileName = FileName.apply(args);
        boolean includeContent = LoadContent.apply(args);
        File f = new File(fileName);
        com.hitorro.util.typesystem.BaseSession sess = DMSSessionFactory.getFactory().getSession();
        try {
            com.hitorro.util.typesystem.HTObjectInputStream is = new com.hitorro.util.typesystem.HTObjectInputStreamImpl(FileUtil.getBufferedFileInputStream(f),
                    com.hitorro.util.typesystem.TypeManager.getTypeManager(),
                    sess);
            try {
                com.hitorro.util.typesystem.HTSerializable bt = is.readVersionedObject();
                int overallCount = 0;
                int batchSize = 100;
                while (bt != null) {
                    //is.commit();
                    overallCount++;
                    if (is.getNonCommittedObjectCount() >= batchSize) {
                        is.commit();
                    }

                    bt = is.readVersionedObject();
                    if (overallCount % 100 == 0) {
                        com.hitorro.util.typesystem.Log.typemanager.info("wrote %s docs", overallCount);
                    }
                }
                if (is.getNonCommittedObjectCount() > 0) {
                    is.commit();
                }
                DMSSessionFactory.getFactory().rollbackCloseSession(sess);

            } catch (ClassNotFoundException e) {
                response.addInfo(com.hitorro.util.commandandcontrol.InfoLevel.Error, Fmt.S("Unable to read object from file %s", fileName));
                results = false;
            } catch (StoreException e) {
                response.addInfo(com.hitorro.util.commandandcontrol.InfoLevel.Error, Fmt.S("Unable to read object from file %s", fileName));
                results = false;
            }
        } catch (IOException e) {
            this.writeSimpleError(response, "Unable to read file %s %s %e",
                    fileName, e, e);
            results = false;
        } finally {
            DMSSessionFactory.closeSession(sess);
        }
        return results;
    }
}
