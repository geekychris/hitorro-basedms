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

import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.util.commandandcontrol.Command;
import com.hitorro.util.commandandcontrol.CommandSession;
import com.hitorro.util.commandandcontrol.Response;
import com.hitorro.util.commandandcontrol.RestOperations;
import com.hitorro.util.commandandcontrol.ano.CommandArgument;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.json.keys.BooleanProperty;
import com.hitorro.util.json.keys.StringProperty;

import java.io.File;
import java.io.IOException;

/**
 */
@CommandDef(command = "dms.dumphql", description = "Dump a series of objects to an output stream")
public class DumpCommand extends Command {
    @CommandArgument(required = true)
    private StringProperty Hql = new StringProperty("hql", "HQL to fetch objects", "");
    @CommandArgument(required = true)
    private StringProperty FileName = new StringProperty("file", "file to write out the serialized content as", "");
    @CommandArgument(required = true)
    private BooleanProperty IncludeContent = new BooleanProperty("includecontent", "include content files", true);

    public boolean execute(String rawValue, JsonNode args, Response response, CommandSession session, RestOperations operation) throws Exception {
        DumpContext dc = new DumpContext(new File(FileName.apply(args)), IncludeContent.apply(args));
        dc.addQuery(Hql.apply(args));
        int counter = 0;
        try {
            counter = dc.dump();
            this.writeSuccess(response, "wrote %s root level objects", Integer.toString(counter));
        } catch (IOException e) {
            this.writeSimpleError(response, "Unable to create dump file% %s %e", e, e);
            return false;
        }
        return true;
    }
}
