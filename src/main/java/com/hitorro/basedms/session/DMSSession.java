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
package com.hitorro.basedms.session;

import com.hitorro.base.objects.Content;
import com.hitorro.base.objects.Document;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.base.typesystem.accessors.GuidAccessor;
import com.hitorro.basedms.db.HibernateService;
import com.hitorro.basedms.db.HibernateUtil;
import com.hitorro.util.core.Console;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.iterator.AbstractIterator;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.json.keys.propaccess.PropaccessError;
import com.hitorro.util.startupframework.ServiceContext;
import com.hitorro.util.typesystem.BaseSessionFactory;
import com.hitorro.util.typesystem.HTSerializable;
import com.hitorro.util.typesystem.SessionException;
import jakarta.persistence.FlushModeType;
import org.hibernate.*;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;

import java.io.InputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.Connection;
import java.util.*;
import java.util.function.Function;

/**
 * Document Management System Session.
 * <p/>
 * Provides the basic transactional services to hibernate and offers higher order accessors to the DMS objects.
 *
 * @author chris
 */


public class DMSSession extends com.hitorro.util.typesystem.BaseSession {
    private static final String NotSet = "Not Set";
    private static final String SOByIdentityHash = "select pst from VersionableObject as pst where identityHash= :a";
    private static final String SOByIdentityHashGuid = "select guid from VersionableObject as pst where identityHash= :a";
    private transient static long m_sessionId = 0;
    private long currentSessionId = m_sessionId++;
    private Session m_session = null;
    private boolean m_closed = false;
    private Transaction m_transaction = null;
    private String m_key = null;
    private boolean m_enableCache = false;
    private HashMap<String, com.hitorro.util.typesystem.BaseType> m_attatchedCache = new HashMap<String, com.hitorro.util.typesystem.BaseType>();
    private HashMap<String, com.hitorro.util.typesystem.BaseType> m_unattatchedCache = new HashMap<String, com.hitorro.util.typesystem.BaseType>();
    private List<String> objectsToDeleteFromAttatched = new ArrayList<String>();
    // debug info
    private String m_threadName = null;
    private String m_groupName = null;
    private Throwable m_throwable = null;
    private long createTime;
    private String m_name = NotSet;
    private String effectiveuserName = "system";
    private String userName = "system";
    private String realm = null;
    private DMSSecurityModel securityModel = DMSOpenSecurityModel.m;
    private DMSSessionInterceptor m_interceptor = new DMSSessionInterceptor(this);

    /**
     * Default constructor that uses a default database configuration
     */
    DMSSession() {

    }

    DMSSession(boolean enableCache) {
        m_enableCache = enableCache;

    }

    DMSSession(String key, boolean enableCache)
            throws SessionException {
        m_enableCache = enableCache;
        initDBKey(key);
    }

    DMSSession(String key)
            throws SessionException {
        initDBKey(key);
    }

    public DMSSecurityModel getSecurityModel() {
        return securityModel;
    }

    public BaseSessionFactory getSessionFactory() {
        return DMSSessionFactory.getFactory();
    }

    /**
     * reference by a key other than a guid.  handled by the guidaccessor. Used by those that know their type but do not
     * use a guid field to access their "stuff'
     *
     * @param key
     * @return
     */
    public HTSerializable getSoftReference(com.hitorro.util.typesystem.Type t, String key) {
        //XXX TODO should not cast, should have a better interface.
        GuidAccessor ga = (GuidAccessor) t.getGuidAccessor();
        return ga.getObjectSoftRef(key, this);
    }

    /**
     * id that could be a standard guid, or something that is
     *
     * @param guid
     * @param guidSansType
     * @return
     */
    public com.hitorro.util.typesystem.HTSerializable getGuidReference(com.hitorro.util.typesystem.Type type, String guid, String guidSansType) {
        //XXX TODO should not cast, should have a better interface.
        GuidAccessor ga = (GuidAccessor) type.getGuidAccessor();
        return ga.getObject(guid, guidSansType, this);
    }

