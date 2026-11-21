package com.hitorro.basedms.session;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Jackson2DbReadBackTest on MySql (TestDatabases.mysql()) in a container; skipped without Docker. */
@Testcontainers(disabledWithoutDocker = true)
class Jackson2DbReadBackMySqlTest extends Jackson2DbReadBackTest {

    @Container
    static final JdbcDatabaseContainer<?> DB = TestDatabases.mysql();

    @Override
    protected String db() {
        return "mysql";
    }

    @Override
    protected String url() {
        return DB.getJdbcUrl();
    }

    @Override
    protected String user() {
        return DB.getUsername();
    }

    @Override
    protected String password() {
        return DB.getPassword();
    }
}
