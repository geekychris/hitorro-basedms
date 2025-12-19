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
package com.hitorro.basedms;

import com.hitorro.basedms.cache.ContentTypeCache;
import com.hitorro.basedms.job.JobService;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.Audit;
import com.hitorro.util.core.Audit.AuditStatus;
import com.hitorro.util.core.Env;
import com.hitorro.util.core.ListUtil;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.job.Job;
import com.hitorro.util.job.JobExecutionResult;
import com.hitorro.util.job.JobParameters;
import com.hitorro.util.job.JobRegistration;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTSerializable;
import com.hitorro.util.typesystem.HTSerializableNotify;
import org.apache.log4j.Level;
import org.hibernate.query.Query;
import org.quartz.JobExecutionException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved.
 * <p/>
 * User: chris
 */
public class BaseUtil {
    public static final long TypeBits = 0xFFFL << 52;
    public static final long Instancebits = 0xFFFL << 40;
    public static final long IDBits = 0x000000FFFFFFFFFFL;

    /**
     * Guid consists of:
     * <p/>
     * bits 0-xx Id            (5 bytes 2 ^ 40 bits 0 - 39) xx+1 - yy instance (1.5 bytes 2 ^ 12 bits 40 - 51) yy+1 - 63
     * type     (1.5 bytes 2 ^ 12 bits 52 - 63)
     *
     * @param guid
     * @return
     */
    public final static long getLongFromGuid(String guid) {
        String parts[] = StringUtil.tokenizeFromSingleChar(guid, ":");
        if (parts == null || parts.length != 3) {
            return -1;
        }
        long type = Long.parseLong(parts[0], 16);
        long instance = Long.parseLong(parts[1]);
        long id = Long.parseLong(parts[2]);
        long x = (type << 52) & TypeBits;
        long y = (instance << 40) & Instancebits;
        long z = id & IDBits;
        return (x | y | z);
    }

    public final static String getGuidFromLong(long id) {
        return getGuidFromParts((int) ((id & TypeBits) >> 52), (int) ((id & Instancebits) >> 40), ((id & IDBits)));
    }

    public final static String getGuidFromParts(int type, int instance, long id) {
        return StringUtil.strcat(Integer.toString(type, 16).toUpperCase(), ":", Integer.toString(instance), ":", Long.toString(id));
    }

    /**
     * Executed a job, and notify.  Once complete delete the PSO.  Once you get the JobExecutionREsult use the
     * getErrorLevel.  If its level is ERROR or worse then it failed and you probably want to rollback.
     *
     * @param session
     * @param pso
     * @return
     * @throws SQLException
     * @throws IOException
     * @throws com.hitorro.util.io.StoreException
     * @throws ClassNotFoundException
     */
    public static JobExecutionResult executeJob(BaseSession session, com.hitorro.base.objects.PersistedSerializedObject pso)
            throws SQLException, IOException, StoreException, ClassNotFoundException {
        JobExecutionResult result = null;
        Object o = pso.getObject();
        if (!(o instanceof JobParameters)) {
            return new JobExecutionResult(Level.ERROR, "JobParameters not provided");
        }

        JobParameters parameters = (JobParameters) o;

        JobRegistration registration = JobService.getService().getAppJobRegistrationByName(parameters.getJobName());
        if (registration == null) {
            Log.util.error("Unable to find a registered job with job name %s", parameters.getJobName());
        }
        Job appJob = registration.getAppJob();

        if (appJob == null) {
            return new JobExecutionResult(Level.ERROR, "AppJob %s not found", parameters.getJobName());
        }

        try {
            appJob.setSession(session);

            //   audit-trail, pre-execution
            String ipAddress = Env.getHostIP();
            String userName = ((DMSSession) session).getEffectiveUser();
            Audit.audit(ipAddress, userName, Audit.AdministrateTopic, AuditStatus.InProgress, "execute ScheduledJob: %s", parameters.getJobName());

            //   execute and notify success or failure
            result = appJob.doAction(parameters);
            BaseUtil.notifyFromParameters(session, parameters, true, pso);

            //   audit-trail, post-execution
            boolean status = result.getErrorLevel() == Level.INFO && !result.shouldRetry();
            Audit.audit(ipAddress, userName, Audit.AdministrateTopic, AuditStatus.getStatus(status), "execute ScheduledJob: %s, with message: %s",
                    parameters.getJobName(),
                    result.getMessage());


        } catch (JobExecutionException exc) {
            String msg = Fmt.S("Error running job %s: ", parameters.getJobName());
            Log.util.error(msg, exc);

            return new JobExecutionResult(Level.ERROR, "Exception running job AppJob %s not found", exc.getMessage());
        }
        return new JobExecutionResult(Level.INFO, "Done");
    }