    public Blob createBlob(InputStream is, int size) {
        ensureSession();
        return m_session.getLobHelper().createBlob(is, size);
    }

    /**
     * Get an object given the ordered listFiles of key parts that make up the key.
     *
     * @param parts
     * @return
     */
    public com.hitorro.util.typesystem.HTSerializable getGuidReference(com.hitorro.util.typesystem.Type t, String... parts) {
        //XXX TODO should not cast, should have a better interface.
        GuidAccessor ga = (GuidAccessor) t.getGuidAccessor();
        String glob = StringUtil.mergeWithJoinToken(parts, com.hitorro.util.typesystem.Type.SoftFieldSeperator);
        return ga.getObjectSoftRef(glob, this);
    }

    /**
     * get an object by its guid provided in a key apply (that is, a apply of key part name ->key value
     *
     * @param keyParts
     * @return
     */
    public com.hitorro.util.typesystem.HTSerializable getGuidReference(com.hitorro.util.typesystem.Type t, Map<String, String> keyParts) {
        //XXX TODO should not cast, should have a better interface.
        GuidAccessor ga = (GuidAccessor) t.getGuidAccessor();
        String[] keyPartsList = ga.softGuidKeyParts();
        StringBuilder builder = new StringBuilder();
        for (String key : keyPartsList) {
            String val = keyParts.get(key);
            if (StringUtil.nullOrEmptyOrBlankString(val)) {
                com.hitorro.util.typesystem.Log.typemanager.error("Key past in did not include key part %s", key);
                return null;
            }
            if (!builder.isEmpty()) {
                builder.append(com.hitorro.util.typesystem.Type.SoftFieldSeperator);
            }
            builder.append(val);
        }

        return ga.getObjectSoftRef(builder.toString(), this);
    }

    public String getEffectiveUser() {
        return effectiveuserName;
    }

    /**
     * User based DMS requests should be accessed under their appropriate realm.
     *
     * @param name
     * @param effectiveName
     * @param realm
     * @param secModel
     */
    public void setUser(String name, String effectiveName, String realm, DMSSecurityModel secModel) {
        userName = name;
        this.effectiveuserName = effectiveName;
        this.realm = realm;
        if (StringUtil.nullOrEmptyString(realm)) {
            this.realm = null;
        }
        this.securityModel = secModel;
    }

    public String getRealm() {
        return realm;
    }

    public String getUser() {
        return userName;
    }

    public String getName() {
        return m_name;
    }

    public void setName(String name) {
        m_name = name;
    }

    public String toString() {
        return m_name;
    }

    /**
     * Verify if the object is part of the session.
     *
     * @param o
     * @return
     */
    public boolean isObjectPartOfSession(Object o) {
        this.ensureSession();
        return m_session.contains(o);
    }

    public String dumpStats() {
        StringBuilder b = new StringBuilder();
        if (m_session != null) {
            // EntityMode
            //m_session.
            //TODO UPDATE`
            //Console.bprintln(b, "CacheMode: %s, EntityMode: %s", m_session.getCacheMode(), m_session.getEntityMode());
            Console.bprintln(b, "CacheMode: %s", m_session.getCacheMode());

        }

        return b.toString();
    }

    public void enableCache(boolean enableCache) {
        m_enableCache = enableCache;
        if (!m_enableCache) {
            this.clearCaches();
        }

    }

    public void setThreadInfo(boolean recordStack) {
        Thread t = Thread.currentThread();
        m_threadName = t.getName();
        ThreadGroup tg = t.getThreadGroup();
        createTime = System.currentTimeMillis();
        m_groupName = tg.getName();
        if (recordStack) {
            m_throwable = new Throwable();
        }
    }

