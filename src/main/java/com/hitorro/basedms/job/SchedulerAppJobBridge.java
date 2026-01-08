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
package com.hitorro.basedms.job;

import com.hitorro.base.objects.ScheduledJob;
import com.hitorro.basedms.BaseUtil;
import com.hitorro.basedms.scheduler.SchedulerService;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.classes.ClassUtil;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.job.Job;
import com.hitorro.util.job.JobExecutionResult;
import com.hitorro.util.job.JobParameters;
import com.hitorro.util.job.PropertiesJobParameters;
import com.hitorro.util.typesystem.BaseSession;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * <p/>
 * Bridge between the scheduler and a j
 */
public class SchedulerAppJobBridge implements org.quartz.Job {
    public static final String AppJobKey = "appjobkey";

    public SchedulerAppJobBridge() {

    }

    public final void execute(JobExecutionContext jobExecutionContext)
            throws JobExecutionException {
        Log.scheduledJobs.info("SchedulerAppJobBridge");
        JobDetail detail = jobExecutionContext.getJobDetail();
        JobDataMap dmap = detail.getJobDataMap();

        Class appJobClass = (Class) dmap.get(AppJobKey);
        Job appJob;
        appJob = (Job) ClassUtil.getInstanceSwallowError(appJobClass, Job.class);
        if (appJob == null) {
            Log.scheduledJobs.error("Unable to execute Job as class not provided or is not an Job");
            return;
        }
        if (!appJob.canRunOnThisNode()) {
            Log.scheduledJobs.info("Unable to execute Job %s as its not on the leader node", appJob.getName());
            return;
        }
        JobParameters parameters = (JobParameters) dmap.get(SchedulerService.AppJobParameters);


        BaseSession session = null;


        if (parameters == null) {
            String parametersGuid = dmap.getString(ScheduledJob.ParametersGuidKey);
            if (parametersGuid != null) {
                session = DMSSessionFactory.getFactory().getSession();
                // fetch the parameters object
                parameters = ScheduledJob.fetchParametersFromGuid(session, parametersGuid);
            }
            parameters = new PropertiesJobParameters(dmap);
        }

        try {
            if (appJob.needsSession()) {
                if (session == null) {
                    session = DMSSessionFactory.getFactory().getSession();
                }
                appJob.setSession(session);
            }
            JobExecutionResult result = appJob.doAction(parameters);
            // note that we notify if we failed or succeeded.
            BaseUtil.notifyFromParameters(parameters);
        } catch (JobExecutionException exc) {
            String msg = Fmt.S("Error running job %s: ", getClass().getName());
            Log.scheduledJobs.error(msg, exc);

            // rethrow the exception, as quartz expects
            throw exc;
        } catch (Exception e) {
            // not a great thing todo to catch all exceptions!
            String msg = Fmt.S("Error running job %s: ", getClass().getName());
            Log.scheduledJobs.error(msg, e);

            // rethrow the exception, as quartz expects
            throw new JobExecutionException(e);
        } finally {
            if (session != null) {
                // any work that the session wanted committed should be committed in the action
                // rollback anything left over
                session.rollback();
                DMSSessionFactory.closeSession(session);
                session = null;
            }
            Log.scheduledJobs.info("Finished SchedulerAppJobBridge for %s", appJobClass.toString());
        }
    }
}