    /**
     * Given a PSO that contains a Job Parameters, notify any object that is waiting for notification post job event.
     * This method is called in two scenarios.  Firstly for the PSO Transactable queue.  The job is complete and we have
     * something to notify.  In this mode we notify and commit whatever happens post success of the notify.  We do not
     * delete the PSO as thats done by the queue.
     * <p/>
     * in the second scenario, it is called by an app step when a parameters object has "made a decision".  Here we may
     * want to delete the pso in the same transaction.
     * <p/>
     * The third case is in the scheduler that also manages jobs and we will want to perform a notify on completion of a
     * scheduled task.
     *
     * @param pso
     * @param deletePSO
     * @return
     */
    public static final JobExecutionResult notify(BaseSession session,
                                                  com.hitorro.base.objects.PersistedSerializedObject pso,
                                                  boolean deletePSO) {

        JobParameters params = null;
        try {
            HTSerializable j = pso.getObject();
            if (j == null) {
                Object[] args = new Object[]{};
                return new JobExecutionResult(Level.ERROR, "PSO element does not have a serialized Object", args);
            }
            if (j instanceof JobParameters) {
                params = (JobParameters) j;
            } else {

                Object[] args = new Object[]{j.getClass().getCanonicalName()};
                return new JobExecutionResult(Level.ERROR, "PSO is not a JobParameters object, its a: %s", args);
            }
        } catch (SQLException e) {
            Object[] args = new Object[]{e};
            return new JobExecutionResult(Level.ERROR, "SqlException reconstituting job parameters %s", args);
        } catch (IOException e) {
            Object[] args = new Object[]{e};
            return new JobExecutionResult(Level.ERROR, "IOException reconstituting job parameters %s", args);
        } catch (StoreException e) {
            Object[] args = new Object[]{e};
            return new JobExecutionResult(Level.ERROR, "StoreException reconstituting job parameters %s", args);
        } catch (ClassNotFoundException e) {
            Object[] args = new Object[]{e};
            return new JobExecutionResult(Level.ERROR, "ClassNotFoundException reconstituting job parameters %s", args);
        }

        return notifyFromParameters(session, params, deletePSO, pso);
    }

    /**
     * Given just a parameters object, attempt to notify a notifiable IF there is one referenced.
     *
     * @param params
     * @return
     */
    public static JobExecutionResult notifyFromParameters(JobParameters params) {
        if (params == null) {
            return null;
        }
        BaseSession session = DMSSessionFactory.getFactory().getSession();
        try {
            return notifyFromParameters(session, params, false, null);
        } finally {
            session.commit();
            DMSSessionFactory.closeSession(session);
        }
    }

    private static JobExecutionResult notifyFromParameters(BaseSession session, JobParameters params,
                                                           boolean deletePSO,
                                                           com.hitorro.base.objects.PersistedSerializedObject pso) {
        String guid = params.getNotifyGuid();
        boolean result = false;
        try {


            HTSerializable pts = session.getObjectFromGuid(guid);
            if (pts instanceof HTSerializableNotify) {
                result = ((HTSerializableNotify) pts).notify(params, session);
                // on success we always commit the pts and delete the PSO.
            }
        } finally {
            if (session != null) {
                // any work that the session wanted committed should be committed in the action
                // rollback anything left over

                if (result) {
                    if (deletePSO) {
                        // only for the case we are not being called from a persisted queue....which
                        // takes care of the delete itself.
                        if (session.refresh(pso) != null) {
                            session.delete(pso);
                        }
                    } else {
                        // dont want to get in a loop.
                        params.setNotifyGuid(null);
                        params.setNotifyGuidState(null);
                    }

                    return new JobExecutionResult(Level.INFO, "success");
                }

            }
        }
        return new JobExecutionResult(Level.WARN, "No notification occured");
    }

