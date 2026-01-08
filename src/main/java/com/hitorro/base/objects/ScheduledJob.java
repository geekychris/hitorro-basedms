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
package com.hitorro.base.objects;


import com.hitorro.basedms.job.JobService;
import com.hitorro.basedms.scheduler.SchedulerService;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.util.core.Constants;
import com.hitorro.util.core.ListValue;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.job.Job;
import com.hitorro.util.job.JobParameters;
import com.hitorro.util.json.keys.propaccess.PropaccessError;
import com.hitorro.util.startupframework.ServiceContext;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;
import com.hitorro.util.typesystem.annotation.UiProperties;
import com.hitorro.util.typesystem.annotation.UiTypeProperties;
import com.hitorro.util.typesystem.annotation.ViewClassReference;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 */
@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.ScheduledJob,
        isView = false,
        isPersisted = true,
        schemaVersion = ScheduledJob.SerializationVersion)
@UiTypeProperties(name = "Scheduled Job",
        views = {@ViewClassReference(name = ViewClassReference.ListView, viewClass = ScheduledJob.ScheduledJobListView.class),
                @ViewClassReference(name = ViewClassReference.EditView, viewClass = ScheduledJob.ScheduledJobEditView.class)})
public class ScheduledJob extends VersionableObject {
    public static final int SerializationVersion = 1;
    public static final String ParametersGuidKey = "parametersguid";
    private static ListValue[] _classChoices;
    private static String ActiveJobsQuery = " where state = " + Constants.ActiveState;
    private String _name;
    private String _jobName;
    private String _chronSchedule;
    private int _secondsSchedule;
    private String _parametersGuid;

    public ScheduledJob() {
        setState(Constants.InactiveState);
    }

    public static List<ScheduledJob> getActiveJobs(DMSSession session) {
        List<ScheduledJob> jobs = new ArrayList<ScheduledJob>();
        session.getObjects(ScheduledJob.class, ActiveJobsQuery, jobs);

        return jobs;
    }

    /**
     * Get the parameters object out of the database.
     *
     * @param session database session to use
     * @param guid    the guid of the parameters, usually taken from the ScheduledJob instance
     * @return the parameters object, or null if it can't be loaded
     */
    public static JobParameters fetchParametersFromGuid(BaseSession session, String guid) {
        if (guid == null) {
            return null;
        }
        JobParameters result = null;
        PersistedSerializedObject pObj =
                (PersistedSerializedObject) session.getObjectFromGuid(guid);
        if (pObj != null) {
            try {
                Object val = pObj.getSerializableObject(session);
                if (val instanceof JobParameters) {
                    result = (JobParameters) val;
                }
            } catch (SQLException exc) {
                Log.scheduledJobs.error("Error reading parameters", exc);
                result = null;
            } catch (IOException exc) {
                Log.scheduledJobs.error("Error reading parameters", exc);
                result = null;
            } catch (StoreException exc) {
                Log.scheduledJobs.error("Error reading parameters", exc);
                result = null;
            } catch (ClassNotFoundException exc) {
                Log.scheduledJobs.error("Error reading parameters", exc);
                result = null;
            }
        }
        if (result == null) {
            Log.scheduledJobs.warn("Unable to lookup job parameters with guid %s", guid);
        }

        return result;
    }

    public void copy(VersionableObject orig) {
        super.copy(orig);
        ScheduledJob other = (ScheduledJob) orig;
        _name = other._name;
        _jobName = other._jobName;
        _chronSchedule = other._chronSchedule;
        _secondsSchedule = other._secondsSchedule;
        _parametersGuid = other._parametersGuid;
    }

    public void delete(BaseSession session) {
        // todo chrisw - cascade delete to the persisted parameter object
        super.delete(session);
    }

    @UiProperties(displayName = "Name", displayType = UiProperties.TextFieldDisplay, order = 10)
    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    @UiProperties(displayName = "Job Name", displayType = UiProperties.SelectListDisplay, order = 20)
    public String getJobName() {
        return _jobName;
    }

    public void setJobName(String jobName) {
        _jobName = jobName;
    }

    public void setJobNameByClass(Job job) {
        setJobName(job.getClass().getName());
    }

    @UiProperties(displayName = "Chron Schedule", displayType = UiProperties.TextFieldDisplay, order = 40)
    public String getChronSchedule() {
        return _chronSchedule;
    }

