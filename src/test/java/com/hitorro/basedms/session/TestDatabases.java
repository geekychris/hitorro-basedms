package com.hitorro.basedms.session;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** The databases of the matrix, besides the in-memory H2 every test runs on. */
public final class TestDatabases {

    public static final String POSTGRES_IMAGE = "postgres:17";
    public static final String MYSQL_IMAGE = "mysql:8.4";

    private TestDatabases() {
    }

    public static JdbcDatabaseContainer<?> postgres() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }

    public static JdbcDatabaseContainer<?> mysql() {
        return new MySQLContainer(MYSQL_IMAGE);
    }
}