    private void initDBKey(String key)
            throws SessionException {
        m_key = key;
        Object o = ServiceContext.getSC().getInitializedModule(HibernateService.class);
        if (o != null) {
            if (o instanceof HibernateService) {
                HibernateService hs = (HibernateService) o;
                try {
                    if (!hs.ensureSessionFactory(key)) {
                        throw new SessionException(Fmt.S("Unable to create factory for %s", key));
                    }
                } catch (PropaccessError propaccessError) {
                    new SessionException(Fmt.S("Unable to retrieve HibernateService %s %e", propaccessError, propaccessError));
                }
                return;
            }
        }
        throw new SessionException(Fmt.S("Unable to retrieve HibernateService"));
    }

    public void fetch(Object o) {
        ensureSession();
        m_session.refresh(o);
    }

    public void persist(Object o) {
        Log.dmssession.debug("Persisting (session: %s) )%s", currentSessionId, o);
        ensureSession();
        if (o instanceof com.hitorro.util.typesystem.BaseType) {
            com.hitorro.util.typesystem.BaseType bt = (com.hitorro.util.typesystem.BaseType) o;
            if (bt.getStale()) {
                Log.dmssession.error("Object %s is stale and being added to the cache, this should not happen!", bt.getGuid());
                return;
            }
        }
        //TypeManager.executeTrigger(OnTrigger.TriggerType.BeforeSave, o, true);
        if (o instanceof VersionableObject) {
            // fill in any VO related stuff
            VersionableObject vo = (VersionableObject) o;
            vo.setRealm(this.realm);
            vo.setEffectiveUser(this.getEffectiveUser());
            vo.setCreator(this.getUser());
        }
        addToCache(o);
        m_session.persist(o);
    }

    public void update(Object o) {
        Log.dmssession.debug("Updating (session: %s) %s", currentSessionId, o);
        ensureSession();
        m_session.update(o);
    }

    public void flush() {
        ensureSession();
        m_session.flush();
        m_session.clear();
    }

    public void saveOrUpdate(Object o) {
        Log.dmssession.debug("Save Or Update (session: %s) %s", currentSessionId, o);
        addToCache(o);
        saveOrUpdateAux(o);
    }

    public Query createQuery(String query) {
        ensureSession();
        return m_session.createQuery(query);
    }

    public AbstractIterator getIteratorFromQuery(HibernateQueryResultObjectAdapter adapter, String query, Object... vals) {
        Query q = getQuery(query, vals);
        if (q == null) {
            return null;
        }
        return getIteratorFromQuery(adapter, q);
    }

    /**
     * iterate through a collection of results, that are not persisted objects, converting them into a single object
     * using the HibernateQueryResultObjectAdapter interface to perform the mapping.
     *
     * @param query
     * @param adapter
     * @return
     */
    public AbstractIterator getIteratorFromQuery(HibernateQueryResultObjectAdapter adapter, Query query) {
        return new HibernateQueryIteratorMapper(adapter, query);
    }

    public void doJdbcWork(Function<Connection, Object> work) {
        m_session.doWork(connection -> {
            work.apply(connection);
        });
    }

    public NativeQuery createSQLQuery(String query) {
        ensureSession();
        return m_session.createNativeQuery(query);
    }

    public Iterator getIteratorFromQuery(String query) {
        return getIteratorFromQueryArgs(query, (Object) null);
    }

    /**
     * Run a query with parameters.
     *
     * @param query The query string, with ? parameters (jdbc style)
     * @param vals  Values to be placed in the parameters, in order.
     * @return An iterator containing the query results, null if there is a problem
     */
    public Iterator getIteratorFromQueryArgs(String query, Object... vals) {
        Query q = getQuery(query, vals);
        if (q != null) {
            return q.stream().iterator();
        }
        return null;
    }

    public Query getQuery(String query, Object... vals) {
        Query q = createQuery(query);
        if (q != null) {
            if (vals != null && (vals.length >= 1 && vals[0] != null)) {
                int indx = 0;
                for (Object obj : vals) {
                    char c = (char) ((int) 'a' + indx++);
                    q.setParameter(String.valueOf(c), obj);
                }
            }
            return q;
        }
        return null;
    }

