package com.hitorro.base.objects;

import com.hitorro.basedms.session.DMSSession;
import com.hitorro.basedms.session.TestSessionFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deleting a version that other versions still reference (canonical / nextVersion /
 * parentVersion / branchVersion) through DMSSession.delete. Before 3.1.1 this failed: an FK
 * violation on Hibernate 6, TransientPropertyValueException on Hibernate 7.
 * <p>
 * The database matrix subclasses (VersionTreeDeletePostgresTest, VersionTreeDeleteMySqlTest) run
 * these tests on PostgreSQL and MySQL by overriding {@link #openDatabase()}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VersionTreeDeleteTest {

    private SessionFactory sessionFactory;
    private SessionFactory previousDefault;

    /** The database under test: in-memory H2 here; a container in the matrix subclasses. */
    protected SessionFactory openDatabase() {
        return TestSessionFactory.get();
    }

    protected void closeDatabase(SessionFactory sf) {
    }

    @BeforeAll
    void factory() {
        sessionFactory = openDatabase();
        previousDefault = TestSessionFactory.useAsDefault(sessionFactory);
    }

    @AfterAll
    void restoreDefaultSessionFactory() {
        TestSessionFactory.useAsDefault(previousDefault);
        closeDatabase(sessionFactory);
    }

    private static VersionableObject vo(String label) {
        VersionableObject v = new VersionableObject();
        v.setGuid("vo:" + UUID.randomUUID());
        v.setVersionLabel(label);
        return v;
    }

    /** root, optionally with versions already linked; persisted and committed. */
    private void commit(VersionableObject... objects) {
        DMSSession s = TestSessionFactory.newSession(false);
        for (VersionableObject o : objects) {
            s.persist(o);
        }
        s.commit();
    }

    private static VersionableObject root(String label) {
        VersionableObject a = vo(label);
        a.setCanonical(a);
        a.setCanonicalGuid(a.getGuid());
        return a;
    }

    /** next version of prev (as createMajorMinorVersion links it). */
    private static VersionableObject nextOf(VersionableObject prev, String label) {
        VersionableObject b = vo(label);
        b.setParentVersion(prev);
        b.setCanonical(prev.getCanonical());
        b.setCanonicalGuid(prev.getCanonicalGuid());
        prev.setNextVersion(b);
        return b;
    }

    /** branch of from (as createBranchVersion links it). */
    private static VersionableObject branchOf(VersionableObject from, String label) {
        VersionableObject d = vo(label);
        d.setParentVersion(from);
        d.setCanonical(from.getCanonical());
        d.setCanonicalGuid(from.getCanonicalGuid());
        from.setBranchVersion(d);
        return d;
    }

    private VersionableObject read(Long id) {
        try (Session s = sessionFactory.openSession()) {
            VersionableObject v = s.get(VersionableObject.class, id);
            if (v != null) {
                // touch the links while the session is open
                ids(v.getCanonical(), v.getNextVersion(), v.getParentVersion(), v.getBranchVersion());
            }
            return v;
        }
    }

    private static void ids(VersionableObject... vs) {
        for (VersionableObject v : vs) {
            if (v != null) v.getId();
        }
    }

    private static Long id(VersionableObject v) {
        return v == null ? null : v.getId();
    }

    private void delete(Long id, boolean loadFirst) {
        DMSSession s = TestSessionFactory.newSession(false);
        Object target = loadFirst ? s.getSingleObjectById(VersionableObject.class, id) : read(id);
        s.delete(target);
        s.commit();
    }

    @Test
    void deleteRootPromotesNextVersion() {
        // the example app's case: a document with one newer version, delete the original
        VersionableObject a = root("1.0");
        VersionableObject b = nextOf(a, "1.1");
        commit(a, b);

        delete(a.getId(), true);

        assertNull(read(a.getId()), "root deleted");
        VersionableObject nb = read(b.getId());
        assertNotNull(nb, "the newer version survives");
        assertEquals(b.getId(), id(nb.getCanonical()), "the next version becomes the root");
        assertEquals(nb.getGuid(), nb.getCanonicalGuid());
        assertNull(nb.getParentVersion());
    }

    @Test
    void deleteMiddleVersionSplicesChain() {
        VersionableObject a = root("1.0");
        VersionableObject b = nextOf(a, "1.1");
        VersionableObject c = nextOf(b, "1.2");
        commit(a, b, c);

        delete(b.getId(), true);

        assertNull(read(b.getId()));
        VersionableObject na = read(a.getId());
        VersionableObject nc = read(c.getId());
        assertEquals(c.getId(), id(na.getNextVersion()), "predecessor now points to the successor");
        assertEquals(a.getId(), id(nc.getParentVersion()), "successor's parent is the predecessor");
        assertEquals(a.getId(), id(nc.getCanonical()), "root unchanged");
        assertEquals(a.getGuid(), nc.getCanonicalGuid());
    }

    @Test
    void deleteBranchClearsBranchLink() {
        VersionableObject a = root("1.0");
        VersionableObject d = branchOf(a, "1.0.1.0");
        commit(a, d);

        delete(d.getId(), true);

        assertNull(read(d.getId()));
        assertNull(read(a.getId()).getBranchVersion());
    }

    @Test
    void deleteRootWithOnlyABranchPromotesTheBranch() {
        VersionableObject a = root("1.0");
        VersionableObject d = branchOf(a, "1.0.1.0");
        commit(a, d);

        delete(a.getId(), true);

        VersionableObject nd = read(d.getId());
        assertEquals(d.getId(), id(nd.getCanonical()));
        assertEquals(nd.getGuid(), nd.getCanonicalGuid());
        assertNull(nd.getParentVersion());
    }

    @Test
    void deleteUnversionedObject() {
        VersionableObject a = root("1.0");
        commit(a);
        delete(a.getId(), true);
        assertNull(read(a.getId()));
    }

    @Test
    void deleteDetachedRoot() {
        VersionableObject a = root("1.0");
        VersionableObject b = nextOf(a, "1.1");
        commit(a, b);

        delete(a.getId(), false);   // instance from another (closed) session

        assertNull(read(a.getId()));
        assertEquals(b.getId(), id(read(b.getId()).getCanonical()));
    }
}
