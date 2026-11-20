package com.hitorro.basedms.session;

import com.hitorro.base.typesystem.listeners.HTHibernateDeleteListener;
import com.hitorro.base.typesystem.listeners.HTHibernateOnSaveListener;
import com.hitorro.base.typesystem.listeners.HTHibernatePersistListener;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.TransientObjectException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.spi.EventType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DMSSession save/update/delete on Hibernate 7 (H2 in memory), and the HTIntegrator listeners
 * that replaced the SAVE_UPDATE listener. Hibernate 7 removed Session.save/update/saveOrUpdate/
 * delete; DMSSession now persists new objects, merges managed/detached ones, and removes via a
 * managed instance.
 * <p>
 * The database matrix subclasses (DMSSessionHibernate7PostgresTest, DMSSessionHibernate7MySqlTest)
 * run these tests on PostgreSQL and MySQL by overriding {@link #openDatabase()}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DMSSessionHibernate7Test {

    private SessionFactory sessionFactory;
    private SessionFactory previousDefault;

    /** The database under test: in-memory H2 here; a container in the matrix subclasses. */
    protected SessionFactory openDatabase() {
        return TestSessionFactory.get();
    }

    protected void closeDatabase(SessionFactory sf) {
    }

    @BeforeAll
    void buildSessionFactory() {
        sessionFactory = openDatabase();
        previousDefault = TestSessionFactory.useAsDefault(sessionFactory);
    }

    @AfterAll
    void restoreDefaultSessionFactory() {
        TestSessionFactory.useAsDefault(previousDefault);
        closeDatabase(sessionFactory);
    }

    private static String guid() {
        return "probe:" + UUID.randomUUID();
    }

    private ProbeEntity readBack(Long id) {
        try (Session s = sessionFactory.openSession()) {
            return s.get(ProbeEntity.class, id);
        }
    }

    /** Persist through one DMSSession and commit; the returned instance is detached from any other session. */
    private ProbeEntity createCommitted(String name, boolean cache) {
        DMSSession s1 = new DMSSession(cache);
        ProbeEntity e = new ProbeEntity(guid(), name);
        s1.saveOrUpdate(e);
        s1.commit();
        assertNotNull(e.getId(), "saveOrUpdate of a new object must persist this very instance");
        return e;
    }

    @Test
    void integratorRegistersHiTorroListeners() {
        var registry = sessionFactory.unwrap(SessionFactoryImplementor.class).getEventListenerRegistry();
        assertTrue(hasListener(registry.getEventListenerGroup(EventType.MERGE), HTHibernateOnSaveListener.class),
                "MERGE must carry the listener that replaced SAVE_UPDATE");
        assertTrue(hasListener(registry.getEventListenerGroup(EventType.PERSIST), HTHibernatePersistListener.class));
        assertTrue(hasListener(registry.getEventListenerGroup(EventType.DELETE), HTHibernateDeleteListener.class));
    }

    private static boolean hasListener(EventListenerGroup<?> group, Class<?> type) {
        return StreamSupport.stream(group.listeners().spliterator(), false).anyMatch(type::isInstance);
    }

    @Test
    void saveOrUpdateOfNewObjectPersistsIt() {
        ProbeEntity e = createCommitted("new", false);
        ProbeEntity stored = readBack(e.getId());
        assertEquals("new", stored.getName());
        assertEquals(e.getGuid(), stored.getGuid());
        // BeforeSave callbacks (as the SAVE_UPDATE listener did) plus the persist listener
        assertTrue(stored.getCallbackCount() >= 1, "save callbacks must run before the insert");
    }

    @Test
    void saveOrUpdateOfDetachedObjectUpdatesRowAndKeepsCallerInstanceUsable() {
        for (boolean cache : new boolean[]{false, true}) {
            ProbeEntity e = createCommitted("before", cache);
            int callbacksAfterCreate = readBack(e.getId()).getCallbackCount();
            int versionAfterCreate = e.getVersionStamp();

            DMSSession s2 = new DMSSession(cache);
            e.setName("after");
            s2.saveOrUpdate(e);
            s2.commit();

            ProbeEntity stored = readBack(e.getId());
            assertEquals("after", stored.getName(), "cache=" + cache);
            assertEquals(stored.getVersionStamp(), e.getVersionStamp(),
                    "the caller's instance must carry the new version, as a re-attached instance did");
            assertTrue(e.getVersionStamp() > versionAfterCreate);
            assertTrue(stored.getCallbackCount() > callbacksAfterCreate,
                    "the MERGE listener must run the save callbacks on the caller's instance before the copy");

            // The same detached instance can be saved again: no optimistic-lock failure
            e.setName("again");
            s2.saveOrUpdate(e);
            s2.commit();
            assertEquals("again", readBack(e.getId()).getName(), "cache=" + cache);

            // ...also from a fresh session
            DMSSession s3 = new DMSSession(cache);
            e.setName("third");
            s3.saveOrUpdate(e);
            s3.commit();
            assertEquals("third", readBack(e.getId()).getName(), "cache=" + cache);
        }
    }

    @Test
    void saveOrUpdateOfManagedObjectIsWrittenAtCommit() {
        ProbeEntity e = createCommitted("managed", false);
        DMSSession s = new DMSSession(false);
        ProbeEntity loaded = (ProbeEntity) s.getSingleObjectById(ProbeEntity.class, e.getId());
        loaded.setName("managed-changed");
        s.saveOrUpdate(loaded);
        s.commit();
        assertEquals("managed-changed", readBack(e.getId()).getName());
    }

    @Test
    void updateOfDetachedObjectUpdatesRow() {
        ProbeEntity e = createCommitted("upd", false);
        DMSSession s = new DMSSession(false);
        e.setName("upd-changed");
        s.update(e);
        s.commit();
        assertEquals("upd-changed", readBack(e.getId()).getName());
    }

    @Test
    void updateOfTransientObjectIsRejected() {
        DMSSession s = new DMSSession(false);
        assertThrows(TransientObjectException.class, () -> s.update(new ProbeEntity(guid(), "transient")));
        s.rollback();
    }

    @Test
    void persistInsertsNewObject() {
        DMSSession s = new DMSSession(false);
        ProbeEntity e = new ProbeEntity(guid(), "persisted");
        s.persist(e);
        s.commit();
        assertEquals("persisted", readBack(e.getId()).getName());
    }

    @Test
    void deleteOfDetachedObjectRemovesRow() {
        ProbeEntity e = createCommitted("del-detached", false);
        DMSSession s = new DMSSession(false);
        s.delete(e);
        s.commit();
        assertNull(readBack(e.getId()));
    }

    @Test
    void deleteOfManagedObjectRemovesRow() {
        ProbeEntity e = createCommitted("del-managed", true);
        DMSSession s = new DMSSession(true);
        Object loaded = s.getSingleObjectById(ProbeEntity.class, e.getId());
        s.delete(loaded);
        s.commit();
        assertNull(readBack(e.getId()));
    }

    @Test
    void flushThenSaveAgainWorks() {
        DMSSession s = new DMSSession(false);
        ProbeEntity e = new ProbeEntity(guid(), "flush-1");
        s.saveOrUpdate(e);
        s.flush();               // flush() also clears the session: e is now detached
        e.setName("flush-2");
        s.saveOrUpdate(e);
        s.commit();
        assertEquals("flush-2", readBack(e.getId()).getName());
    }

    @Test
    void rollbackDiscardsChanges() {
        ProbeEntity e = createCommitted("rb", false);
        DMSSession s = new DMSSession(false);
        e.setName("rb-changed");
        s.saveOrUpdate(e);
        s.rollback();
        assertEquals("rb", readBack(e.getId()).getName());
    }

    @Test
    void doJdbcWorkOnAFreshSessionRuns() {
        // JDBCUtils.performSimpleJDBCUpdate calls doJdbcWork on a new session
        ProbeEntity e = createCommitted("jdbc", false);
        DMSSession s = new DMSSession(false);
        s.doJdbcWork(connection -> {
            try (var st = connection.prepareStatement("update probe_entity set name = ? where system_id = ?")) {
                st.setString(1, "jdbc-changed");
                st.setLong(2, e.getId());
                assertEquals(1, st.executeUpdate());
            } catch (java.sql.SQLException ex) {
                throw new RuntimeException(ex);
            }
            return null;
        });
        s.commit();
        assertEquals("jdbc-changed", readBack(e.getId()).getName());
    }
}
