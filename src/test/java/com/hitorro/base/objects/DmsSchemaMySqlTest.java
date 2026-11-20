package com.hitorro.base.objects;

import com.hitorro.basedms.session.TestDatabases;
import com.hitorro.basedms.session.TestSessionFactory;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** DmsSchemaTest on MySQL (mysql:8.4) in a container; skipped without Docker. */
@Testcontainers(disabledWithoutDocker = true)
class DmsSchemaMySqlTest extends DmsSchemaTest {

    @Container
    static final JdbcDatabaseContainer<?> DB = TestDatabases.mysql();

    @Override
    protected SessionFactory openDatabase() {
        return TestSessionFactory.open(DB, ALL_ENTITIES);
    }

    @Override
    protected Configuration validatingConfiguration() {
        return TestSessionFactory.configuration(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword(), ALL_ENTITIES);
    }
}