    public void setChronSchedule(String chronSchedule) {
        _chronSchedule = chronSchedule;
    }

    @UiProperties(displayName = "Alternate schedule (seconds)", displayType = UiProperties.IntFieldDisplay, order = 50)
    public int getSecondsSchedule() {
        return _secondsSchedule;
    }

    public void setSecondsSchedule(int seconds) {
        _secondsSchedule = seconds;
    }

    /**
     * The soft reference to an object containing the parameters for this job.
     *
     * @return the guid to the PersistedSerializedObject which contains the parameters, or null if no parameters have
     * been set
     */
    public String getParametersGuid() {
        return _parametersGuid;
    }

    public void setParametersGuid(String guid) {
        _parametersGuid = guid;
    }

    /**
     * Kick this job off in the scheduler.
     */
    public void start() {

        try {
            SchedulerService ssrv = (SchedulerService) ServiceContext.getSC().getInitializedModule(SchedulerService.class);
            JVS jvs = new JVS();

            jvs.set(SchedulerService.JobNameKey, _name);
            jvs.set(SchedulerService.JobClassKey, _jobName);
            if (_chronSchedule != null && _chronSchedule.length() > 0) {
                jvs.set(SchedulerService.JobScheduleCron, _chronSchedule);
            } else {
                jvs.set(SchedulerService.JobScheduleSeconds, Integer.toString(_secondsSchedule));
            }
            if (_parametersGuid != null) {
                String key = StringUtil.strcat(SchedulerService.JobPropertiesKey, ".", ParametersGuidKey);
                jvs.set(key, _parametersGuid);
            }
            ssrv.scheduleJob(_name, jvs);
        } catch (PropaccessError propaccessError) {
            propaccessError.printStackTrace();
        }
    }

    /**
     * Run this job once, right now.
     */
    public void runNow() throws PropaccessError {
        SchedulerService ssrv = (SchedulerService) ServiceContext.getSC().getInitializedModule(SchedulerService.class);
        JVS jobProps = new JVS();
        if (_parametersGuid != null) {
            jobProps.set(ParametersGuidKey, _parametersGuid);
        }

        //
        Class jobClass = SchedulerService.getJobClass(_name, jobProps, _jobName);

        // make a special temporary name, so that we don't collide with the regular job name, if it is running already
        String jobName = StringUtil.strcat(_name, "_runOnce");
        ssrv.scheduleJobNow(jobName, jobClass, jobProps);
    }

    /**
     * Make this job stop running.
     */
    public void stop() {
        SchedulerService ssrv = (SchedulerService) ServiceContext.getSC().getInitializedModule(SchedulerService.class);
        ssrv.stopJobNow(_name);
    }

    // ----------------- ListValueSource

    public ListValue[] getValues(Object obj, String fieldName, String tag) {
        if (fieldName.equals("jobName")) {
            if (_classChoices == null) {
                _classChoices = JobService.getService().getRegisteredJobsSelectList();
            }
            return _classChoices;
        } else {
            return super.getValues(obj, fieldName, tag);
        }
    }

    // ----------------- HTSerializable

    public void serialize(HTObjectOutputStream os)
            throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeString(_name);
        os.writeString(_jobName);
        os.writeString(_chronSchedule);
        os.writeInt(_secondsSchedule);
        os.writeString(_parametersGuid);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);

        switch (version) {
            case 1:
                _name = os.readString();
                _jobName = os.readString();
                _chronSchedule = os.readString();
                _secondsSchedule = os.readInt();
                _parametersGuid = os.readString();
        }
    }

    public int getSerializationVersion() {
        return SerializationVersion;
    }

    /**
     * View class enumerating which fields to show when listing.
     */
    @TypeClassMetaInfo(shortTypeName = "ScheduledJobListView",
            isView = true,
            isPersisted = false,
            schemaVersion = ScheduledJob.SerializationVersion)
    public abstract static class ScheduledJobListView {
        public abstract String getName();
    }

    /**
     * View class enumerating which fields to show when editing.
     */
    @TypeClassMetaInfo(shortTypeName = "ScheduledJobEditView",
            isView = true,
            isPersisted = false,
            schemaVersion = ScheduledJob.SerializationVersion)
    public abstract static class ScheduledJobEditView {
        public abstract String getName();

        public abstract String getJobName();

        public abstract String getChronSchedule();

        public abstract int getSecondsSchedule();

        public abstract int getState();
    }
}
