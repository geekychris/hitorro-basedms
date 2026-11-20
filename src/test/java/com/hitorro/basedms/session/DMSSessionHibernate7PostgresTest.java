package com.hitorro.basedms.session;

import org.hibernate.SessionFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** DMSSessionHibernate7Test on PostgreSQL (postgres:17) in a container; skipped without Docker. */
@Testcontainers(disabledWithoutDocker = true)
class DMSSessionHibernate7PostgresTest extends DMSSessionHibernate7Test {

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
