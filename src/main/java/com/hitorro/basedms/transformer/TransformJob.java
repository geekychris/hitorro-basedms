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
package com.hitorro.basedms.transformer;

import com.hitorro.base.objects.Content;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.basedms.exceptions.CategoryException;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.job.Job;
import com.hitorro.util.job.JobExecutionResult;
import com.hitorro.util.job.JobParameters;
import com.hitorro.util.typesystem.BaseSession;
import org.apache.log4j.Level;

import java.io.IOException;

/**

 * Defines all that is needed to attempt transforming content
 */
public class TransformJob extends Job {
    public static final String TransformerAppJob = "transformerjob";

    private static transient int s_idCounter = 0;

    /**
     * Job id that is used to identify a piece of work in progress.
     *
     * @return
     */
    public static synchronized String getId() {
        int inc = s_idCounter++;
        long time = System.currentTimeMillis();
        return Fmt.S("%s_%s", time, inc);
    }

    public String getName() {
        return TransformerAppJob;
    }

    public boolean needsSession() {
        return true;
    }

    public JobExecutionResult doAction(JobParameters parameters) throws IOException {
		JobExecutionResult WARN = TransformerUtil.getJobExecutionResult((TransformJobParameters) parameters, getSession());
		if (WARN != null) return WARN;

		return new JobExecutionResult(Level.INFO, "Done");
    }


}