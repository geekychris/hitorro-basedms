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

import com.hitorro.util.core.Log;
import com.hitorro.util.core.iterator.queue.AbstractEnqueue;
import com.hitorro.util.core.iterator.sinks.Sink;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.thread.EnhancedThreadGroup;
import com.hitorro.util.core.thread.RestartableService;
import com.hitorro.util.core.thread.farm.Farm;
import com.hitorro.util.core.thread.farm.FarmCommand;
import com.hitorro.util.core.thread.farm.FarmSink;
import com.hitorro.util.job.JobExecutionResult;
import org.apache.log4j.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Queue that determines completeness of a queue element / need to re-insert for later
 * retry or complete failure
 */

/**
 * The put takes whatever was processed and determines what to do with it....should it be
 */
abstract class TransactableQueue<T, C> implements Sink<JobQueueElement<T>> {

    protected int m_workQueueLength;
    protected int m_workerThreadCount;
    protected AbstractEnqueue<JobQueueElement<T>> m_inQueue;
    protected AbstractEnqueue<JobQueueElement<T>> m_outputQueue;
    protected Farm<JobQueueElement<T>, JobQueueElement<T>, Object> m_farm;
    protected FarmSink m_sink;
    protected EnhancedThreadGroup m_threadGroup;
    protected RestartableService m_rs;
    protected String m_name;
    protected String m_group;
    private Map<String, JobQueueElement<T>> m_outstandingGuids = new HashMap<String, JobQueueElement<T>>();

    public TransactableQueue(String name, String group, int workQueueLength, int workerThreadCount,
                             FarmCommand<JobQueueElement<T>, JobQueueElement<T>, Object> command) {
        m_name = name;
        m_group = group;
        m_workQueueLength = workQueueLength;
        m_workerThreadCount = workerThreadCount;
        m_inQueue = AbstractEnqueue.arrayBlocking(m_workQueueLength);
        m_outputQueue = AbstractEnqueue.arrayBlocking(m_workQueueLength);
        m_threadGroup = new EnhancedThreadGroup(group);
        m_farm = new Farm<JobQueueElement<T>, JobQueueElement<T>, Object>(name, m_threadGroup,
                m_inQueue,
                m_outputQueue, command, workerThreadCount);


        m_sink = new FarmSink(Fmt.S("%s-put", m_name), m_threadGroup, m_outputQueue, this);
        m_farm.useKeepAliveThread(1000);
        m_farm.start();
        m_sink.start();
    }

    public int getInQueueCount() {
        return m_inQueue.size();
    }

    public void purgeInQueue() {
        if (m_inQueue != null) {
            m_inQueue.clear();
        }
    }

    public int getOutQueueCount() {
        return m_outputQueue.size();
    }

    public int getWorkerThreadCount() {
        return m_workerThreadCount;
    }

    public abstract void elementComplete(String guid, JobQueueElement<T> elem);

    public abstract void elementRetry(String guid, JobQueueElement<T> elem, int minutesRetry);

    protected boolean hasCapacity() {
        return m_inQueue.remainingCapacity() != 0;
    }

    protected boolean hasMaxCapacity() {
        return m_inQueue.remainingCapacity() != 0;
    }


    protected synchronized void removeGuidFromBookKeeping(JobQueueElement<T> elem) {
        m_outstandingGuids.remove(elem.getGuid());
    }

    protected synchronized void addGuid(String guid, JobQueueElement<T> elem) {
        m_outstandingGuids.put(guid, elem);
    }

    protected boolean hasGuidInQueue(String guid) {
        return m_outstandingGuids.get(guid) != null;
    }

    public boolean consume(JobQueueElement<T> object) {
        JobExecutionResult result = object.getResult();
        if (result.getErrorLevel() == Level.INFO && !result.shouldRetry()) {
            // things are good, we can remove the item.
            elementComplete(object.getGuid(), object);
        } else {
            if (result.shouldRetry()) {
                // we should retry
                elementRetry(object.getGuid(), object, result.getRetryMinutes());
            } else {
                // things are good, we can remove the item.
                elementComplete(object.getGuid(), object);
                result.applyMessageToLogger(Log.util);

            }
        }
        removeGuidFromBookKeeping(object);
        return false;
    }

    public void close() {
    }
}


