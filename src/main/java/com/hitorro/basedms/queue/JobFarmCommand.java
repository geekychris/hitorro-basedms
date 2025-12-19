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

import com.hitorro.base.objects.PersistedSerializedObject;
import com.hitorro.basedms.job.JobService;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.core.thread.farm.FarmCommand;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.job.Job;
import com.hitorro.util.job.JobExecutionResult;
import com.hitorro.util.job.JobParameters;
import com.hitorro.util.job.JobRegistration;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.BaseSessionFactory;
import com.hitorro.util.typesystem.HTSerializable;
import org.apache.log4j.Level;
import org.quartz.JobExecutionException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Jan 28, 2005 Time: 7:30:09 PM
 */
public class JobFarmCommand extends FarmCommand<JobQueueElement<PersistedSerializedObject>, JobQueueElement<PersistedSerializedObject>, Object> {

    private JobQueueElement<PersistedSerializedObject> error(JobQueueElement<PersistedSerializedObject> jqe, String msg, Object... args) {
        JobExecutionResult jer = new JobExecutionResult(Level.ERROR, msg, args);
        jqe.setResult(jer);
        return jqe;

    }


    public JobQueueElement apply(JobQueueElement<PersistedSerializedObject> inElement) {
        PersistedSerializedObject pso = inElement.getPayload();

        JobParameters params = null;
        try {
            HTSerializable j = pso.getObject();
            if (j == null) {
                return error(inElement, "PSO element does not have a serialized Object");
            }
            if (j instanceof JobParameters) {
                params = (JobParameters) j;
            } else {

                return error(inElement, "PSO is not a JobParameters object, its a: %s", j.getClass().getCanonicalName());
            }
        } catch (SQLException e) {
            return error(inElement, "SqlException reconstituting job parameters %s", e);
        } catch (IOException e) {
            return error(inElement, "IOException reconstituting job parameters %s", e);
        } catch (StoreException e) {
            return error(inElement, "StoreException reconstituting job parameters %s", e);
        } catch (ClassNotFoundException e) {
            return error(inElement, "ClassNotFoundException reconstituting job parameters %s", e);
        }
        JobRegistration registration = null;
        if (JobService.getService() == null) {
            return null;
        } else {
            registration = JobService.getService().getAppJobRegistrationByName(params.getJobName());
        }
        if (registration == null) {
            error(inElement, "Unable to find a registered job with job name %s", params.getJobName());
        }
        Job appJob = registration.getAppJob();

        if (appJob == null) {
            return null;
        }
        BaseSession session = null;

        try {
            if (appJob.needsSession()) {
                session = BaseSessionFactory.getFactory().getSession();
                appJob.setSession(session);
            }
            JobExecutionResult jer = appJob.doAction(params);

            if (jer.getErrorLevel() == Level.INFO || !jer.shouldRetry()) {
                if (StringUtil.nullOrEmptyString(jer.getNewEventName()) &&
                        !StringUtil.nullOrEmptyString(params.getNotifyGuid())) {
                    // we are not switching to a different queue and we have marked ourselfs for a notify.
                    // this probably should change in the future to a copy and save if we want to go onto
                    // another queue and notify.
                    jer.setNewEventName("notify", PersistedSerializedObject.CollectionID_NotificationQueue);
                }
            }

            inElement.setResult(jer);

        } catch (JobExecutionException exc) {
            return error(inElement, "Error running class %s with error %s", appJob.getClass().getCanonicalName(), exc);
        } catch (IOException exc) {
            return error(inElement, "Error running class %s with error %s", appJob.getClass().getCanonicalName(), exc);
        } finally {
            if (session != null) {
                // any work that the session wanted committed should be committed in the action
                // rollback anything left over
                session.rollback();
                DMSSessionFactory.closeSession(session);
                session = null;
            }
        }
        return inElement;
    }

    public JobQueueElement produceJob(JobQueueElement<PersistedSerializedObject> inElement) {
        PersistedSerializedObject pso = inElement.getPayload();

        JobParameters params = null;
        try {
            HTSerializable j = pso.getObject();
            if (j == null) {
                return error(inElement, "PSO element does not have a serialized Object");
            }
            if (j instanceof JobParameters) {
                params = (JobParameters) j;
            } else {

                return error(inElement, "PSO is not a JobParameters object, its a: %s", j.getClass().getCanonicalName());
            }
        } catch (SQLException e) {
            return error(inElement, "SqlException reconstituting job parameters %s", e);
        } catch (IOException e) {
            return error(inElement, "IOException reconstituting job parameters %s", e);
        } catch (StoreException e) {
            return error(inElement, "StoreException reconstituting job parameters %s", e);
        } catch (ClassNotFoundException e) {
            return error(inElement, "ClassNotFoundException reconstituting job parameters %s", e);
        }
        JobRegistration registration = JobService.getService().getAppJobRegistrationByName(params.getJobName());
        if (registration == null) {
            error(inElement, "Unable to find a registered job with job name %s", params.getJobName());
        }
        Job appJob = registration.getAppJob();

        if (appJob == null) {
            return null;
        }
        BaseSession session = null;

        try {
            if (appJob.needsSession()) {
                session = BaseSessionFactory.getFactory().getSession();
                appJob.setSession(session);
            }
            JobExecutionResult jer = appJob.doAction(params);

            if (jer.getErrorLevel() == Level.INFO || !jer.shouldRetry()) {
                if (StringUtil.nullOrEmptyString(jer.getNewEventName()) &&
                        !StringUtil.nullOrEmptyString(params.getNotifyGuid())) {
                    // we are not switching to a different queue and we have marked ourselfs for a notify.
                    // this probably should change in the future to a copy and save if we want to go onto
                    // another queue and notify.
                    jer.setNewEventName("notify", PersistedSerializedObject.CollectionID_NotificationQueue);
                }
            }

            inElement.setResult(jer);

        } catch (JobExecutionException exc) {
            return error(inElement, "Error running class %s with error %s", appJob.getClass().getCanonicalName(), exc);
        } catch (IOException exc) {
            return error(inElement, "Error running class %s with error %s", appJob.getClass().getCanonicalName(), exc);
        } finally {
            if (session != null) {
                // any work that the session wanted committed should be committed in the action
                // rollback anything left over
                session.rollback();
                DMSSessionFactory.closeSession(session);
                session = null;
            }
        }
        return inElement;
    }
}
