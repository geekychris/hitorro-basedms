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

import com.hitorro.base.objects.Store;
import com.hitorro.base.objects.StoreType;
import com.hitorro.basedms.StoreUtil;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.basefile.tools.queue.reader.DirectoryVisitorIterator;
import com.hitorro.util.basefile.tools.queue.reader.GoodFileKeyWriter;
import com.hitorro.util.commandandcontrol.Command;
import com.hitorro.util.commandandcontrol.CommandSession;
import com.hitorro.util.commandandcontrol.Response;
import com.hitorro.util.commandandcontrol.RestOperations;
import com.hitorro.util.commandandcontrol.ano.CommandArgument;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.core.Console;
import com.hitorro.util.core.HTAssert;
import com.hitorro.util.core.opers.AlwaysTrueOperator;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.json.keys.StringProperty;
import com.hitorro.util.typesystem.BaseSession;

import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;

/**
 * Scan a file system, identify files that do not have a content object, optionally remove them or generate a script to
 * remove them
 */
@CommandDef(command = "dms.removeorphanedfiles",
        description = "Scan a file system, identify files that do not have a content object, optionally remove them or generate a script to remove them")
public class RemoveOrphanedFiles extends Command {
    private static final String ContentQuery = "select itm.guid from Content as itm where storeName= :a and fileName= :b";
    @CommandArgument(required = true)
    private StringProperty Filestore = new StringProperty("filestore", "filestore name", "");
    @CommandArgument(required = true)
    private StringProperty FileName = new StringProperty("outputfile", "output file name", "");

    public boolean execute(String rawValue, JsonNode args, Response response, CommandSession session, RestOperations operation) throws Exception {
        HTAssert.assertThat(false, "command does not work at the moment you need to work through the fact that enumeration is over base file");
        BaseSession dmsSession = DMSSessionFactory.getFactory().getSession();
        try {
            String file = FileName.apply(args);
            String store = Filestore.apply(args);
            File outFile = new File(file);
            if (outFile.exists()) {
                if (outFile.isFile()) {
                    outFile.delete();
                } else {
                    this.writeSimpleError(response, "File provided is a directory", file);
                    return true;
                }
            }
            PrintWriter pw = FileUtil.getBufferedPrintWriterFromFile(outFile);
            Console.println(pw, "#Orphaned files script");

            Store st = StoreUtil.getStore(store);
            if (st == null) {
                this.writeSimpleError(response, "Filestore %s does not exist", store);
                return true;
            }
            if (st.getStoreTypeType() == StoreType.Blob) {
                this.writeSimpleError(response, "Filestore %s is a blobstore", store);
                return true;
            }
            if (st.getStoreTypeType() == StoreType.Unmanaged) {
                this.writeSimpleError(response, "Filestore %s is an unmanaged", store);
                return true;
            }
            BaseFile root = st.getRootPathPath();
            String rootPath = root.getAbsolutePath();
            String storeGuid = st.getSoftGuid();
            File lgkFile = FileUtil.getUniqueTmpFile("lgk");
            GoodFileKeyWriter lgk = new GoodFileKeyWriter(lgkFile);

            DirectoryVisitorIterator iter = new DirectoryVisitorIterator(root, -1, lgk, AlwaysTrueOperator.oper);
            int rootLength = root.getRelativePath().length();
            int found = 0;
            int toRemove = 0;
            while (iter.hasNext()) {
                BaseFile f = iter.next();

                String rel = f.getRelativePath();
                String relTrunk = rel.substring(rootLength + 1);
                Iterator itr = dmsSession.getIteratorFromQueryArgs(ContentQuery,
                        storeGuid, StringUtil.strcat("/", relTrunk));
                if (itr.hasNext()) {
                    iter.next();
                    Console.println(pw, "#exists: %s", f);
                    found++;
                } else {
                    Console.println(pw, "rm %s/%s", rootPath, f);
                    toRemove++;
                }

                dmsSession.rollback();
            }
            this.writeSuccess(response, "found %s %s", found, toRemove);
            pw.flush();
            pw.close();
        } finally {
            DMSSessionFactory.getFactory().rollbackClose(dmsSession);
        }
        return true;
    }
}
