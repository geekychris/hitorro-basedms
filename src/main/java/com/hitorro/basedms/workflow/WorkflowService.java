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
package com.hitorro.basedms.workflow;

import com.hitorro.base.objects.PersistedSerializedObject;
import com.hitorro.basedms.job.JobService;
import com.hitorro.basedms.queue.GroupSpacedPSOQueueProcessor;
import com.hitorro.basedms.statemachine.NotifyJobCommand;
import com.hitorro.basedms.workflow.debugcommand.WorkflowItemSetState;
import com.hitorro.util.core.thread.RestartableService;
import com.hitorro.util.core.thread.RestartableServiceDaemon;
import com.hitorro.util.json.keys.BooleanProperty;
import com.hitorro.util.json.keys.IntegerProperty;
import com.hitorro.util.startupframework.phases.ServiceDefinition;
import com.hitorro.util.statemachine.StateMachineService;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved.
 * <p/>
 * User: chris
 */
@ServiceDefinition(dependentService = {StateMachineService.class},
        shortName = "workflow",
        description = "Simple workflow service",
        debugCommands = {WorkflowItemSetState.class},
        typeManagedClasses = {WorkFlowItem.class, WorkFlowItemEntry.class, OutstandingWorkflowItem.class},
        uiDirectories = {})
public class WorkflowService {
    public static final String PSO_JOB_NAME = "notify";

    public static final IntegerProperty JobThreads = new IntegerProperty("workflow.threads", "Number of threads", 1);
    public static final BooleanProperty DisableJobService = new BooleanProperty("workflow.disablequeue", "", false);

    private static WorkflowService s_service;

    public static WorkflowService getService() {
        return s_service;
    }

    public String init(boolean dbInit, final boolean upgrading, final long currentVersion, final long targetVersion) {
        s_service = this;
        NotifyJobCommand jfc = new NotifyJobCommand();
        // create a queue just for workflows
        GroupSpacedPSOQueueProcessor<PersistedSerializedObject> workflow = new GroupSpacedPSOQueueProcessor<PersistedSerializedObject>(JobService.WorkflowKey, "PSO-NotifierService",
                80, JobThreads.apply(), jfc,
                PersistedSerializedObject.CollectionID_NotificationQueue);
        workflow.addNames(PSO_JOB_NAME);
        workflow.start();

        // start a poller to look for dormant wfi's
        RestartableService rs = new RestartableService("DormontWorkflow", "DormontWorkflow", 100, new DormontWorkflowItemTickler(), true);
        RestartableServiceDaemon.addService(rs);

        return null;
    }

    public String start(boolean dbInit) {
        return null;
    }

    public String deInit() {
        return null;
    }
}