    /**
     * Fetch objects with an HQL query.
     *
     * @param clazz       The class to be queried
     * @param query       a constraint (query without the classname).  May have JDBC-style ? parameters.
     * @param list        Resulting objects are placed in this listFiles.
     * @param paramValues values, in order, to be placed in parameters
     */
    public void getObjects(Class clazz, String query, List list, Object... paramValues) {
        getObjects(Fmt.S("from %s %s", clazz.getCanonicalName(), query), list, paramValues);
    }

    /**
     * Fetch objects with an HQL query.
     *
     * @param clazz The class to be queried
     * @param query a constraint (query without the classname).  No parameters allowed.
     * @param list  Resulting objects are placed in this listFiles.
     */
    public void getObjects(Class clazz, String query, List list) {
        getObjects(Fmt.S("from %s %s", clazz.getCanonicalName(), query), list, (Object) null);
    }

    /**
     * Fetch objects with an HQL query.
     *
     * @param query The full HQL query (including classname) to use.  No parameters allowed.
     * @param list  Resulting objects are placed in this listFiles.
     */
    public void getObjects(String query, List list) {
        getObjects(query, list, (Object) null);
    }

    /**
     * Fetch objects with an HQL query.
     *
     * @param query       The full HQL query (including classname) to use.  May have JDBC-style ? parameters.
     * @param list        Resulting objects are placed in this listFiles.
     * @param paramValues values, in order, to be placed in parameters
     */
    public void getObjects(String query, List list, Object... paramValues) {
        ensureSession();
        Iterator iter = getIteratorFromQueryArgs(query, paramValues);
        while (iter.hasNext()) {
            list.add(iter.next());
        }
    }

    /**
     * Fetch a single object with an HQL query.
     *
     * @param clazz       The class to be queried
     * @param query       a constraint (query without the classname).  May have JDBC-style ? parameters.
     * @param paramValues values, in order, to be placed in parameters
     * @return the found object or null if none, or more than one, object is found
     */
    public Object getObject(Class clazz, String query, Object... paramValues) {
        return getObjectElipses(Fmt.S("from %s %s", clazz.getCanonicalName(), query), paramValues);
    }

    /**
     * Fetch a single object with an HQL query.
     *
     * @param query       The full HQL query (including classname) to use.  May have JDBC-style ? parameters.
     * @param paramValues values, in order, to be placed in parameters
     * @return the found object or null if none, or more than one, object is found
     */
    public Object getObjectElipses(String query, Object... paramValues) {
        return getObject(query, false, false, paramValues);
    }

    public Object getObject(String query, boolean allowDupes, boolean warn, Object... paramValues) {
        ensureSession();
        Object result = null;
        Iterator iter = getIteratorFromQueryArgs(query, paramValues);
        if (iter.hasNext()) {
            // we want one result...
            result = iter.next();

            if (iter.hasNext()) {
                if (warn) {
                    Log.util.error("Duplicate row for query %s", query);
                }
                if (!allowDupes) {
                    // ... but not more
                    result = null;
                }
            }
        }

        return result;
    }

    public void deleteObjectIfExists(String guid) {
        com.hitorro.util.typesystem.HTSerializable pts = getObjectFromGuid(guid);
        if (pts != null) {
            delete(pts);
        }
    }

    public void delete(Object o) {
        ensureSession();
        m_session.delete(o);
        markForRemoval(o);
    }

    /**
     * Get an object from its hash.
     *
     * @param linkHash
     * @param ensureNotProxy
     * @return
     */
    public com.hitorro.util.typesystem.HTSerializable getObjectFromHash(long linkHash, boolean ensureNotProxy) {
        ensureSession();

        com.hitorro.util.typesystem.HTSerializable pts = (com.hitorro.util.typesystem.HTSerializable) getObject(SOByIdentityHash, true, true, linkHash);
        if (ensureNotProxy) {
            if (pts != null) {
                return getNonProxyObject(pts);
            }
        }
        return pts;
    }

