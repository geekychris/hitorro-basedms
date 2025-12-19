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

import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.base.objects.PersistedSerializedObject;
import com.hitorro.basedms.job.JobService;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.network.rpc.cluster.ClusterService;
import com.hitorro.util.core.Console;
import com.hitorro.util.core.Env;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.UTCDateUtil;
import com.hitorro.util.core.queue.ThreadedQueueCanceledException;
import com.hitorro.util.core.queue.ThreadedQueueTimeoutException;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.core.thread.RestartableService;
import com.hitorro.util.core.thread.RestartableServiceDaemon;
import com.hitorro.util.core.thread.farm.FarmCommand;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.job.JobExecutionResult;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.BaseSessionFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Jan 25, 2005 Time: 10:34:05 AM
 * <p/>
 * The PersistedSerializedObject can be used as a typed work queue.
 * <p/>
 * One can create one of these queues, providing a few paramters to define queue length, how many threads, what kind of
 * processing to do.  Further you provide the collection id and the name so the queue knows what to query.  You put the
 * names via the "addName" method.
 * <p/>
 * If you are using this queue in a cluster of nodes sharing the same db, you can make a specific instance of the queue
 * to only work on a segment of the queue.  This is done through simple modulo arithmetic.  One provides the amount of
 * nodes (say 4) and the id of the node you want to represent.  The key used here for input is the content_id column.
 * Once you do this you will get 1 in n rows(where n is the amount of nodes)
 * <p/>
 * <p/>
 * <p/>
 * T is the type of thread data (such as a HTMLFetcher.
 */
public class PSOQueueProcessor<P extends PersistedSerializedObject> extends TransactableQueue<P, P>
        implements Runnable {
    public int m_waitSecondsNoResult = 30;
    public int m_waitSecondsAfterResults = 1;
    private String m_preparedStatment;
    private int m_collectionId = PersistedSerializedObject.CollectionID_Queue;
    private List<String> m_names = new ArrayList<String>();

    private boolean m_hasMod = false;
    private int m_mod;
    private int[] m_modValues;

    private String m_query;

    private boolean m_enabled = true;
    private int currWait = 1;
    private int maxWait = 30;
    private boolean m_orderByMod = false;
    private boolean m_orderByPriority = false;
    private int modSize;

    public PSOQueueProcessor(String name, String groupName, int workQueueLength, int workerThreadCount,
                             FarmCommand<JobQueueElement<P>, JobQueueElement<P>, Object> command) {
        super(name, groupName, workQueueLength, workerThreadCount, command);
        initQueueTracker(name);
    }

    public PSOQueueProcessor(String name, String groupName, int workQueueLength, int workerThreadCount,
                             FarmCommand<JobQueueElement<P>, JobQueueElement<P>, Object> command,
                             int collectionId) {

        super(name, groupName, workQueueLength, workerThreadCount, command);
        m_collectionId = collectionId;
        initQueueTracker(name);
    }

    public int getCollectionId() {
        return m_collectionId;
    }

    public void setCollectionId(int collectionId) {
        m_collectionId = collectionId;
    }

    public void disable() {
        m_enabled = false;
        purgeInQueue();

    }

    public void enable() {
        m_enabled = true;

    }

    private void initQueueTracker(final String name) {
        JobService.addQueue(this);

        ClusterService.getThisInstanceDefinition().addInstanceCapability(name, "", "", "", true);
        if (ClusterService.getService().isMemberOfDBGroup()) {
            // as we initialize, we must set ourselfs up to be disable until we are told otherwise.
            disable();
        }
    }

    public void dumpStats(String message) {
        Console.println("%s InputQueueSize %s, OutputQueueSize: %s", message, this.m_inQueue.size(), this.m_outputQueue.size());
    }

    public void elementComplete(String guid, JobQueueElement<P> elem) {
        JobExecutionResult jer = elem.getResult();
        PersistedSerializedObject pso = elem.getPayload();
        Log.queue.debug("Completed element with group id of %s", elem.getGroupId());

        QueueUtil.actionComplete(jer, pso, guid);

    }

    public void elementRetry(String guid, JobQueueElement<P> elem, int minutesRetry) {
        BaseSession session = BaseSessionFactory.getFactory().getSession();
        try {
            JobExecutionResult result = elem.getResult();
            PersistedSerializedObject pso = elem.getPayload();
            try {
                pso.setSerializableObject(result.getJobParameters());
            } catch (IOException e) {
                Log.util.error("%s %e", e, e);
            } catch (StoreException e) {
                Log.util.error("%s %e", e, e);
            }
            pso.setEffectiveFrom(UTCDateUtil.getDateForNMinutesFromNow(minutesRetry));
            session.saveOrUpdate(pso);
        } finally {
            DMSSessionFactory.getFactory().commitAndCloseSession(session);
        }
    }

    public String getName() {
        return m_name;
    }

    public String getNames() {
        return StringUtil.mergeWithPrefixAndJoinToken(m_names, "", ", ");
    }

    public void addNames(String name) {
        m_names.add(name);
    }

    /**
     * If called, causes the query fired only to look for
     *
     * @param mod
     * @param modValue
     */
    public void setContentIdHashFunction(int mod, int modValue) {
        m_hasMod = true;
        m_mod = mod;
        m_modValues = new int[1];
        m_modValues[0] = modValue;
        m_query = null;
    }

    public void setModValues(int[] ints, int mod) {
        m_modValues = ints;
        m_mod = mod;
        m_query = null;
    }

    public int getQueueSize() {
        return this.m_inQueue.size();
    }

    public int poll() {
        BaseSession session = null;
        BaseSession fetchSession = null;
        int addCount = 0;
        try {
            long time = System.currentTimeMillis();
            addCount = flush(time);


            if (!hasCapacity()) {
                return addCount;
            }
            String query = getPreparedStatement();
            if (query == null) {
                Log.queue.error("Unable to perform Query Poller, no event types provided");
                return addCount;
            }
            session = BaseSessionFactory.getFactory().getSession();
            fetchSession = BaseSessionFactory.getFactory().getSession();

            Iterator<String> iter = session.getIteratorFromQueryArgs(query, new Date());

            while (iter.hasNext() && hasMaxCapacity() && m_enabled) {
                String guid = iter.next();
                time = System.currentTimeMillis();
                if (!hasGuidInQueue(guid)) {
                    addCount += addToQueue(addCount, guid, fetchSession, time);
                }
            }

        } catch (ThreadedQueueTimeoutException e) {
            Log.queue.error("Timed out performing threaded queue operation %s %e", e, e);
        } catch (org.hibernate.query.SemanticException se) {
            Log.util.error("%s %e", se, se);
        } catch (ThreadedQueueCanceledException e) {
            Log.util.error("%s %e", e, e);
        } finally {
            DMSSessionFactory.getFactory().rollbackCloseSession(session);
            DMSSessionFactory.getFactory().rollbackCloseSession(fetchSession);
        }

        return addCount;
    }

    protected int flush(long time) throws ThreadedQueueTimeoutException, ThreadedQueueCanceledException {
        return 0;
    }

    public int addToQueue(int addCount, String guid, BaseSession fetchSession, long time)
            throws ThreadedQueueCanceledException, ThreadedQueueTimeoutException {
        addCount++;

        P pso = (P) fetchSession.getHTSerializableFromGUID(guid);
        if (pso != null) {
            // why could it be null?  because we can delete items and we may loose the item underneath us
            JobQueueElement<P> jqe = new JobQueueElement<P>(pso, pso.getGuid());
            addGuid(pso.getGuid(), jqe);
            addToInQueue(jqe, time);
        }
        fetchSession.rollback();
        return addCount;
    }

    protected void addToInQueue(JobQueueElement<P> jqe, long time) throws ThreadedQueueCanceledException, ThreadedQueueTimeoutException {
        try {
            this.m_inQueue.put(jqe);
        } catch (InterruptedException e) {

        }
    }

    @Override
    public boolean init(final JsonNode node) {
        return true;
    }

    /**
     * Register self with Restartable Daemon
     */
    public boolean start() {
        m_rs = new RestartableService(m_name, m_group, 100, this, true);
        RestartableServiceDaemon.addService(m_rs);
        return true;
    }

    @Override
    public boolean add(final JobQueueElement<P> o) throws IOException, StoreException {
        return false;
    }

    @Override
    public boolean stop() throws IOException {
        return false;
    }

    public void run() {

        while (true) {
            while (!m_enabled) {
                // check every 15 seconds if we are not enabled.
                Env.sleepNSeconds(15);
            }
            int count = poll();
            if (count == 0) {
                currWait = currWait * 2;
                if (currWait > maxWait) {
                    currWait = maxWait;
                }
                Log.httpfetcher.debug("Waiting %s as there was no results from poll (col = %s)", currWait, m_collectionId);
                Env.sleepNSeconds(currWait);
            } else {
                currWait = 1;
                Log.httpfetcher.debug("Waiting count(%s) %sseconds as there were results from poll (col = %s)", count, currWait, m_collectionId);
                Env.sleepNSeconds(currWait);
            }
        }
    }

    public void setOrderByMod(int modSize) {
        this.modSize = modSize;
        m_orderByMod = true;
    }

    public void setOrderByPriority() {
        m_orderByPriority = true;
    }

    private String getPreparedStatement() {
        if (m_query != null) {
            return m_query;
        }
        if (m_names.size() == 0) {
            return null;
        }
        if (StringUtil.nullOrEmptyOrBlankString(m_preparedStatment)) {
            StringBuilder b = new StringBuilder();
            //'2007-01-25 22:14:50'
            Console.bprint(b, "select guid from PersistedSerializedObject as entry where collectionId=%s", m_collectionId);

            //Console.bprint(b, "select entry from PersistedSerializedObject as entry where ");
            //Console.bprint(b, " collectionId=%s", m_collectionId);


            if (m_names.size() > 1) {
                Console.bprint(b, " and (");
                for (String name : m_names) {
                    Console.bprint(b, " name = '%s'", name);
                }
                Console.bprint(b, ")");

            } else {
                Console.bprint(b, " and name = '%s'", m_names.get(0));
            }

            Console.bprint(b, " and effectiveFrom < :a");
            if (m_hasMod) {
                if (m_modValues != null && m_modValues.length > 0) {
                    Console.bprint(b, " and (");
                    boolean first = true;
                    for (Integer i : m_modValues) {
                        if (first == false) {
                            Console.bprint(b, " and ");
                        }
                        //Console.bprint(b, "mod(content_id, %s) = %s", m_mod, i);
                        first = false;
                    }
                    Console.bprint(b, " ) ");
                }
            }

            if (m_orderByMod) {
                //Console.bprint(b, " order by priority, mod(content_id, %s)", modSize);
            } else if (m_orderByPriority) {
                Console.bprint(b, " order by priority desc");
            }
            //Console.bprint(b, " and effectiveFrom < '2007-01-25 22:14:50'");

            m_preparedStatment = b.toString();
        }
        return m_preparedStatment;
    }
}
