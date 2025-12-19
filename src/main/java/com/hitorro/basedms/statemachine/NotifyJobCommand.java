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
package com.hitorro.basedms.statemachine;

import com.hitorro.base.objects.PersistedSerializedObject;
import com.hitorro.basedms.BaseUtil;
import com.hitorro.basedms.queue.JobQueueElement;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.thread.farm.FarmCommand;
import com.hitorro.util.job.JobExecutionResult;
import com.hitorro.util.typesystem.BaseSession;
import org.apache.log4j.Level;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved.
 * <p/>
 * User: chris
 */
public class NotifyJobCommand extends FarmCommand<JobQueueElement<PersistedSerializedObject>,
        JobQueueElement<PersistedSerializedObject>, Object> {

    public JobQueueElement<PersistedSerializedObject> apply(JobQueueElement<PersistedSerializedObject> inElement) {
        PersistedSerializedObject pso = inElement.getPayload();
        BaseSession session = DMSSessionFactory.getFactory().getSession();
        JobExecutionResult jer = null;
        try {
            jer = BaseUtil.notify(session, pso, false);
            inElement.setResult(jer);
            return inElement;
        } finally {
            if (jer == null) {

            }
            if (jer != null && jer.getErrorLevel().equals(Level.INFO)) {
                DMSSessionFactory.getFactory().commitAndCloseSession(session);
            } else {
                // notify failed, lets not commit it.
                DMSSessionFactory.getFactory().rollbackCloseSession(session);
            }

        }
    }
}