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
package com.hitorro.basedms.debugcommand;

import com.hitorro.util.commandandcontrol.CommandSession;
import com.hitorro.util.core.Log;
import com.hitorro.util.job.Job;
import com.hitorro.util.job.JobExecutionResult;
import com.hitorro.util.job.JobParameters;
import org.apache.log4j.Level;
import org.quartz.JobExecutionException;



public class ActionDebugJob extends Job {
    public static final String JobName = "actiondebugjob";

    /**
     * This job must run on the db leader.
     *
     * @return
     */
    public boolean getMustRunOnDBLeader() {
        return true;
    }

    public String getName() {
        return JobName;
    }

    public boolean needsSession() {
        // don't need a session because ComputeConversation does its own session
        return false;
    }

    public JobExecutionResult doAction(JobParameters parameters)
            throws JobExecutionException {
        Log.commands.info(JobName);
        try {
            String action = null;
            String params = null;

            if (CommandSession.executeToLog(action, params)) {
                return new JobExecutionResult(Level.ERROR, "Action unknown were not provided: %s", action);
            }

            return JobExecutionResult.Executed;
        } finally {
            Log.commands.info("ActionJob finished");
        }
    }


}
