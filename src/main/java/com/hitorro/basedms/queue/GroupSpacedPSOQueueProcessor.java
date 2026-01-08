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
import com.hitorro.util.core.Constants;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.map.LRUHashMap;
import com.hitorro.util.core.queue.ThreadedQueueCanceledException;
import com.hitorro.util.core.queue.ThreadedQueueTimeoutException;
import com.hitorro.util.core.thread.farm.FarmCommand;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * <p/>
 * Takes the PSOQueueProcessor one step further by not placing onto the input queue any element that has the same site
 * id as something that we recently put on the queue.
 */
public class GroupSpacedPSOQueueProcessor<P extends PersistedSerializedObject> extends PSOQueueProcessor<P> {
    public static final int QueueSize = 1024 * 5;
    private LRUHashMap<Object, InOutTime> m_sites = new LRUHashMap<Object, InOutTime>(QueueSize);
    private List<JobQueueElement<P>> m_holdBack = new LinkedList<JobQueueElement<P>>();

    // hold back 5 secs.
    private long m_holdBackTime = Constants.MillisInSecond / 4;

    // how many things to hold in back before we choke the real queue.
    private long m_maxHoldbackSize = 8000;
    private double m_refillThreshold = 0.5;

    public GroupSpacedPSOQueueProcessor(String name, String groupName, int workQueueLength, int workerThreadCount, FarmCommand<JobQueueElement<P>, JobQueueElement<P>, Object> farmCommand) {
        super(name, groupName, workQueueLength, workerThreadCount, farmCommand);
    }

    public GroupSpacedPSOQueueProcessor(String name, String groupName, int workQueueLength, int workerThreadCount, FarmCommand<JobQueueElement<P>, JobQueueElement<P>, Object> farmCommand, int collectionId) {
        super(name, groupName, workQueueLength, workerThreadCount, farmCommand, collectionId);
    }

    public int getDeferredQueueCount() {
        return m_holdBack.size();
    }

    public void disable() {
        super.disable();
        if (m_sites != null) {
            m_sites.clear();
        }
        if (m_holdBack != null) {
            m_holdBack.clear();
        }
    }

    protected boolean hasCapacity() {
        if (m_inQueue.isFull()) {
            return false;
        }
        double min = m_maxHoldbackSize * m_refillThreshold;
        return m_holdBack.size() <= min;
    }

    protected boolean hasMaxCapacity() {
        if (m_inQueue.isFull()) {
            return false;
        }
        return m_holdBack.size() <= m_maxHoldbackSize;
    }

    public int getQueueSize() {
        if (m_holdBack.size() > m_maxHoldbackSize) {
            return super.getQueueSize() + m_holdBack.size();
        }
        return super.getQueueSize();
    }

    /**
     * gets called before we do a poll.  This is our chance to see if we have held back any elements in our holdback
     * listFiles that can be moved forward.
     */
    protected int flush(long time) throws ThreadedQueueTimeoutException, ThreadedQueueCanceledException {
        int count = 0;
        Iterator<JobQueueElement<P>> iter = m_holdBack.iterator();
        int c = 0;
        while (iter.hasNext() && super.hasCapacity()) {
            c++;
            if (c % 100 == 0) {
                // update the time every 100 elements...is this important?
                time = System.currentTimeMillis();
            }
            //we dont care about checking if the queue is full, we can block.
            JobQueueElement<P> item = iter.next();
            Object group = item.getGroupId();
            InOutTime iot = m_sites.get(group);

            if (iot == null) {
                // we dont have this site in our site mask
                iot = new InOutTime(time);
                addToRealInQueueAndUpdateIOT(iot, time, item, iter, group, true);
                count++;

            } else if (iot.m_in == false && iot.m_time + m_holdBackTime < time) {
                // held back long enoughy
                addToRealInQueueAndUpdateIOT(iot, time, item, iter, group, false);
                count++;
            } else {
                // do nothing with this row....we have you already in our listFiles.
                //Log.util.info("Site (%s) in listFiles...", group);
            }

        }
        return count;

    }

    private void addToRealInQueueAndUpdateIOT(InOutTime iot, long time, JobQueueElement<P> item, Iterator<JobQueueElement<P>> iter, Object group, boolean add) throws ThreadedQueueCanceledException, ThreadedQueueTimeoutException {
        if (add) {
            m_sites.put(group, iot);
            Log.util.debug("Site (%s) not in listFiles adding...(%s) this queue %s", group, this.m_inQueue.size(), m_holdBack.size());
        } else {
            Log.util.debug("Site (%s)was in listFiles but out of date, refreshing.... (%s) ", group, this.m_inQueue.size(), m_holdBack.size());
        }
        iot.m_in = true;
        iot.m_time = time;
        // we are back in and refreshed (assume that lru refreshes on get.
        super.addToInQueue(item, time);
        // remove that element now (does require that this LinkedList supports remove)
        if (iter != null) {
            iter.remove();
        }

    }

    /**
     * we have determined we dont have this guid in the queue ANYWHERE...put it to either the in queue or the holdback
     *
     * @param jqe
     * @throws ThreadedQueueCanceledException
     * @throws com.hitorro.util.core.queue.ThreadedQueueTimeoutException
     */
    public void addToInQueue(JobQueueElement<P> jqe, long time) throws ThreadedQueueCanceledException, ThreadedQueueTimeoutException {
        Object group = jqe.getGroupId();
        InOutTime iot = m_sites.get(group);
        if (iot == null) {
            // we dont have this site in our site mask
            iot = new InOutTime(time);
            addToRealInQueueAndUpdateIOT(iot, time, jqe, null, group, true);

        } else if (iot.m_in == false && iot.m_time + m_holdBackTime < time) {
            addToRealInQueueAndUpdateIOT(iot, time, jqe, null, group, false);
        } else {
            // hold back for a later time
            m_holdBack.add(jqe);
        }
    }

    protected synchronized void removeGuidFromBookKeeping(JobQueueElement<P> elem) {
        Object group = elem.getGroup();
        InOutTime iot = m_sites.get(group);
        if (iot != null) {
            iot.m_time = System.currentTimeMillis();
            iot.m_in = false;
        } else {
            // something is wrong.
            Log.queue.error("Expected an IOT to be in the sites listFiles.");
        }
        super.removeGuidFromBookKeeping(elem);
    }

}

class InOutTime {
    public long m_time;
    boolean m_in;

    public InOutTime(long time) {
        m_time = time;
        m_in = true;
    }
}
