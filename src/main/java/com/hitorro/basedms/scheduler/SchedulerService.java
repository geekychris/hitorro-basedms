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
package com.hitorro.basedms.scheduler;

import com.hitorro.basedms.job.JobService;
import com.hitorro.basedms.job.SchedulerAppJobBridge;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.jsontypesystem.propreaders.JVSProperties;
import com.hitorro.util.core.ArrayUtil;
import com.hitorro.util.core.classes.ClassUtil;
import com.hitorro.util.core.events.EventListener;
import com.hitorro.util.core.events.LocalEventHub;
import com.hitorro.util.core.params.JsonKeyMap;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.job.JobRegistration;
import com.hitorro.util.json.keys.propaccess.PropaccessError;
import com.hitorro.util.scheduler.SchedulerContext;
import com.hitorro.util.scheduler.SchedulerIntf;
import com.hitorro.util.startupframework.ServiceContext;
import com.hitorro.util.startupframework.ServiceWrapper;
import com.hitorro.util.startupframework.phases.ServiceDefinition;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * Service which provides scheduled jobs. Mostly implemented with the Quartz package.
 * <p>
 * Services can put to the configs scheduled jobs.  These jobs can be automatically registered
 * if the ServiceDefinition.scheduledJobPath() is set.
 *
 * @author chris
 */
@ServiceDefinition(dependentService = {JobService.class},
        shortName = "scheduler",
        description = "Scheduler service",
        debugCommands = {DumpScheduledJobs.class},
        uiDirectories = {})
public class SchedulerService implements SchedulerIntf, EventListener {
    public static final String AppJobParameters = "appjobparameters";
    // keys for reading job information from properties
    public static final String JobNameKey = "name";
    public static final String JobClassKey = "class";
    public static final String JobPropertiesKey = "properties";
    public static final String JobScheduleSeconds = "scheduleseconds";
    public static final String JobScheduleHours = "schedulehours";
    public static final String JobScheduleOnce = "scheduleonce";
    public static final String JobScheduleCron = "schedulecron";
    private static final String RootTasksKey = "roottasks";
    private boolean _started = false;
    private Scheduler _scheduler = null;

    public static SchedulerService getService() {
        return (SchedulerService) ServiceContext.getSC().getInitializedModule(SchedulerService.class);
    }

    public static final Class getJobClass(String jname, JVS jproperties, String jobClassString) throws PropaccessError {
        Class jobClass;
        JobRegistration ajr = JobService.getService().getAppJobRegistrationByName(jobClassString);

        if (ajr != null) {
            jobClass = SchedulerAppJobBridge.class;
            jproperties.set(SchedulerAppJobBridge.AppJobKey, ajr._jobClass);
        } else {
            jobClass = ClassUtil.getClassForName(jobClassString, Job.class);
        }
        return jobClass;
    }

    public Scheduler getScheduler() {
        return _scheduler;
    }

    public String deInit() {
        if (_started) {
            try {
                _scheduler.shutdown();
            } catch (SchedulerException se) {
                String message = se.toString();
                Log.scheduler.error(message);
                return message;
            }
            _started = false;
        }

        _scheduler = null;

        return null;
    }

    public Map<JobDetail, Trigger> getScheduledJobs() {
/*
        Map<JobDetail, Trigger> jobs = new HashMap<JobDetail, Trigger>();
        for (String group : this._scheduler.getJobGroupNames())
        {
            Set<JobKey> jobKeys  = _scheduler.getJobKeys(GroupMatcher.jobGroupEquals(group))
            for (JobKey key : jobKeys)
            {
                JobDetail detail = _scheduler.getJobDetail(key);

                Trigger trigger = _scheduler.getTrigger(name, group);
                if (detail != null && trigger != null)
                {
                    jobs.put(detail, trigger);
                }
            }
        }
        return jobs;
        */
        return null;
    }

    public String init(boolean dbInit, final boolean upgrading, final long currentVersion, final long targetVersion) {
        // get the scheduler properties out of our properties soup
        Properties sprops = null;
        try {
            sprops = new JVS(JVSProperties.getProperties().get("scheduler")).getAsProperties();
        } catch (PropaccessError e) {
            String message = e.getMessage();
            Log.scheduler.error("Error: %s %e", e, e);
            return message;
        }
        StdSchedulerFactory factory = new StdSchedulerFactory();
        SchedulerContext.set(this);
        try {
            factory.initialize(sprops);
            _scheduler = factory.getScheduler();
        } catch (SchedulerException se) {
            String message = se.getMessage();
            Log.scheduler.error("Error: %s %e", se, se);
            return message;
        }

        // run test jobs
        scheduleJobs(RootTasksKey);
        // register self event handler
        LocalEventHub.get().addEventListener(this, ServiceContext.ServerUp);

        return null;
    }

