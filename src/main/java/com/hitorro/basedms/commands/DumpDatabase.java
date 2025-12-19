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

import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.basedms.db.DatabaseUtil;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.util.commandandcontrol.Command;
import com.hitorro.util.commandandcontrol.CommandSession;
import com.hitorro.util.commandandcontrol.Response;
import com.hitorro.util.commandandcontrol.RestOperations;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.core.Env;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.osprocessexec.ExecResultRowMapper;

import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;

/**
 * <p/>
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Aug 13, 2005 Time: 9:18:19 PM
 */
@CommandDef(command = "db.dump", description = "Dump the database contents as a sql file")
public class DumpDatabase extends Command {
    public boolean execute(String rawValue, JVS args, Response response, CommandSession session, RestOperations operation) throws Exception {
        File sqlFile = new File(Env.getHome(), "/sqldump/dbdump.sql");
        if (!FileUtil.ensureParentDirectories(sqlFile, true)) {
            this.writeSimpleError(response, "Unable to get write access to %s", sqlFile);
            return false;
        }
        PrintWriter pw = FileUtil.getBufferedPrintWriterFromFile(sqlFile);
        JVS defaultDbProperties = DatabaseUtil.getDefaultDBContext();
        JsonNode defaultInfo = DatabaseUtil.getConnectionInfoMap(defaultDbProperties);

        /*   build out database connection variables from connection properties apply.  */
        String username = DatabaseUtil.UsernameKey.apply(defaultInfo);
        String password = DatabaseUtil.PasswordKey.apply(defaultInfo);
        String databaseUrl = DatabaseUtil.DatabaseUrlKey.apply(defaultInfo);
        String hostName = DatabaseUtil.HostNameKey.apply(defaultInfo);
        String databaseName = DatabaseUtil.DatabaseNameKey.apply(defaultInfo);
        ExecResultRowMapper mapper = new ExecResultRowMapper("mysqldump", new String[]{Fmt.S("-p%s", password), Fmt.S("-u%s", username), databaseName});
        try {
            Iterator<String> iter = mapper.map(10000);
            int i = 0;
            while (iter.hasNext()) {
                pw.write(iter.next());
            }
            pw.flush();
            pw.close();
        } catch (InterruptedException e) {
            Log.util.error("Exception %s %e", e, e);
        }
        return false;
    }
}
