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
import org.apache.log4j.Level;

import java.io.IOException;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved.
 * <p/>
 * User: chris
 * <p/>
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
        TransformJobParameters params = (TransformJobParameters) parameters;

        String method = params.getTransformerMethod();
        TransformMethod m = TransformerService.getService().getMethod(method);
        if (m == null) {
            return new JobExecutionResult(Level.WARN, "Unable to find convertion edge for  %s", method);
        }
        if (!m.ensureServiceAvailable()) {
            return new JobExecutionResult(Level.WARN, "service not running for method %s", method);
        }
        String soGuid = params.getJobGuid();
        VersionableObject so = (VersionableObject) getSession().getObjectFromGuid(soGuid);
        if (so == null) {
            return new JobExecutionResult(Level.WARN, "Unable to find system object for transformation %s", soGuid);
        }

        Content c = so.getContentByConstraint(params.getContentConstraint(), false);
        if (c == null) {
            return new JobExecutionResult(Level.WARN, "Unable to find content for system object for transformation %s", soGuid);
        }

        BaseFile sourceFile = c.getContentFile();
        BaseFile targetFile = null;
        try {
            targetFile = m.convert(sourceFile, params.getJobId(), params.getTransformerMethodArgs(), params.getNotifyGuid(), 0);
            if (!BaseFile.notNullAndExists(targetFile)) {
                return new JobExecutionResult(Level.ERROR, "Unable to convertToPdf %s file %s no output file generated.", soGuid, sourceFile);
            }
            if (params.getAddContentAsChildOfContent()) {
                long size = sourceFile.length();
                params.getContentSetter().setFile(targetFile, getSession(), c, true);
            } else {
                params.getContentSetter().setFile(targetFile, getSession(), null, true);
            }
        } catch (IOException ioe) {
            return new JobExecutionResult(Level.WARN, "Unable to convertToPdf %s file %s error %s", soGuid, sourceFile, ioe.getMessage());
        } catch (CategoryException ce) {
            return new JobExecutionResult(Level.WARN, "Unable to convertToPdf %s file %s error %s", soGuid, sourceFile, ce.getMessage());
        } catch (StoreException se) {
            return new JobExecutionResult(Level.WARN, "Unable to convertToPdf %s file %s error %s", soGuid, sourceFile, se.getMessage());
        } finally {
            //if (FileUtil.notNullAndExists(sourceFile))
            //{
            //    sourceFile.delete();
            //}
            if (!BaseFile.notNullAndExists(targetFile)) {
                targetFile.delete();
            }
        }


        return new JobExecutionResult(Level.INFO, "Done");
    }
}