    /**
     * Schedule a set of jobs. The information about the jobs is taken out of configuration.  The jobsKey indicates what
     * portion of configuration should be used.  See the method scheduleJob for information about exactly what needs to
     * be in the configuration.
     * <p/>
     * Imagine that you have configuration that looks like:
     * <pre>
     * myservice.tasks.checkfiletask.schedulehours=2
     * myservice.tasks.checkfiletask.class=ht.appframework.stuff.CheckFiles
     * myservice.tasks.sendemail.schedulehours=24
     * myservice.tasks.sendemail.class=ht.appframework.stuff.SendEmail
     * </pre>
     * If you then used the jobsKey of "myservice.tasks" you would end up creating the jobruns called "checkfiletask"
     * and "sendemail", with the associated configuration.  With this approach you will not need to specify a name for
     * the jobrun because the name is taken from the configuration key.
     *
     * @param jobsKey Key indicating which portion of the tree to use
     */
    public void scheduleJobs(String jobsKey) {
        // the listFiles of tasks
        JVS pprops = JVSProperties.getProperties();
        List<JsonKeyMap> taskNames = null;
        try {
            taskNames = pprops.getSubMaps(jobsKey);
            // pull out the tree for each task and schedule a job
            for (JsonKeyMap taskname : taskNames) {
                String key = Fmt.S("%s.%s", jobsKey, taskname.getKey());
                scheduleJob(taskname.getKey(), new JVS(taskname.getValue()));
            }
        } catch (PropaccessError propaccessError) {
            propaccessError.printStackTrace();
        }
        if (taskNames == null) {
            return;
        }


    }

    /**
     * Schedule a job to run, based on properties (presumably taken from our configuration). This will create a
     * scheduled task.  The properties contain the information needed to set up the job.  These are: <ul> <li>name - the
     * name of this job run (can be overridden by the jobRunName argument to this function). <li>class - the name of the
     * class which provides the implementation of the job, required to be an implementation of org.quartz.Job
     * <li>properties - a subtree of properties which will be extracted and passed to the job <li>scheduleseconds - a
     * simple schedule, run every N seconds forever <li>schedulehours - a simple schedule, run every N hours forever
     * <li>scheduleonce - run the job once, in N seconds <li>schedulecron - run the job using a cron schedule string
     * </ul> In the normal case this tree of properties will be a subtree of some larger configuration.  The properties
     * class and details are required.  The name property is required if the jobName argument is null.  One of the
     * schedule properties must be provided.
     * <p/>
     * A job can be scheduled multiple times, but must have a unique jobRunName each time.
     *
     * @param properties The job's properties for this run
     * @param jobRunName This run's name.  If null, we expect to see a name in the properties.  If not null this value
     *                   will override any name in the properties.
     * @return an error message if there is a problem (also logged) or null if all is fine
     */
    public String scheduleJob(String jobRunName, JVS properties) throws PropaccessError {
        // run name
        String jname = jobRunName;
        if (jname == null) {
            jname = properties.getString(JobClassKey);
            if (jname == null) {
                String message = "Job does not provide run name";
                Log.scheduler.error(message);
                return message;
            }
        }
        // see if we have

        // job class
        String jobClassString = properties.getString(JobClassKey);
        if (jobClassString == null) {
            String message = "Job does not provide class name";
            Log.scheduler.error(message);
            return message;
        }

        Class jobClass = null;

        JVS jproperties = new JVS(properties.get(JobPropertiesKey));
        jobClass = getJobClass(jname, jproperties, jobClassString);
        if (jobClass == null) {
            String message = StringUtil.strcat("Job has bad jobclass: ", jobClassString);
            Log.scheduler.error(message);
            return message;
        }

        // schedule
        Trigger trigger = makeTriggerFromProperties(properties);
        if (trigger == null) {
            String message = "Job does not provide schedule information";
            Log.scheduler.error(message);
            return message;
        }
        return scheduleJob(jname, jobClass, trigger, jproperties);
    }

    /**
     * Schedule a job to run once, right now. This will create a scheduled task. A job can be scheduled multiple times,
     * but must have a unique jobRunName each time.
     *
     * @param jname       the name of this job run (can be overridden by the jobRunName argument to this function).
     * @param jobClass    the Class of the job to run, must be a subclass of Job
     * @param jproperties The job's properties for this run, may be null
     * @return an error message if there is a problem (also logged) or null if all is fine
     */
    public String scheduleJobNow(String jname, Class jobClass, JVS jproperties) {
    /*
        Trigger trigger = TriggerUtils.makeSecondlyTrigger(1, 0);
        trigger.setStartTime(new Date());
        return scheduleJob(jname, jobClass, trigger, jproperties);
        */
        return null;
    }

