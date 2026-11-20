package com.hitorro.base.objects;

import com.hitorro.basedms.session.TestDatabases;
import com.hitorro.basedms.session.TestSessionFactory;
import org.hibernate.SessionFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** VersionTreeDeleteTest on MySQL (mysql:8.4) in a container; skipped without Docker. */
@Testcontainers(disabledWithoutDocker = true)
class VersionTreeDeleteMySqlTest extends VersionTreeDeleteTest {

    @Container
    static final JdbcDatabaseContainer<?> DB = TestDatabases.mysql();

    @Override
    protected SessionFactory openDatabase() {
        return TestSessionFactory.open(DB);
    }

    @Override
    protected void closeDatabase(SessionFactory sf) {
        sf.close();
    }
}
