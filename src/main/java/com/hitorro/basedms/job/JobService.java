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

import com.hitorro.base.objects.BaseDMSService;
import com.hitorro.base.objects.PersistedSerializedObject;
import com.hitorro.basedms.debugcommand.ActionDebugJob;
import com.hitorro.basedms.debugcommand.ActionDebugJobParameters;
import com.hitorro.basedms.queue.DumpQueues;
import com.hitorro.basedms.queue.GroupSpacedPSOQueueProcessor;
import com.hitorro.basedms.queue.JobFarmCommand;
import com.hitorro.basedms.queue.PSOQueueProcessor;
import com.hitorro.util.core.ListValue;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.classes.ClassUtil;
import com.hitorro.util.json.keys.BooleanProperty;
import com.hitorro.util.json.keys.IntegerProperty;
import com.hitorro.util.startupframework.ServiceContext;
import com.hitorro.util.startupframework.phases.ServiceDefinition;
import com.hitorro.util.typesystem.HTSerializable;
import com.hitorro.util.typesystem.Type;
import com.hitorro.util.typesystem.TypeManager;
import com.hitorro.util.typesystem.annotation.ViewClassReference;
import com.hitorro.util.job.JobRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Jan 25, 2005 Time: 5:45:45 PM.
 */
@ServiceDefinition(dependentService = {BaseDMSService.class},
        shortName = "job",
        description = "Job service",
        debugCommands = {DumpQueues.class},
        typeManagedClasses = {},
        uiDirectories = {})
public class JobService implements com.hitorro.util.job.JobServiceIntf {

    public static final String TranscoderKey = "transcoder";
    public static final String JobKey = "job";
    public static final String HTMLFetcherKey = "htmlFetcher";
    public static final String WorkflowKey = "workflow";

    public static final IntegerProperty JobThreads = new IntegerProperty("jobs.threads", "Number of threads", 5);
    public static final IntegerProperty HTTPJobThreads = new IntegerProperty("jobs.http.threads", "Number of threads", 5);
    public static final BooleanProperty DisableJobService = new BooleanProperty("jobs.disable", "", false);
    public static final String PSO_JOB_NAME = "jobs";
    public static JobService _service;
    private static List<PSOQueueProcessor> queues = new ArrayList<PSOQueueProcessor>();
    private boolean _started = false;
    // registered scheduled jobs
    private Map<String, com.hitorro.util.job.JobRegistration> _registeredJobsByDisplayName;
    private Map<String, JobRegistration> _registeredJobsByJobName;
    private Map<String, JobRegistration> _registeredJobsByClass = new HashMap<String, JobRegistration>();
    private boolean m_startJobQueue = true;
    private int m_queueLength = 40;
    //private int m_threads = 4;
    private int m_threads = JobThreads.apply();
    private int m_httpThreads = HTTPJobThreads.apply();

    public JobService() {
        _registeredJobsByDisplayName = new HashMap<String, JobRegistration>();
        _registeredJobsByJobName = new HashMap<String, JobRegistration>();
    }

    public static void addQueue(PSOQueueProcessor q) {
        queues.add(q);
    }

