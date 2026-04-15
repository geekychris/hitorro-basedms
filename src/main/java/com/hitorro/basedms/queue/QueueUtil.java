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
import com.hitorro.basedms.PostFromUrlParameters;
import com.hitorro.basedms.html.HTMLFetcherAppJobParameters;
import com.hitorro.basedms.job.JobService;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.job.JobExecutionResult;
import com.hitorro.util.job.JobParameters;
import com.hitorro.util.typesystem.BaseSession;
import org.hibernate.StaleStateException;

import java.io.IOException;
import java.util.Date;

/**
 * <p/>
 * Set of utility methods
 */
public class QueueUtil {


    public static void enqueueHtmlForContentAddition(String url, int retries, String targetGuid, String contentLabel, int priority) {
        BaseSession session = DMSSessionFactory.getFactory().getSession();

        try {
            enqueueHtmlForContentAddition(url, retries, targetGuid, contentLabel, session, priority);
        } finally {
            DMSSessionFactory.getFactory().rollbackCloseSession(session);
        }
    }

    public static PersistedSerializedObject enqueueUrlFetchPostGenerator(BaseSession session,
                                                                               String url,
                                                                               int retries,
                                                                               String parentGuid,
                                                                               String containerGuid,
                                                                               int parentDegree,
                                                                               int parentType,
                                                                               int priority,
                                                                               String guid, boolean commit) {
        try {
            PostFromUrlParameters p = new PostFromUrlParameters();
            p.setParentGuid(parentGuid);
            p.setContainerGuid(containerGuid);
            p.setRetries(retries);
            p.setType(parentType);
            p.setReadUrl(url);
            p.setDegree(parentDegree);
            p.setGuid(guid);
            return enqueJob(p, session, commit, PersistedSerializedObject.CollectionID_HTMLQueue, priority);
        } finally {
            if (commit) {
                DMSSessionFactory.getFactory().rollbackCloseSession(session);
            }
        }
    }

    public static PersistedSerializedObject enqueueHtmlForContentAddition(String url,
                                                                                int retries,
                                                                                String targetGuid,
                                                                                String contentLabel,
                                                                                BaseSession session, int priority) {
        if (StringUtil.nullOrEmptyOrBlankString(url)) {
            return null;
        }
        HTMLFetcherAppJobParameters p = new HTMLFetcherAppJobParameters();
        p.setTargetGuid(targetGuid);
        p.setRetries(retries);
        p.setLabel(contentLabel);
        p.setReadUrl(url);
        return enqueJob(p, session, true, PersistedSerializedObject.CollectionID_Queue, priority);
    }

    public static PersistedSerializedObject enqueJob(JobParameters p, BaseSession session, boolean commit, int id, int priority) {
        return enqueJob(p, session, commit, id, null, priority);
    }

    public static PersistedSerializedObject enqueJob(JobParameters p,
                                                           BaseSession session,
                                                           boolean commit,
                                                           int id,
                                                           String executor,
                                                           int priority) {
        PersistedSerializedObject pso = new PersistedSerializedObject();

        pso.setEffectiveFrom(new Date());

        pso.setExecutor(executor);
        pso.setName(JobService.PSO_JOB_NAME);
        pso.setPriority(priority);
        try {
            pso.setSerializableObject(p, session);  // Pass session explicitly
        } catch (IOException e) {
            Log.util.error("%s %e", e, e);
            return null;
        } catch (StoreException e) {
            Log.util.error("%s %e", e, e);
            return null;
        }
        pso.setCollectionId(id);
        session.persist(pso);
        if (commit) {
            session.commit();
        }
        return pso;
    }

    /**
     * Given a job result and its pso that it came from, decide how to flushToDisk or delete it to the PSO table.
     *
     * @param jer
     * @param pso
     * @param guid
     * @return
     */
    public static boolean actionComplete(JobExecutionResult jer, PersistedSerializedObject pso, String guid) {
        BaseSession session = DMSSessionFactory.getFactory().getSession();
        try {
            if (jer.getCompleteWithRewrite()) {
                pso.setEffectiveFrom(new Date());
                pso.setName(jer.getNewEventName());
                pso.setCollectionId(jer.getCollectionID());
                session.saveOrUpdate(pso);
            } else {
                session.refresh(pso);
                session.delete(pso);
            }
        } finally {

            try {
                DMSSessionFactory.getFactory().commitAndCloseSession(session);
            } catch (StaleStateException sse) {
                // commit failed, roll it back.
                DMSSessionFactory.getFactory().rollbackCloseSession(session);
                Log.queue.error("Stale State exception for guid: %s error %s", guid, sse);
                return false;
            }
        }
        return true;
    }
}