    public String getObjectGuidFromHash(long linkHash, boolean ensureNotProxy) {
        ensureSession();

        return (String) getObject(SOByIdentityHashGuid, true, true, linkHash);
    }

    /**
     * @param guid
     * @return
     */
    public com.hitorro.util.typesystem.HTSerializable getObjectFromGuid(String guid) {
        ensureSession();
        if (guid == null) {
            return null;
        }
        com.hitorro.util.typesystem.HTSerializable pts;
        if (this.m_enableCache) {
            pts = m_attatchedCache.get(guid);
            if (pts != null) {
                return pts;
            }
            pts = m_unattatchedCache.get(guid);
            if (pts != null) {
                // re-attach
                Log.dmssession.debug("getting object guid (session: %s) %s and adding to cache %s", currentSessionId, guid, pts);

                saveOrUpdateAux(pts);
                m_attatchedCache.put(guid, (com.hitorro.util.typesystem.BaseType) pts);
                m_unattatchedCache.remove(guid);
                return pts;
            }
        }
        /**
         * not enabled or not in cache!
         *
         * Note that because we have an interceptor in the cached case, we catch the onLoad and therefor put
         * to the cache.  No need to put to the cache here.
         *
         */
        return getHTSerializableFromGUID(guid);
    }

    private void resetVersionStamps() {
        if (this.m_enableCache) {
            Collection<com.hitorro.util.typesystem.BaseType> col = m_unattatchedCache.values();
            for (com.hitorro.util.typesystem.BaseType bt : col) {
                if (bt instanceof com.hitorro.util.typesystem.VersionBaseType) {
                    com.hitorro.util.typesystem.VersionBaseType vbt = (com.hitorro.util.typesystem.VersionBaseType) bt;
                    vbt.recoverSnapshotVersionStamp();
                    vbt.resetMarkedFlushed();
                }
            }
        }
    }

    public void dumpObjects(String description) {
        Console.println(description);
        dumpObjects(m_attatchedCache.values(), "attatched");
        dumpObjects(this.m_unattatchedCache.values(), "unattatched");
    }

    private void dumpObjects(Collection<com.hitorro.util.typesystem.BaseType> col, String attatchedState) {
        for (com.hitorro.util.typesystem.BaseType bt : col) {
            Console.println(formatBT(bt, attatchedState));
        }
    }

    private String formatBT(com.hitorro.util.typesystem.BaseType bt, String attatchedState) {
        return Fmt.S("S:%s, G:%s, C:%s, O:%s", attatchedState, bt.getGuid(), bt.getCommitCount(), bt.getStale());
    }

    private void snapshotVersionStampsPostCommit() {
        // should be an extra condition that the object was being ed
        if (this.m_enableCache) {
            Collection<com.hitorro.util.typesystem.BaseType> col = this.m_attatchedCache.values();
            for (com.hitorro.util.typesystem.BaseType bt : col) {
                if (bt instanceof com.hitorro.util.typesystem.VersionBaseType) {
                    com.hitorro.util.typesystem.VersionBaseType vbt = (com.hitorro.util.typesystem.VersionBaseType) bt;
                    if (vbt.isMarkedFlushed()) {
                        vbt.incrementCommitCount();
                        // only snapshot the version stamp IF we believe it was flushed to the db
                        vbt.snapshotVersionStamp();
                        vbt.resetMarkedFlushed();
                    }
                }
            }
        }
    }

    public Object refresh(Object o) {
        ensureSession();
        try {
            m_session.refresh(o);
            return o;
        } catch (UnresolvableObjectException e) {
            Log.dmssession.warn("Unable to fetch object, it no longer exists, %s", e);
            return null;
        }

    }

    public Document retrieveObjectById(Class c, Long id) {
        ensureSession();
        return (Document) m_session.get(c, id);
    }

    public Document retrieveDocumentById(Long id) {
        return retrieveObjectById(Document.class, id);
    }

