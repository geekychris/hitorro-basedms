package com.hitorro.basedms.session;

import org.hibernate.SessionFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** DMSSessionHibernate7Test on MySQL (mysql:8.4) in a container; skipped without Docker. */
@Testcontainers(disabledWithoutDocker = true)
class DMSSessionHibernate7MySqlTest extends DMSSessionHibernate7Test {

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