    /**
     * Schedule a job to run. This will create a scheduled task. A job can be scheduled multiple times, but must have a
     * unique jobRunName each time.
     *
     * @param jname       the name of this job run (can be overridden by the jobRunName argument to this function).
     * @param jobClass    the Class of the job to run, must be a subclass of Job
     * @param trigger     the Quartz trigger which is used to schedule the job's execution
     * @param jproperties The job's properties for this run, may be null
     * @return an error message if there is a problem (also logged) or null if all is fine
     */
    public String scheduleJob(String jname, Class jobClass, Trigger trigger, JVS jproperties) {
        /*
        jobClass = getJobClass(jname, jproperties, jobClass.getCanonicalName());


        if (jname == null)
        {
            String message = "Job does not provide run name";
            Log.scheduler.error(message);
            return message;
        }

        // give the trigger the same name as the job run
        trigger.setName(jname);

        try
        {
            // set up the details and kick off the job
            JobDetail detail = new JobDetail(jname, Scheduler.DEFAULT_GROUP, jobClass);
            if (jproperties != null)
            {
                JobDataMap jobMap = new JobDataMap(jproperties);
                detail.setJobDataMap(jobMap);
            }

            _scheduler.scheduleJob(detail, trigger);

        }
        catch (SchedulerException se)
        {
            String message = se.toString();
            Log.scheduler.error(message);
            return message;
        }

        // everything worked
        Log.scheduler.info(Fmt.S("Job run %s started successfully", jname));
        */
        return null;
    }

    /**
     * Stop one of our scheduled jobs immediately.
     *
     * @param jname the job name
     */
    public void stopJobNow(String jname) {
        /*try
        {
            _scheduler.deleteJob(jname, Scheduler.DEFAULT_GROUP);
        }
        catch (SchedulerException exc)
        {
            Log.scheduler.error(exc);
        }*/
    }

    /**
     * Create a scheduler trigger from the task properties. The trigger will have the correct period and start time.
     *
     * @param properties The source of our trigger information
     * @return a constructed Trigger, or null if there is a problem
     */
    private Trigger makeTriggerFromProperties(JVS properties) {
        /*Trigger result;
        String sched;
        Calendar now = new GregorianCalendar();

        try
        {
            sched = properties.get(JobScheduleCron);
            if (sched != null && sched.length() > 0)
            {
                result = new CronTrigger("CronTrigger", null, sched);
                return result;
            }

            sched = properties.get(JobScheduleOnce);
            if (sched != null)
            {
                result = TriggerUtils.makeSecondlyTrigger(1, 0);
                now.put(Calendar.SECOND, Integer.parseInt(sched));
                result.setStartTime(now.getTime());
                return result;
            }

            sched = properties.get(JobScheduleSeconds);
            if (sched != null)
            {
                int nn = Integer.parseInt(sched);
                result = TriggerUtils.makeSecondlyTrigger(nn);
                now.put(Calendar.SECOND, nn);
                result.setStartTime(now.getTime());
                return result;
            }

            sched = properties.get(JobScheduleHours);
            if (sched != null)
            {
                int nn = Integer.parseInt(sched);
                result = TriggerUtils.makeHourlyTrigger(nn);
                now.put(Calendar.HOUR, nn);
                result.setStartTime(now.getTime());
                return result;
            }

        }
        catch (NumberFormatException nfe)
        {
            Log.scheduler.error(Fmt.S("Error while creating job run: ", nfe));
            return null;
        }
        catch (ParseException pe)
        {
            Log.scheduler.error(Fmt.S("Error parsing cron schedule string", pe));
            return null;
        }
        */
        // no schedule
        return null;
    }

    public String start(boolean dbInit) {
        if (_started) {
            return null;
        }

        try {
            _scheduler.start();
            _started = true;
        } catch (SchedulerException schex) {
            String message = schex.toString();
            Log.scheduler.error(message);
            return message;
        }


        return null;
    }

    @Override
    public boolean event(final String topic, final String subTopic, final Object args) {
        List<ServiceWrapper> services = ServiceContext.getSC().getServices();
        for (ServiceWrapper service : services) {
            String paths[] = service.getSchedulerJobPaths();
            if (!ArrayUtil.nullOrEmpty(paths)) {
                for (String path : paths) {
                    this.scheduleJobs(path);
                }
            }
        }
        return true;
    }

    @Override
    public String eventName() {
        return "SchedulerServiceEvents";
    }

    @Override
    public boolean runAsync() {
        return false;
    }
}
