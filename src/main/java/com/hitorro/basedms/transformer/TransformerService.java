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

import com.hitorro.base.objects.PersistedSerializedObject;
import com.hitorro.basedms.job.JobService;
import com.hitorro.basedms.queue.GroupSpacedPSOQueueProcessor;
import com.hitorro.basedms.queue.JobFarmCommand;
import com.hitorro.basedms.transformer.debugcommand.DumpWorkflowTranscodeCurrent;
import com.hitorro.basedms.transformer.debugcommand.DumpWorkflowTranscodeQueue;
import com.hitorro.network.rpc.cluster.ClusterService;
import com.hitorro.util.core.Log;
import com.hitorro.util.json.keys.BooleanProperty;
import com.hitorro.util.json.keys.FileProperty;
import com.hitorro.util.json.keys.IntegerProperty;
import com.hitorro.util.startupframework.phases.ServiceDefinition;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved.
 * <p/>
 * User: chris
 */
@ServiceDefinition(dependentService = {JobService.class},
        shortName = "transformer",
        description = "Transformer service",
        debugCommands = {DumpWorkflowTranscodeQueue.class, DumpWorkflowTranscodeCurrent.class},
        typeManagedClasses = {},
        uiDirectories = {})
public class TransformerService {
    public static final String TransformationKey = "transformer";
    public static final FileProperty TransformerConfig = new FileProperty("transcoder.config",
            "General transcoding config",
            "${HT_BIN}/data/transcoder/edges.csv");
    public static final IntegerProperty JobThreads = new IntegerProperty("transcoder.threads", "Number of threads", 2);
    public static final BooleanProperty DisableJobService = new BooleanProperty("transcoder.disablequeue", "", false);
    public static final String PSO_JOB_NAME = "jobs";
    private static TransformerService s_service;
    private ConvertionContext m_convertionContext = new ConvertionContext();
    private Map<String, TransformMethod> m_methods = new HashMap<String, TransformMethod>();
    private boolean _initialized = false;
    private boolean _started = false;

    private boolean m_startJobQueue = true;
    private int m_queueLength = 40;
    //private int m_threads = 4;
    private int m_threads = JobThreads.apply();

    public static TransformerService getService() {
        return s_service;
    }

    private void startJobQueue() {
        if (m_startJobQueue) {

            JobFarmCommand jfc = new JobFarmCommand();
            // create a queue just for transcoder
            GroupSpacedPSOQueueProcessor<PersistedSerializedObject> httpQp = new GroupSpacedPSOQueueProcessor<PersistedSerializedObject>(JobService.TranscoderKey, "PSO-TranscoderService",
                    80, m_threads, jfc,
                    PersistedSerializedObject.CollectionID_TranscoderQueue);
            httpQp.addNames(PSO_JOB_NAME);
            httpQp.start();
        }
    }

    public TransformMethod getMethod(String method) {
        return m_methods.get(method.toLowerCase());
    }

    public void setMethod(TransformMethod method) {
        m_methods.put(method.getMethodName().toLowerCase(), method);
    }

    public ConvertionContext getConvertionContext() {
        return m_convertionContext;
    }

    public String init(boolean dbInit, final boolean upgrading, final long currentVersion, final long targetVersion) {
        try {
            m_convertionContext.loadContext(TransformerConfig.apply());
            JobService.getService().registerAppJob(TransformJob.class, "Transformation Job", TransformJobParameters.class);
            s_service = this;
            ClusterService.getThisInstanceDefinition().addInstanceCapability(TransformationKey, "", "", "", true);
        } catch (IOException e) {
            Log.util.error("%s %e", e, e);
            return e.getMessage();
        }
        return null;
    }

    public String start(boolean dbInit) {
        startJobQueue();
        return null;
    }

    public String deInit() {
        return null;
    }
}
