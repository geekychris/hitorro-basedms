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
package com.hitorro.basedms.queue;

import com.hitorro.basedms.job.JobService;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.core.string.Fmt;

import java.util.List;

/**
 * <p/>
 */
@CommandDef(command = "job.dumpqueues", description = "Dump the persisted queues")
public class DumpQueues extends com.hitorro.util.commandandcontrol.Command {
    public boolean execute(String rawValue, JVS args, com.hitorro.util.commandandcontrol.Response response, com.hitorro.util.commandandcontrol.CommandSession session, com.hitorro.util.commandandcontrol.RestOperations operation) throws Exception {
        List<PSOQueueProcessor> list = JobService.getQueues();
        for (PSOQueueProcessor qp : list) {
            int deferred = 0;
            String deferredString = "NA";
            if (qp instanceof GroupSpacedPSOQueueProcessor) {
                GroupSpacedPSOQueueProcessor qpg = (GroupSpacedPSOQueueProcessor) qp;
                deferred = qpg.getDeferredQueueCount();
                deferredString = Integer.toString(deferred);
            }
            response.addInfo(com.hitorro.util.commandandcontrol.InfoLevel.Info, Fmt.S("name: %s inQ: collectionId: %s, inQ: %s, outQ: %s, workers: %s, deferred: %s",
                    qp.getNames(), qp.getCollectionId(), qp.getInQueueCount(),
                    qp.getOutQueueCount(), qp.getWorkerThreadCount(), deferredString));
        }
        return true;
    }
}
