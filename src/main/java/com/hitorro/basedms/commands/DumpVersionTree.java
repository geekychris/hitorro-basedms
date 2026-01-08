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
package com.hitorro.basedms.commands;

import com.hitorro.base.objects.VersionableObject;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.util.commandandcontrol.ano.CommandArgument;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.commandandcontrol.ano.RespColumn;
import com.hitorro.util.commandandcontrol.ano.ResponseDefinition;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.json.keys.StringProperty;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTSerializable;

/**
 */
@CommandDef(command = "dms.dumpversiontree", description = "Dump the version tree of a versioned object.")
public class DumpVersionTree extends com.hitorro.util.commandandcontrol.Command {
    @CommandArgument(required = true)
    private StringProperty Guid = new StringProperty("guid", "Guid of an object in the version tree, does not have to be the root version", null);

    @ResponseDefinition(command = "dumpversiontree",
            rowname = "document",
            columns = {@RespColumn(name = "Meaning", lName = "meaning"),
                    @RespColumn(name = "Guid", lName = "guid"),
                    @RespColumn(name = "VersionLabel", lName = "versionlabel"),
                    @RespColumn(name = "Content Shares", lName = "contshares")})
    private com.hitorro.util.commandandcontrol.ResponseShape shape = new com.hitorro.util.commandandcontrol.ResponseShape();

    public boolean execute(String rawValue, JVS args, com.hitorro.util.commandandcontrol.Response response, com.hitorro.util.commandandcontrol.CommandSession session, com.hitorro.util.commandandcontrol.RestOperations operation) throws Exception {
        BaseSession dmsSession = DMSSessionFactory.getFactory().getSession();
        try {
            String guid = Guid.apply(args);
            if (StringUtil.nullOrEmptyOrBlankString(guid)) {
                this.writeSimpleError(response, "no guid provided");
                return false;
            }
            HTSerializable pt = dmsSession.getHTSerializableFromGUID(guid);
            if (pt == null) {
                this.writeSimpleError(response, "Guid %s does not exist", guid);
                return false;
            }
            if (!(pt instanceof VersionableObject)) {
                this.writeSimpleError(response, "Guid %s is not versionable", guid);
                return false;
            }
            VersionableObject o = (VersionableObject) pt;
            String rootGuid = o.getCanonicalGuid();
            VersionableObject root = (VersionableObject) dmsSession.getHTSerializableFromGUID(rootGuid);
            VersionableObject curr = root;
            response.setResponseShape(shape);
            com.hitorro.util.commandandcontrol.MultiRowResponse mrr = response.getMultiRowResponse();
            StringBuilder meaning = new StringBuilder();
            StringBuilder shares = new StringBuilder();
            while (curr != null) {
                meaning.setLength(0);
                shares.setLength(0);
                if (curr.getGuid().equals(rootGuid)) {
                    meaning.append("root");
                }
                if (curr.getGuid().equals(guid)) {
                    if (meaning.length() > 0) {
                        meaning.append(", ");
                    }
                    meaning.append("selected");
                }
                mrr.addTuple(0, meaning.toString(), curr.getGuid(), curr.getVersionLabel());

                //Hibernate.initialize(curr);
                //getDocumentShares(curr, mrr, 3, shares);
                response.addMultiRowResponse(mrr);
                if (curr.getNextVersion() == curr) {
                    response.addInfo(com.hitorro.util.commandandcontrol.InfoLevel.Error, Fmt.S("Guid %s nextVersion points to itself", curr.getGuid()));
                    curr = null;
                } else {
                    curr = curr.getNextVersion();
                }
            }
            response.end();
        } finally {
            DMSSessionFactory.closeSession(dmsSession);
        }
        return true;
    }

    /*private void getDocumentShares (VersionableObject curr, MultiRowResponse mrr, int column, StringBuilder shares)
    {
        Set<Content> conts = curr.getContents();
        for (Content c: conts)
        {
            shares.setPlayLength(0);
            shares.append(c.getOriginalFileName());
            shares.append("(");
            Set<VersionableObject> docs = c.getVersionableObjects();
            boolean first = true;
            for (VersionableObject doc : docs)
            {
                if(first)
                {
                    first = false;
                }
                else
                {
                    shares.append(",");
                }
                String docGuid = doc.getGuid();
                if (curr.getGuid().equals(docGuid))
                {
                    docGuid = "me";
                }
                shares.append(docGuid);
            }
            shares.append(")");
            mrr.add(column, shares.toString());

        }
    }    */
}
