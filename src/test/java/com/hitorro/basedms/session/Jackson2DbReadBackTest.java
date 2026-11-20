package com.hitorro.basedms.session;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Data written by hitorro-basedms 3.1.2 (Jackson 2) reads back the same with 4.0.0 (Jackson 3), and
 * 4.0.0 writes the same rows. {@code jackson2-db-3.1.2.txt} was recorded by running
 * {@link BasedmsDbGolden} on the published 3.1.2 jar, on H2, PostgreSQL 17 and MySQL 8.4: the raw
 * rows of every table (persisted serialized objects holding every job-parameter kind, including
 * JSON transformer arguments, plus plain entities) and what 3.1.2 read back from them (each
 * deserialized payload field by field, the stored transformer arguments parsed as the transformer
 * methods parse them with Jackson, and the entities).
 * <p>
 * Here 4.0.0 creates the schema, writes the same objects (the rows must equal 3.1.2's, apart from
 * the wall-clock columns), then the table contents are replaced by 3.1.2's rows and read back.
 * On H2 here; on PostgreSQL and MySQL in the subclasses.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Jackson2DbReadBackTest {

    private TimeZone previousZone;
    private SessionFactory sessionFactory;
    private SessionFactory previousDefault;

    /** Database name in the golden file. */
    protected String db() {
        return "h2";
    }

    protected String url() {
        return "jdbc:h2:mem:basedms-jackson2-golden;DB_CLOSE_DELAY=-1";
    }

    protected String user() {
        return "sa";
    }

    protected String password() {
        return "";
    }

    @BeforeAll
    void open() throws Exception {
        previousZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));   // as the golden file was recorded
        sessionFactory = BasedmsDbGolden.open(url(), user(), password());
        previousDefault = BasedmsDbGolden.useAsDefault(sessionFactory);
    }

    @AfterAll
    void close() throws Exception {
        BasedmsDbGolden.useAsDefault(previousDefault);
        sessionFactory.close();
        TimeZone.setDefault(previousZone);
    }

    private Map<String, String> golden(String section) throws Exception {
        String prefix = db() + "." + section + ".";
        Map<String, String> m = new TreeMap<>();
        try (InputStream in = getClass().getResourceAsStream("jackson2-db-3.1.2.txt")) {
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (!line.startsWith(prefix)) continue;
                int eq = line.indexOf('=');
                m.put(line.substring(prefix.length(), eq), line.substring(eq + 1));
            }
        }
        return m;
    }

    private static void assertSame(Map<String, String> expected, Map<String, String> actual) {
        for (Map.Entry<String, String> e : expected.entrySet()) {
            assertEquals(e.getValue(), actual.get(e.getKey()), e.getKey());
        }
        assertEquals(expected.keySet(), actual.keySet());
    }

    @Test
    @Order(1)
    void writesTheRowsThat312Wrote() throws Exception {
        BasedmsDbGolden.write();
        Map<String, String> expected = new TreeMap<>();
        for (Map.Entry<String, String> e : golden("rows").entrySet()) {
            String col = e.getKey().substring(e.getKey().lastIndexOf('.') + 1);
            boolean clock = BasedmsDbGolden.CLOCK_COLUMNS.contains(col) && e.getValue().startsWith("T:");
            expected.put(e.getKey(), clock ? "T:<clock>" : e.getValue());
        }
        try (Connection c = DriverManager.getConnection(url(), user(), password())) {
            assertSame(expected, BasedmsDbGolden.dumpRows(c, true));
        }
    }

    @Test
    @Order(2)
    void readsTheRowsThat312Wrote() throws Exception {
        try (Connection c = DriverManager.getConnection(url(), user(), password())) {
            BasedmsDbGolden.wipe(c);
            BasedmsDbGolden.insertRows(c, golden("rows"));
            assertSame(golden("rows"), BasedmsDbGolden.dumpRows(c, false));
        }
        assertSame(golden("read"), BasedmsDbGolden.readBack());
    }
}
