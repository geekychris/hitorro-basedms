package com.hitorro.base.objects;

import com.hitorro.basedms.session.TestDatabases;
import com.hitorro.basedms.session.TestSessionFactory;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** DmsSchemaTest on PostgreSQL (postgres:17) in a container; skipped without Docker. */
@Testcontainers(disabledWithoutDocker = true)
class DmsSchemaPostgresTest extends DmsSchemaTest {

    @Container
    static final JdbcDatabaseContainer<?> DB = TestDatabases.postgres();

    @Override
    protected SessionFactory openDatabase() {
        return TestSessionFactory.open(DB, ALL_ENTITIES);
    }

    @Override
    protected Configuration validatingConfiguration() {
        return TestSessionFactory.configuration(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword(), ALL_ENTITIES);
    }
}