    public Content retrieveContentById(Long id) {
        ensureSession();
        return m_session.get(Content.class, id);
    }

    /**
     * retrieve a single object from the target class table with the optional constraint.
     *
     * @param cls        class to query
     * @param constraint extension to the from cls
     * @return deserialized object matching criteria
     */
    public Object getSingleObject(Class cls, String constraint) {
        ensureSession();
        String q = Fmt.S("from %s %s", cls.getCanonicalName(), constraint);
        Iterator i = m_session.createQuery(q).stream().iterator();
        if (i != null && i.hasNext()) {
            return i.next();
        }
        return null;
    }

    public Object getSingleObjectById(Class cls, Serializable ser) {
        ensureSession();
        return m_session.get(cls, ser);
    }

    public long getTableRowCount(String table) {
        ensureSession();
        return (Long) (m_session.createQuery(Fmt.S("select count(*) from %s", table)).stream().iterator().next());
    }

    public void rollback() {
        //TODO UPDATE
        //if (m_transaction != null && m_session != null && m_session.isConnected() && m_transaction.isActive())

        if (m_transaction != null && m_session != null && m_session.isConnected()) {
            m_transaction.rollback();
            m_transaction = null;
        }
        if (m_session != null) {
            m_session.clear();
        }
    }

    public void commitList(List list) {
        ensureSession();
        for (Object o : list) {
            this.persist(o);
        }
        this.commit();
    }

    public void commit() {
        if (m_transaction != null) {
            if (this.m_enableCache) {
                //Console.println("Commit: %s", m_name);
            }
            m_transaction.commit();
            // we should probably only do the objects that got really commited
            snapshotVersionStampsPostCommit();
            m_transaction = null;
            applyRemoveListPostCommit();
        }
    }

    /**
     * Disconnect the hibernate session
     */
    public void disconnectSession() {
        if (m_session != null) {
            if (this.m_enableCache) {
                //  Console.println("disconnect: %s", m_name);
            }
            Log.dmssession.debug("disconnectingSession %s", this.m_name);

            if (m_transaction != null) {
                m_transaction.rollback();
            }
            if (m_session.isConnected()) {
                m_session.close();
            }
            m_session = null;
            m_transaction = null;
            m_closed = true;
        }
        moveAttatchedToUnattatched();
    }

    public void rollbackAndClose() {
        if (this.m_enableCache) {
            //  Console.println("rollback and close: %s", m_name);
        }
        rollback();
        close();
    }

    /**
     * We have completely finished with this session. XXX TODO Should not be public
     */
    public void close() {
        disconnectSession();
        m_attatchedCache.clear();
        m_unattatchedCache.clear();
    }

    /**
     * Clear the object caches for this session.
     */
    public void clearCaches() {
        if (m_enableCache) {
            m_attatchedCache.clear();
            m_unattatchedCache.clear();
        }
    }


    private void ensureSession() {

        if (m_session == null || !m_session.isConnected()) {
            if (m_session != null && !m_session.isConnected()) {
                Log.dmssession.error("DMSSession.ensureSession session was not null but session disconnected %s");
            }
            if (m_closed == true) {
                ((DMSSessionFactory) DMSSessionFactory.getFactory()).reRegisterAttatchSession(this);
                m_closed = false;
            }
            resetVersionStamps();
            if (m_key != null) {
                if (this.m_enableCache) {
                    // we need an interceptor to catch
                    m_session = HibernateUtil.getNonThreadBasedSession(m_key, this.m_interceptor);

                } else {
                    m_session = HibernateUtil.getNonThreadBasedSession(m_key);
                }
            } else {
                if (this.m_enableCache) {
                    // we need an interceptor to catch
                    m_session = HibernateUtil.getNonThreadBasedSession(this.m_interceptor);
                } else {
                    m_session = HibernateUtil.getNonThreadBasedSession();
                }
            }
            // TODO Switch off cache??
            m_session.setCacheMode(CacheMode.IGNORE);
            CacheMode cm = m_session.getCacheMode();
            // ensure that the transaction is null before we go any further!
            if (m_transaction != null) {
                Log.hibernate.error("Transaction != when session was null...NULLING OUT");
                m_transaction = null;
            }
        }

        //TODO UPDATE
        //if (m_transaction == null || !m_transaction.isActive())
        if (m_transaction == null) {
            //EntityMode em = m_session.getEntityMode();
            FlushModeType fm = m_session.getFlushMode();
            //m_session.setFlushMode(FlushMode.COMMIT);
            m_transaction = m_session.beginTransaction();

        }
    }

