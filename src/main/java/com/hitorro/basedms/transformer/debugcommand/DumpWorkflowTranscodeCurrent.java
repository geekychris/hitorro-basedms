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
package com.hitorro.basedms.transformer.debugcommand;

import com.hitorro.basedms.transformer.squeeze.SqueezeService;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.commandandcontrol.ano.RespColumn;
import com.hitorro.util.commandandcontrol.ano.ResponseDefinition;
import com.hitorro.util.core.Constants;
import com.hitorro.util.core.string.StringUtil;

import java.util.Date;

@CommandDef(command = "transform.dumpworkflowtranscodecurrent", description = "Dump current transcode job for Squeeze Service")
public class DumpWorkflowTranscodeCurrent extends com.hitorro.util.commandandcontrol.Command {

    private static final long DefaultTranscodeUpdateTime = 0L;
    @ResponseDefinition(command = "workflow",
            rowname = "item",
            columns = {@RespColumn(name = "Job ID", lName = "jobid"),
                    @RespColumn(name = "File Name", lName = "filename"),
                    @RespColumn(name = "Effective Date", lName = "effectivedate"),
                    @RespColumn(name = "Percent", lName = "percent")})
    private com.hitorro.util.commandandcontrol.ResponseShape shape = new com.hitorro.util.commandandcontrol.ResponseShape();

    public boolean execute(String rawValue, JVS args, com.hitorro.util.commandandcontrol.Response response, com.hitorro.util.commandandcontrol.CommandSession session, com.hitorro.util.commandandcontrol.RestOperations operation) throws Exception {

        SqueezeService service = SqueezeService.getService();

        String[] responseRow = new String[4];
        Long transcodeLastUpdateTime = DefaultTranscodeUpdateTime;

        if (service != null) {
            responseRow[RowElement.File.ordinal()] = service.getProcessingFile();
            responseRow[RowElement.JobId.ordinal()] = service.getProcessingJobId();
            responseRow[RowElement.Percentage.ordinal()] = service.getProcessingPercentComplete();

            transcodeLastUpdateTime = service.getProcessingLastUpdate();
            if (transcodeLastUpdateTime != DefaultTranscodeUpdateTime) {
                responseRow[RowElement.LastUpdated.ordinal()] = new Date(transcodeLastUpdateTime).toString();
            }
        }


        for (int i = 0; i < responseRow.length; i++) {
            if (StringUtil.nullOrEmptyOrBlankString(responseRow[i])) {
                responseRow[i] = Constants.EmptyString;
            }
        }

        response.setResponseShape(shape);
        if (StringUtil.nullOrEmptyString(responseRow[RowElement.JobId.ordinal()]) ||
                StringUtil.nullOrEmptyString(responseRow[RowElement.File.ordinal()])) {
            response.addInfo(com.hitorro.util.commandandcontrol.InfoLevel.Info, "Squeeze Service not currently transforming any files.");
        }

        response.addRow(new Object[]{responseRow});
        response.end();
        return true;
    }

    private enum RowElement {
        JobId, File, LastUpdated, Percentage
    }


}
