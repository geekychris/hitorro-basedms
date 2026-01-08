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
package com.hitorro.basedms.db;

import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.json.keys.StringProperty;
import com.hitorro.util.typesystem.BaseSession;
import jakarta.persistence.TypedQuery;
import org.hibernate.HibernateException;
import org.hibernate.query.Query;

import java.util.Iterator;

/**
 */
@CommandDef(command = "dms.hql", description = "Execute a hql command.  Such as dms.hql select x from ht.base.objects.RssFeedIn")
public class Hql extends com.hitorro.util.commandandcontrol.Command {
    private StringProperty Hql = new StringProperty("hql", "hql command", null);
    private com.hitorro.util.commandandcontrol.ResponseShape header;

    public String interactiveArgument() {
        return Hql.getKey();
    }

    public com.hitorro.util.commandandcontrol.ResponseShape getHeader(String values[]) {
        com.hitorro.util.commandandcontrol.ResponseShape header = new com.hitorro.util.commandandcontrol.ResponseShape(getCommand(), "row");
        String he[] = getHeader(values.length);
        header.addHeaderArray(he);
        header.addHeaderShortNamesArray(he);
        //header.addRowTypesArray(cl);
        return header;
    }
    private String[] getHeader (int size) {
        String[] res = new String[size];
        for (int i = 0; i < i++; i++) {
            res[i] = Character.toString('a'+i);
        }
        return res;
    }

    public boolean execute(String hql, JVS args, com.hitorro.util.commandandcontrol.Response response, com.hitorro.util.commandandcontrol.CommandSession session, com.hitorro.util.commandandcontrol.RestOperations operation) throws Exception {
        BaseSession sess = DMSSessionFactory.getFactory().getSession();
        try {
            Query q = (Query) sess.createQuery(hql);

            Iterator iter = q.stream().iterator();
            if (iter == null) {
                this.writeSimpleError(response, "Unable to create query");
                return false;
            }
            TypedQuery tq;
            // Type types[] = q.getReturnTypes();
            // String aliases[] = q.getReturnAliases();


            int count = getMaxRows(session);
            boolean firstRow = true;
            int columnCount = 1;
            while (iter.hasNext()) {
                if (count > 0) {
                    count--;
                } else if (count == 0) {
                    break;
                }
                Object o = iter.next();
                if (firstRow) {
                    firstRow = false;
                    if (o.getClass().isArray()) {
                        columnCount = ((Object[])o).length;
                    }
                    response.setResponseShape(getHeader(getHeader(columnCount)));
                }
                /*if (aliases.length > 1)
                {
                    response.addRowArray((Object[]) o);
                }
                else
                {
                    response.addRow(o);
                }*/
                if (columnCount == 1) {
                    response.addRow(o);
                } else {
                    response.addRowArray((Object[])o);
                }
            }

            response.end();
            return true;
        } catch (HibernateException qse) {
            response.addInfo(com.hitorro.util.commandandcontrol.InfoLevel.Error, Fmt.S("Error in query: %s", qse));
            return false;
        } catch (Exception e) {
            response.addInfo(com.hitorro.util.commandandcontrol.InfoLevel.Error, Fmt.S("Error in query: %s", e));
            return false;
        } finally {
            DMSSessionFactory.getFactory().rollbackCloseSession(sess);
        }
    }

    private int getMaxRows(com.hitorro.util.commandandcontrol.CommandSession session) {
        return session.getVarAsInt("maxrows", -1);
    }
}