    public static PSOQueueProcessor getQueue(String name) {
        for (PSOQueueProcessor p : queues) {
            String n = p.getName();
            if (n.equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    public static List<PSOQueueProcessor> getQueues() {
        return queues;
    }

    public static JobService getService() {
        return (JobService) ServiceContext.getSC().getInitializedModule(JobService.class);
    }

    public void disableQueue(String name) {
        PSOQueueProcessor p = getQueue(name);
        if (p != null) {
            p.disable();
        }
    }

    public void enableQueue(String name, int mods[], int mod) {
        PSOQueueProcessor p = getQueue(name);
        if (p != null) {
            if (mod == mods.length) {
                // optimization to not include the mod crap....since it looks like we are managing everything.
                mods = null;
            }
            p.setModValues(null, mod);
            p.enable();
        }
    }

    private void startJobQueue() {
        if (m_startJobQueue) {

            JobFarmCommand jfc = new JobFarmCommand();
            PSOQueueProcessor<PersistedSerializedObject> qp = new PSOQueueProcessor<PersistedSerializedObject>(JobKey, "PSO-JobService", m_queueLength, m_threads, jfc);
            qp.addNames(PSO_JOB_NAME);
            qp.start();

            // create a queue just for HTML
            GroupSpacedPSOQueueProcessor<PersistedSerializedObject> httpQp = new GroupSpacedPSOQueueProcessor<PersistedSerializedObject>(HTMLFetcherKey, "PSO-JobService",
                    m_httpThreads * 6, m_httpThreads, jfc,
                    PersistedSerializedObject.CollectionID_HTMLQueue);
            httpQp.addNames(PSO_JOB_NAME);
            httpQp.setOrderByMod(1000);
            httpQp.start();
        }
    }

    public String deInit() {

        if (_started) {
            _started = false;
        }
        _service = null;
        return null;
    }

    public String init(boolean dbInit, final boolean upgrading, final long currentVersion, final long targetVersion) {
        _service = this;
        com.hitorro.util.job.JobServiceBase.setJobServiceBase(this);
        registerAppJob(com.hitorro.util.job.NoOpJob.class, "NoOp Job", com.hitorro.util.job.DecisionJobParameters.class);
        JobService js = JobService.getService();

        registerAppJob(ActionDebugJob.class, "ActionJob",
                ActionDebugJobParameters.class);

        return null;
    }

    public String start(boolean dbInit) {
        if (_started) {
            return null;
        }

        if (!DisableJobService.apply()) {
            startJobQueue();
        } else {
            Log.scheduledJobs.info("JobService is disabled.");
        }


        _started = true;

        return null;
    }

    /**
     * Register an application scheduled job
     *
     * @param jobClass       the class of the executable job, should be a subclass of Job
     * @param displayName    string to use as the display name of the job, used in the admin user interface
     * @param parameterClass class of parameters for the job, or null if there is none
     */
    public void registerAppJob(Class jobClass, String displayName, Class parameterClass) {
        registerAppJob(jobClass, displayName, parameterClass, ViewClassReference.EditView);
    }

    public void registerAppJob(Class jobClass, String displayName, Class parameterClass, String viewName) {
        if (!ClassUtil.isSubClass(jobClass, com.hitorro.util.job.Job.class)) {
            Log.scheduledJobs.error("Attempt to register non-Job class %s as an application job", jobClass);
            return;
        }
        if (parameterClass != null && !ClassUtil.isSubClass(parameterClass, HTSerializable.class)) {
            Log.scheduledJobs.error("Attempt to use non-serializable class %s for job parameters", parameterClass);
            return;
        }
        JobRegistration newReg = new JobRegistration(jobClass, displayName, parameterClass, viewName);
        JobRegistration oldReg = _registeredJobsByDisplayName.put(displayName, newReg);
        _registeredJobsByClass.put(newReg._jobClassString, newReg);
        _registeredJobsByJobName.put(newReg._name, newReg);

        if (oldReg != null) {
            Log.scheduledJobs.warn("duplicate job registration for %s, had class of %s, now have %s",
                    displayName, oldReg._jobClass, jobClass);
        }

        if (parameterClass != null) {
            // register the parameter class with the type manager, but only if it hasn't already been registered
            TypeManager tm = TypeManager.getTypeManager();
            Type oldType = tm.getTypeForClass(parameterClass);
            if (oldType == null || oldType.getImplementationClass() != parameterClass) {
                tm.addTypeIfAbsent(parameterClass);
            }
        }
    }

    public JobRegistration getAppJobRegistrationByName(String name) {
        JobRegistration jrResult = _registeredJobsByJobName.get(name.toLowerCase());
        if (jrResult != null) {
            return jrResult;
        }
        // lets check to see if we have a jr with that class name anyway.
        return this._registeredJobsByClass.get(name);
    }

    /**
     * Get the select listFiles of registered scheduled jobs.
     *
     * @return ListValue array, suitable for select listFiles, with display names and class names
     */
    public ListValue[] getRegisteredJobsSelectList() {
        ListValue[] result = new ListValue[_registeredJobsByDisplayName.size()];
        int indx = 0;
        for (JobRegistration reg : _registeredJobsByDisplayName.values()) {
            result[indx++] = new ListValue(reg._displayName, reg._jobClass.getName());
        }

        return result;
    }

    /**
     * Get the parameter class for a scheduled job.
     *
     * @param jobClassName class name of the scheduled job
     * @return the class of the parameters for the job or null if there is none
     */
    public Class getJobParameterClass(String jobClassName) {
        JobRegistration jreg = getJobRegistration(jobClassName);
        return (jreg != null) ? jreg._parameterClass : null;
    }

    /**
     * Get the name of the view class for a scheduled job.
     *
     * @param jobClassName class name of the scheduled job
     * @return the name of the job edit view or null if there is none
     */
    public String getJobParameterView(String jobClassName) {
        JobRegistration jreg = getJobRegistration(jobClassName);
        return (jreg != null) ? jreg._viewName : null;
    }

    private JobRegistration getJobRegistration(String jobClassName) {
        if (jobClassName == null) {
            return null;
        }
        // stupid linear search
        for (JobRegistration reg : _registeredJobsByDisplayName.values()) {
            if (jobClassName.equals(reg._jobClass.getName())) {
                return reg;
            }
        }

        return null;
    }
}

