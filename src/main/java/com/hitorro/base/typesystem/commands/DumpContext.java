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
package com.hitorro.base.typesystem.commands;

import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.BaseType;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.HTObjectStreamFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Nov 7, 2006 Time: 9:24:18 AM Context is
 * responsible for dumping a set of objects from a set of queries
 * <p/>
 * A dump file consists of numerous persisted object and a lead out record.
 * <p/>
 * The lead out record includes:
 * <p/>
 * max values for named longs.
 * <p/>
 * NOTE: that if your dumping and loading between systems of the same instance id, you MUST dump the NamedLongEntry
 * values
 */
public class DumpContext {
    private List<String> m_queries = new ArrayList<String>();
    private BaseSession m_session;
    private File m_outputFile;
    private boolean m_includeContent;

    public DumpContext(File outputFile, boolean includeContent) {
        m_outputFile = outputFile;
        m_includeContent = includeContent;
    }

    /**
     * Adds all the queries required for upgradeAllInstances.  This is:
     * <p/>
     * NamedLongEntry's VersionableObjects of all types
     *
     * @param outputFile
     * @return
     */
    public static DumpContext getDumpContextForUpgrade(File outputFile) {
        DumpContext dc = new DumpContext(outputFile, true);
        dc.addClass(com.hitorro.base.objects.NamedLongEntry.class, false);
        dc.addClass(com.hitorro.base.objects.Category.class, false);
        dc.addClass(com.hitorro.base.objects.DomainInfo.class, false);
        dc.addClass(com.hitorro.base.objects.Document.class, true);
        dc.addClass(com.hitorro.base.objects.Container.class, true);
        dc.addClass(com.hitorro.base.objects.Store.class, false);
        dc.addClass(com.hitorro.base.objects.User.class, true);
        return dc;
    }

    public void addClass(Class c, boolean hasGuid) {
        if (hasGuid) {
            m_queries.add(Fmt.S("select guid from %s", c.getCanonicalName()));
        } else {
            m_queries.add(Fmt.S("from %s", c.getCanonicalName()));
        }
    }

    public void addQuery(String q) {
        m_queries.add(q);
    }

    public List<String> getQueries() {
        return m_queries;
    }

    public int dump() throws IOException {
        m_session = DMSSessionFactory.getFactory().getSession();
        BaseSession querySession = DMSSessionFactory.getFactory().getSession();
        try {
            m_outputFile.delete();
            OutputStream oos = FileUtil.getBufferedFileOutputStream(m_outputFile);

            HTObjectOutputStream os = HTObjectStreamFactory.getOutputStream(oos, m_session, m_includeContent);
            int counter = 0;
            BaseSession fetchSession = DMSSessionFactory.getFactory().getSession();
            // write out a null file offset for where the lead out record is.  We will fix this up at the end.

            for (String q : m_queries) {
                try {
                    Iterator<Object> iter = querySession.getIteratorFromQuery(q);


                    while (iter.hasNext()) {
                        try {
                            Object o = iter.next();
                            BaseType bt = null;
                            if (o instanceof BaseType) {
                                bt = (BaseType) o;
                            } else {
                                bt = (BaseType) fetchSession.getHTSerializableFromGUID(o.toString());
                            }
                            if (bt == null) {
                                Log.dmssession.error("Unable to retrieve %s", o);
                            }
                            os.writeVersionedObject(bt);
                            counter++;
                        } finally {
                            DMSSessionFactory.getFactory().rollbackCloseSession(fetchSession);
                            DMSSessionFactory.getFactory().rollbackCloseSession(m_session);
                        }
                    }
                } catch (Exception ioe) {
                    Log.dmssession.error("Error querying %s %e", ioe, ioe);
                } finally {
                    DMSSessionFactory.getFactory().rollbackCloseSession(querySession);
                }

            }


            os.writeEnd();
            oos.flush();
            oos.close();
            return counter;
        } finally {
            DMSSessionFactory.getFactory().rollbackCloseSession(m_session);
            DMSSessionFactory.getFactory().rollbackCloseSession(querySession);
        }
    }
}