    protected void addToCache(Object o) {
        if (m_enableCache) {
            if (o instanceof com.hitorro.util.typesystem.BaseType) {
                com.hitorro.util.typesystem.BaseType bt = (com.hitorro.util.typesystem.BaseType) o;
                String guid = ((com.hitorro.util.typesystem.BaseType) o).getGuid();
                applySessionToBaseType(bt);
                this.addToCache(guid, (com.hitorro.util.typesystem.BaseType) o);
            }
        }
    }

    protected void markForRemoval(Object o) {
        if (m_enableCache) {
            if (o instanceof com.hitorro.util.typesystem.BaseType) {

                String guid = ((com.hitorro.util.typesystem.BaseType) o).getGuid();
                objectsToDeleteFromAttatched.add(guid);
            }
        }
    }

    protected void applyRemoveListPostCommit() {
        if (objectsToDeleteFromAttatched.size() > 0) {
            for (String guid : objectsToDeleteFromAttatched) {
                this.m_attatchedCache.remove(guid);
            }
        }
        objectsToDeleteFromAttatched.clear();
    }


    protected void applySessionToBaseType(com.hitorro.util.typesystem.BaseType bt) {
        if (!bt.setSession(this)) {
            Log.dmssession.error("Attempted to set a different session on a persisted object %s", bt.getGuid());
        }
    }

    public void addToCache(String guid, com.hitorro.util.typesystem.BaseType pts) {
        if (pts.getStale()) {
            Log.dmssession.error("Object %s is stale and being added to the cache, this should not happen!", guid);
            return;
        }

        if (m_enableCache) {
            com.hitorro.util.typesystem.BaseType ptsOld = m_attatchedCache.put(guid, pts);
            if (ptsOld != null) {
                ptsOld.setStale();
            }
            // just in case this is through a query and we are reconstituting
            ptsOld = m_unattatchedCache.remove(guid);
            if (ptsOld != null) {
                ptsOld.setStale();
            }
        }
    }

    protected void removeFromCache(String guid) {
        if (m_enableCache) {
            com.hitorro.util.typesystem.BaseType bt = m_attatchedCache.remove(guid);
            if (bt != null) {
                bt.setStale();
            }
            bt = m_unattatchedCache.remove(guid);
            if (bt != null) {
                bt.setStale();
            }
        }
    }

    protected void moveAttatchedToUnattatched() {
        m_unattatchedCache.putAll(m_attatchedCache);
        m_attatchedCache.clear();
    }

    protected void saveOrUpdateAux(Object o) {
        Log.dmssession.debug("Save Or Update (AUX) (Session: %s) %s", currentSessionId, o);
        ensureSession();
        //TypeManager.executeTrigger(OnTrigger.TriggerType.BeforeSave, o, true);
        /*(BaseType bt = (BaseType)o;
        if (bt instanceof VersionBaseType)
        {
            VersionBaseType vbt = (VersionBaseType) bt;
            int versionStamp = vbt.getVersionStamp();
            if (versionStamp == 0)
            {
                m_session.persist(o);
                return;
            }
        } */
        m_session.saveOrUpdate(o);
        //m_session.save(o);
    }