    /**
     * Create an Document in a folder path
     *
     * @param path
     * @param docName
     * @param session
     * @param create
     * @return
     * @throws IOException
     * @throws StoreException
     */
    public static final com.hitorro.base.objects.VersionableObject getDocumentFromPathCreatingWithEmptyContent(String path,
                                                                                                               String docName,
                                                                                                               DMSSession session,
                                                                                                               boolean create)
            throws IOException, StoreException {
        com.hitorro.base.objects.Folder folder = getLeafFolder(path, create, session);
        if (folder == null) {
            return null;
        }
        Query q = folder.getQueryWithAppend(session, Fmt.S(" and title='%s'", docName));
        List<com.hitorro.base.objects.VersionableObject> children = q.list();
        if (ListUtil.nullOrEmpty(children)) {
            com.hitorro.base.objects.Document doc = new com.hitorro.base.objects.Document();
            doc.setTitle(docName);
            doc.addContainer(folder);
            session.persist(doc);

            createContentForFileName(doc, docName);
            return doc;
        } else {
            com.hitorro.base.objects.VersionableObject child = children.get(0);
            com.hitorro.base.objects.Content c = child.getContentByFileName(docName, true);
            if (c == null) {
                createContentForFileName(child, docName);
            }
            return child;
        }
    }

    private static void createContentForFileName(com.hitorro.base.objects.VersionableObject doc, String docName) throws IOException, StoreException {
        com.hitorro.base.objects.ContentType ct = ContentTypeCache.getCache().getTypeFromFileWithDefault(docName);
        doc.createZeroLengthContent(docName, ct, null);
    }


    /**
     * Given a path of:
     * <p/>
     * /x/y/z
     *
     * @param path
     * @param createIfMissing
     * @param session
     * @return
     */
    public static com.hitorro.base.objects.Folder getLeafFolder(String path, boolean createIfMissing, DMSSession session) {
        String parts[] = StringUtil.tokenizeFromSingleChar(path, "/");
        if (parts == null || parts.length == 0) {
            return null;
        }
        com.hitorro.base.objects.Folder root = com.hitorro.base.objects.Folder.getFolderForName(session, parts[0], true);
        com.hitorro.base.objects.Folder curr = root;
        if (root == null) {
            if (createIfMissing == false) {
                return null;
            }
            root = new com.hitorro.base.objects.Folder();
            root.setName(parts[0]);
            root.setIsRootLevel(true);
            session.persist(root);
            return createTailFolders(root, parts, 1, session);
        }
        for (int i = 1; i < parts.length; i++) {
            Query q = curr.getQuery(session,
                    Fmt.S("select f from Folder f where containers.id= :id and name='%s'",
                            parts[i]));

            List<com.hitorro.base.objects.Folder> children = q.list();
            if (ListUtil.nullOrEmpty(children)) {
                if (createIfMissing == false) {
                    return null;
                }
                return createTailFolders(curr, parts, i, session);
            }
            curr = children.get(0);
        }
        return curr;
    }

    /**
     * iteratively descend the folder path, creating all the listChildren.
     *
     * @param parent
     * @param path
     * @param childIndex
     * @param session
     * @return
     */
    private static com.hitorro.base.objects.Folder createTailFolders(com.hitorro.base.objects.Folder parent, String path[], int childIndex, DMSSession session) {

        for (int i = childIndex; i < path.length; i++) {
            com.hitorro.base.objects.Folder child = new com.hitorro.base.objects.Folder();
            child.setName(path[i]);
            child.addContainer(parent);
            session.persist(child);
            parent = child;
        }
        return parent;
    }


    public static boolean touchObject(String guid) {
        boolean touched = false;
        BaseSession session = DMSSessionFactory.getFactory().getSession();

        if (session != null) {
            try {
                HTSerializable serializable = session.getObjectFromGuid(guid);
                com.hitorro.base.objects.VersionableObject sysobject = (com.hitorro.base.objects.VersionableObject) serializable;

                sysobject.touch();

                session.commit();
                touched = true;
            } finally {
                DMSSessionFactory.closeSession(session);
            }
        }

        return touched;
    }

}
