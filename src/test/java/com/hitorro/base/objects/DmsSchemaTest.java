package com.hitorro.base.objects;

import com.hitorro.basedms.session.DMSSession;
import com.hitorro.basedms.session.TestSessionFactory;
import com.hitorro.basedms.transformer.debugcommand.DumpWorkflowTranscodeQueue;
import com.hitorro.basedms.workflow.OutstandingWorkflowItem;
import com.hitorro.basedms.workflow.WorkFlowItem;
import com.hitorro.basedms.workflow.WorkFlowItemEntry;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * basedms's whole entity model (every @Entity it ships, as HibernateService registers them) on a
 * real schema: Hibernate creates it, a second factory validates it, a blob column round-trips, and
 * the native SQL basedms runs works. On H2 here; on PostgreSQL and MySQL in DmsSchemaPostgresTest
 * and DmsSchemaMySqlTest.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DmsSchemaTest {

    static final Class<?>[] ALL_ENTITIES = {
            VersionableObject.class, Content.class, Container.class, Document.class, Folder.class,
            Post.class, Forum.class, Category.class, SubjectArea.class, Reference.class,
            RenditionDescriptor.class, PersistedSerializedObject.class, Client.class, ExtractedRelation.class,
            ContentType.class, User.class, Role.class, Permission.class, Store.class, Extension.class,
            DomainInfo.class, NamedLongEntry.class, RssFeedIn.class, ScheduledJob.class,
            WorkFlowItem.class, WorkFlowItemEntry.class, OutstandingWorkflowItem.class,
    };

    private SessionFactory sessionFactory;
    private SessionFactory previousDefault;

    /** A factory over the whole model on the database under test (H2 here). */
    protected SessionFactory openDatabase() {
        return TestSessionFactory.configuration(
                "jdbc:h2:mem:basedms-schema;DB_CLOSE_DELAY=-1", "sa", "", ALL_ENTITIES).buildSessionFactory();
    }

    /** The same database with hbm2ddl=validate, or null where validation does not apply. */
    protected Configuration validatingConfiguration() {
        return TestSessionFactory.configuration(
                "jdbc:h2:mem:basedms-schema;DB_CLOSE_DELAY=-1", "sa", "", ALL_ENTITIES);
    }

    @BeforeAll
    void open() {
        sessionFactory = openDatabase();
        previousDefault = TestSessionFactory.useAsDefault(sessionFactory);
    }

    @AfterAll
    void close() {
        TestSessionFactory.useAsDefault(previousDefault);
        sessionFactory.close();
    }

    @Test
    void createdSchemaValidates() {
        Configuration cfg = validatingConfiguration();
        cfg.setProperty("hibernate.hbm2ddl.auto", "validate");
        try (SessionFactory validated = cfg.buildSessionFactory()) {
            assertNotNull(validated);
        }
    }

    @Test
    void everyEntityIsMappedAndQueryable() {
        DMSSession s = TestSessionFactory.newSession(false);
        try {
            for (Class<?> c : ALL_ENTITIES) {
                List<Object> rows = new java.util.ArrayList<>();
                s.getObjects(c, "where id < 0", rows);
                assertTrue(rows.isEmpty(), c.getSimpleName());
            }
        } finally {
            s.rollback();
        }
    }

    private static PersistedSerializedObject<?> pso(DMSSession s, String name, int collection, byte[] content) {
        PersistedSerializedObject<?> p = new PersistedSerializedObject<>();
        p.setGuid("pso:" + UUID.randomUUID());
        p.setName(name);
        p.setCollectionId(collection);
        p.setEffectiveFrom(new Date());
        p.setBlobContent(s.createBlob(new ByteArrayInputStream(content), content.length));
        return p;
    }

    @Test
    void blobColumnRoundTrips() throws Exception {
        String name = "blob-" + UUID.randomUUID();
        byte[] content = ("a blob: " + "x".repeat(70_000)).getBytes(StandardCharsets.UTF_8);
        DMSSession w = TestSessionFactory.newSession(false);
        w.persist(pso(w, name, PersistedSerializedObject.CollectionID_Queue, content));
        w.commit();

        // read it back as PersistedSerializedObject.getPSO does: load, then stream the blob
        DMSSession r = TestSessionFactory.newSession(false);
        try {
            PersistedSerializedObject<?> loaded = (PersistedSerializedObject<?>) r.getObject(
                    PersistedSerializedObject.class, "where collectionId= :a and name= :b",
                    PersistedSerializedObject.CollectionID_Queue, name);
            assertNotNull(loaded);
            Blob blob = loaded.getBlobContent();
            assertEquals(content.length, blob.length());
            try (InputStream in = blob.getBinaryStream()) {
                assertArrayEquals(content, in.readAllBytes());
            }
        } finally {
            r.rollback();
        }
    }

    @Test
    void workflowQueueNativeQueryRuns() {
        DMSSession w = TestSessionFactory.newSession(false);
        PersistedSerializedObject<?> older = pso(w, "q-older", PersistedSerializedObject.CollectionID_UIJobQueue, new byte[]{1});
        older.setEffectiveFrom(new Date(System.currentTimeMillis() - 60_000));
        PersistedSerializedObject<?> newer = pso(w, "q-newer", PersistedSerializedObject.CollectionID_UIJobQueue, new byte[]{2});
        PersistedSerializedObject<?> other = pso(w, "q-other", PersistedSerializedObject.CollectionID_Queue, new byte[]{3});
        w.persist(older);
        w.persist(newer);
        w.persist(other);
        w.commit();

        DMSSession r = TestSessionFactory.newSession(false);
        try {
            List<String> guids = DumpWorkflowTranscodeQueue.workflowQueueGuids(r);
            assertEquals(List.of(newer.getGuid(), older.getGuid()), guids, "UI job queue, newest first");
        } finally {
            r.rollback();
        }
    }
}