    /**
     * Get an object back by its guid
     *
     * @param guid
     * @return
     */
    public com.hitorro.util.typesystem.HTSerializable getHTSerializableFromGUID(String guid) {
        String shortName[] = StringUtil.splitByToken(guid, ':');
        com.hitorro.util.typesystem.Type type = (com.hitorro.util.typesystem.Type) com.hitorro.util.typesystem.TypeManager.getTypeManager().getTypeByShortName(shortName[0]);

        return getGuidReference(type, guid, shortName[1]);
    }

    public com.hitorro.util.typesystem.HTSerializable getBySoftReference(Class c, String key) {
        com.hitorro.util.typesystem.Type type = com.hitorro.util.typesystem.TypeManager.getTypeManager().getTypeForClass(c);
        if (type == null) {
            return null;
        }
        return getSoftReference(type, key);
    }

    /**
     * Sometimes an object returned may not be the appropriate implementation...for instance the sysobject reference in
     * a sysobject may infact point to a blog.  In that case we may end up using the wrong logic on that object (for
     * instance serialization). https://forum.hibernate.org/viewtopic.php?f=1&t=987738&start=0
     *
     * @param pts
     * @return
     */
    public com.hitorro.util.typesystem.HTSerializable getNonProxyObject(com.hitorro.util.typesystem.HTSerializable pts) {
        if (pts.isPersisted() && pts.hasGuid()) {
            /*String guid = ((GuidBaseType) pts).getGuid();
            String shortName[] = StringUtil.splitByToken(guid, ':');
            Type t = TypeManager.getTypeManager().getTypeByShortName(shortName[0]);
            if (t == null)
            {
                com.hitorro.util.typesystem.Log.typemanager.warn("No Type defined for %s", pts.getClassification().getCanonicalName());
                return pts;
            }
            if (t.isInstanceOf(pts))
            {
                return pts;
            }
            return getGuidReference(t, guid, shortName[1]); */
            if (pts instanceof HibernateProxy) {
                return (com.hitorro.util.typesystem.HTSerializable) ((HibernateProxy) pts).getHibernateLazyInitializer().getImplementation();
            }
        }
        return pts;
    }


    public long getCreateTime() {
        return createTime;
    }

    public Date getCreateDate() {
        return new Date(createTime);
    }

    public String getReadableCreateTime() {
        return Fmt.formatDateTime(createTime);
    }

    public String getThreadName() {
        return m_threadName;
    }

    public String getGroupName() {
        return m_groupName;
    }

    public Throwable getThrowable() {
        return m_throwable;
    }

    class DMSSessionInterceptor extends EmptyInterceptor {
        private DMSSession m_sess;

        DMSSessionInterceptor(DMSSession sess) {
            m_sess = sess;
        }

        public void postFlush(java.util.Iterator iterator) {
            if (iterator != null) {
                while (iterator.hasNext()) {
                    Object bt = iterator.next();
                    if (bt instanceof com.hitorro.util.typesystem.VersionBaseType) {
                        com.hitorro.util.typesystem.VersionBaseType vbt = (com.hitorro.util.typesystem.VersionBaseType) bt;
                        vbt.markFlushed();
                    }
                }
            }
        }

        public void onDelete(java.lang.Object object, java.io.Serializable serializable, java.lang.Object[] objects, java.lang.String[] strings, org.hibernate.type.Type[] types) {
            if (object instanceof com.hitorro.util.typesystem.BaseType) {
                ((com.hitorro.util.typesystem.BaseType) object).setSession(m_sess);
                String guid = ((com.hitorro.util.typesystem.BaseType) object).getGuid();
                m_sess.removeFromCache(guid);
            }
        }

        public boolean onLoad(java.lang.Object object, java.io.Serializable serializable, java.lang.Object[] objects, java.lang.String[] strings, org.hibernate.type.Type[] types) {
            if (object instanceof com.hitorro.util.typesystem.BaseType) {
                com.hitorro.util.typesystem.BaseType bt = (com.hitorro.util.typesystem.BaseType) object;
                bt.setSession(m_sess);
                m_sess.applySessionToBaseType(bt);
            }
            return true;
        }
    }
}


