package com.hitorro.base.objects;

import com.hitorro.basedms.session.TestDatabases;
import com.hitorro.basedms.session.TestSessionFactory;
import org.hibernate.SessionFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** VersionTreeDeleteTest on PostgreSQL (postgres:17) in a container; skipped without Docker. */
@Testcontainers(disabledWithoutDocker = true)
class VersionTreeDeletePostgresTest extends VersionTreeDeleteTest {

    @Container
    static final JdbcDatabaseContainer<?> DB = TestDatabases.postgres();

    @Override
    protected SessionFactory openDatabase() {
        return TestSessionFactory.open(DB);
    }

    @Override
    protected void closeDatabase(SessionFactory sf) {
        sf.close();
    }
}
