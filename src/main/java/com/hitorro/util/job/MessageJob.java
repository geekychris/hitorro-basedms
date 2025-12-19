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
package com.hitorro.util.job;

import com.hitorro.util.core.Console;
import com.hitorro.util.core.Log;
import org.apache.log4j.Level;

/**
 * Simple job that will print a message.
 */
public class MessageJob extends Job {

    public static final String MessageJob = "messagejob";

    public JobExecutionResult doAction(JobParameters parameters) {
        if (!(parameters instanceof MessageJobParameters)) {
            return new JobExecutionResult(Level.ERROR, "MessageJobParameters were not provided, got %s",
                    parameters.getClass().getCanonicalName());
            // we only work off parameters
        }

        MessageJobParameters mjb = (MessageJobParameters) parameters;
        String msg = mjb.getMessage();
        int outkind = mjb.getOutputKind();
        switch (outkind) {
            case MessageJobParameters.ToConsoleMessage:
                Console.println(msg);
                break;
            case MessageJobParameters.ToLogMessage:
                Log.scheduledJobs.info(msg);
                break;
            default:
                break;
        }
        return JobExecutionResult.Executed;
    }

    public String getName() {
        return MessageJob;
    }

    public boolean needsSession() {
        return false;
    }
}
