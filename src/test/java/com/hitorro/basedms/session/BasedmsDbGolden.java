package com.hitorro.basedms.session;

import com.hitorro.base.objects.*;
import com.hitorro.basedms.db.HTIntegrator;
import com.hitorro.basedms.PersistableList;
import com.hitorro.basedms.PostFromUrlParameters;
import com.hitorro.basedms.contentconstraints.MimeTypeContentConstraint;
import com.hitorro.basedms.html.HTMLFetcherAppJobParameters;
import com.hitorro.basedms.transformer.TransformJobParameters;
import com.hitorro.basedms.workflow.OutstandingWorkflowItem;
import com.hitorro.basedms.workflow.WorkFlowItem;
import com.hitorro.basedms.workflow.WorkFlowItemEntry;
import com.hitorro.util.job.MessageJobParameters;
import com.hitorro.util.typesystem.HTSerializable;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Generator (hitorro-j3 tools/Jackson2BasedmsDbGolden.java.txt runs it on 3.1.2) and checker for
 * Jackson2DbReadBackTest. Writes a fixed set of DMS objects through DMSSession/Hibernate, dumps the raw rows of every
 * table, and reads the objects back. The same source runs on hitorro-basedms 3.1.2 (to record the
 * golden rows and what 3.1.2 reads from them) and on 4.0.0 (to check that 4.0.0 reads 3.1.2's rows
 * the same way and writes the same rows). Run it with the default time zone set to UTC: TIMESTAMP
 * columns hold the JVM's local time.
 */
public final class BasedmsDbGolden {

    public static final Class<?>[] ALL_ENTITIES = {
            VersionableObject.class, Content.class, Container.class, Document.class, Folder.class,
            Post.class, Forum.class, Category.class, SubjectArea.class, Reference.class,
            RenditionDescriptor.class, PersistedSerializedObject.class, Client.class, ExtractedRelation.class,
            ContentType.class, User.class, Role.class, Permission.class, Store.class, Extension.class,
            DomainInfo.class, NamedLongEntry.class, RssFeedIn.class, ScheduledJob.class,
            WorkFlowItem.class, WorkFlowItemEntry.class, OutstandingWorkflowItem.class,
    };

    /** Columns whose value is the wall-clock time of the write (differ between any two runs). */
    public static final Set<String> CLOCK_COLUMNS = Set.of("creationdate", "modifieddate", "authoreddate");

    private BasedmsDbGolden() {
    }

    public static SessionFactory open(String url, String user, String password) {
        Configuration cfg = new Configuration(
                new BootstrapServiceRegistryBuilder().applyIntegrator(new HTIntegrator()).build());
        cfg.setProperty("hibernate.connection.url", url);
        cfg.setProperty("hibernate.connection.username", user);
        cfg.setProperty("hibernate.connection.password", password);
        cfg.setProperty("hibernate.hbm2ddl.auto", "create-only");
        cfg.setProperty("hibernate.hbm2ddl.halt_on_error", "true");
        for (Class<?> c : ALL_ENTITIES) {
            cfg.addAnnotatedClass(c);
        }
        return cfg.buildSessionFactory();
    }

    public static SessionFactory useAsDefault(SessionFactory sf) throws Exception {
        Field f = com.hitorro.basedms.db.HibernateUtil.class.getDeclaredField("s_session");
        f.setAccessible(true);
        SessionFactory previous = (SessionFactory) f.get(null);
        f.set(null, sf);
        return previous;
    }

    static final String QA_ARGS = "{\"question\":\"What is \\u00e9t\\u00e9?\",\"questions\":[\"a\",\"b\"],\"format\":\"json\",\"n\":1.10,\"deep\":{\"x\":[1,null,true]}}";
    static final String SHEET_ARGS = "{\"sheetIndex\": 0, \"format\":\"object\",\"includeMetadata\":true}";

    /** The PSO payloads: every HTSerializable job-parameter kind basedms queues. */
    static List<HTSerializable> payloads() {
        List<HTSerializable> l = new ArrayList<>();
        TransformJobParameters t = new TransformJobParameters();
        t.setTranformer("tika");
        t.setTransformerMethod("document_qa");
        t.setTransformerMethodArgs(QA_ARGS);
        t.setJobGuid("job:1");
        t.setJobId("id-1");
        t.setTemplateGuid(null);
        t.setAddContentAsChildOfContent(true);
        t.setContentConstraint(new MimeTypeContentConstraint("application/pdf"));
        l.add(t);
        TransformJobParameters t2 = new TransformJobParameters();
        t2.setTranformer("poi");
        t2.setTransformerMethod("spreadsheet_to_json");
        t2.setTransformerMethodArgs(SHEET_ARGS);
        l.add(t2);
        MessageJobParameters m = new MessageJobParameters();
        m.setMessage("hello \u00e9 \uD83D\uDE00 \"quoted\" {\"json\":true}");
        m.setOutputKind(2);
        l.add(m);
        HTMLFetcherAppJobParameters h = new HTMLFetcherAppJobParameters();
        h.setReadUrl("https://example.com/a?b=c");
        h.setRetries(3);
        h.setLabel("label");
        h.setTargetGuid("target:1");
        l.add(h);
        PostFromUrlParameters p = new PostFromUrlParameters();
        p.setReadUrl("https://example.com/post");
        p.setGuid("post:1");
        p.setRetries(1);
        p.setType(4);
        p.setDegree(2);
        p.setParentGuid("parent:1");
        p.setContainerGuid("container:1");
        p.setAuthoredDate(1700000000123L);
        l.add(p);
        PersistableList<HTSerializable> pl = new PersistableList<>();
        List<HTSerializable> inner = new ArrayList<>();
        MessageJobParameters m2 = new MessageJobParameters();
        m2.setMessage("inner");
        inner.add(m2);
        inner.add(h);
        pl.setSerializableObjects(inner);
        l.add(pl);
        return l;
    }

    /** The payload classes must be known to the TypeManager (an app loads them from its type config). */
    public static void registerTypes() {
        com.hitorro.util.typesystem.TypeManager tm = com.hitorro.util.typesystem.TypeManager.getTypeManager();
        for (Class<?> c : new Class<?>[]{TransformJobParameters.class, MessageJobParameters.class, HTMLFetcherAppJobParameters.class,
                PostFromUrlParameters.class, PersistableList.class, MimeTypeContentConstraint.class, ContentSetter.class}) {
            tm.addTypeIfAbsent(c);
        }
    }

    /** Write the fixed objects. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void write() throws Exception {
        registerTypes();
        DMSSession s = new DMSSession(false);
        int i = 0;
        for (HTSerializable o : payloads()) {
            PersistedSerializedObject pso = new PersistedSerializedObject();
            pso.setGuid("pso:" + i);
            pso.setName(o.getClass());
            pso.setCollectionId(i % 2 == 0 ? PersistedSerializedObject.CollectionID_Queue : PersistedSerializedObject.CollectionID_UIJobQueue);
            pso.setPriority(i);
            pso.setExecutor("exec-" + i);
            pso.setEffectiveFrom(new java.util.Date(1700000000000L + i));
            pso.setSerializableObject(o, s);
            s.persist(pso);
            i++;
        }
        // plain entities next to the serialized objects
        Store st = new Store();
        st.setName("main"); st.setStoreType("File"); st.setRootPath("file:/tmp/none"); st.setDocRoot("/docs");
        st.setIsPubliclyVisible(true); st.setDefaultStore(true);
        s.saveOrUpdate(st);
        ContentType ct = new ContentType();
        ct.setGuid("ct:1"); ct.setMimeType("application/json");
        s.saveOrUpdate(ct);
        ScheduledJob sj = new ScheduledJob();
        sj.setGuid("job:sched"); sj.setName("nightly"); sj.setJobName("com.example.Job"); sj.setChronSchedule("0 0 * * * ?");
        sj.setSecondsSchedule(60); sj.setParametersGuid("pso:0");
        Field cg = VersionableObject.class.getDeclaredField("canonicalGuid");   // set by the version API
        cg.setAccessible(true);
        cg.set(sj, "job:sched");
        s.saveOrUpdate(sj);
        Category cat = new Category();
        cat.setDisplayName("Caf\u00e9"); cat.setDomain("topic"); cat.setValue("food"); cat.setDescription("{\"json\":\"in a column\"}");
        s.saveOrUpdate(cat);
        DomainInfo di = new DomainInfo();
        di.setDomain("topic"); di.setDisplayName("Topic"); di.setDescription("d"); di.setValueMapImpl(null);
        s.saveOrUpdate(di);
        s.commit();
    }

    // ------------------------------------------------------------------ dump

    static String sha(byte[] b) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-256").digest(b);
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < 8; k++) sb.append(String.format("%02x", d[k]));
        return b.length + ":" + sb;
    }

    static String esc(String v) {
        return v.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }

    /**
     * Every row of every table, as "table.row.column=TYPE:value". Rows are ordered by all columns
     * (as text) so the order does not depend on the database. Binary values are base64.
     */
    public static Map<String, String> dumpRows(Connection c, boolean maskClock) throws Exception {
        Map<String, String> out = new TreeMap<>();
        List<String> tables = tables(c);
        for (String t : tables) {
            List<String> rows = new ArrayList<>();
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("select * from " + t)) {
                ResultSetMetaData md = rs.getMetaData();
                while (rs.next()) {
                    TreeMap<String, String> cols = new TreeMap<>();
                    for (int k = 1; k <= md.getColumnCount(); k++) {
                        String name = md.getColumnLabel(k).toLowerCase();
                        cols.put(name, cell(rs, k, md.getColumnType(k), maskClock && CLOCK_COLUMNS.contains(name)));
                    }
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, String> e : cols.entrySet()) sb.append(e.getKey()).append('=').append(e.getValue()).append('\u0001');
                    rows.add(sb.toString());
                }
            }
            Collections.sort(rows);
            out.put(t.toLowerCase() + ".count", Integer.toString(rows.size()));
            for (int r = 0; r < rows.size(); r++) {
                for (String kv : rows.get(r).split("\u0001")) {
                    int eq = kv.indexOf('=');
                    out.put(t.toLowerCase() + "." + r + "." + kv.substring(0, eq), esc(kv.substring(eq + 1)));
                }
            }
        }
        return out;
    }

    static String cell(ResultSet rs, int k, int type, boolean mask) throws Exception {
        switch (type) {
            case Types.BLOB: case Types.BINARY: case Types.VARBINARY: case Types.LONGVARBINARY: {
                byte[] b = rs.getBytes(k);
                return b == null ? "null" : "B:" + Base64.getEncoder().encodeToString(b);
            }
            case Types.TIMESTAMP: case Types.TIMESTAMP_WITH_TIMEZONE: case Types.DATE: {
                Timestamp ts = rs.getTimestamp(k);
                if (ts == null) return "null";
                return mask ? "T:<clock>" : "T:" + ts;
            }
            case Types.BIT: case Types.BOOLEAN: {
                boolean v = rs.getBoolean(k);
                return rs.wasNull() ? "null" : "Z:" + v;
            }
            case Types.TINYINT: case Types.SMALLINT: case Types.INTEGER: case Types.BIGINT: {
                long v = rs.getLong(k);
                return rs.wasNull() ? "null" : "J:" + v;
            }
            case Types.FLOAT: case Types.REAL: case Types.DOUBLE: case Types.NUMERIC: case Types.DECIMAL: {
                double v = rs.getDouble(k);
                return rs.wasNull() ? "null" : "D:" + v;
            }
            default: {
                String v = rs.getString(k);
                return v == null ? "null" : "S:" + v;
            }
        }
    }

    static List<String> tables(Connection c) throws Exception {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData md = c.getMetaData();
        try (ResultSet rs = md.getTables(c.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String schema = rs.getString("TABLE_SCHEM");
                if (schema != null && (schema.equalsIgnoreCase("information_schema") || schema.equalsIgnoreCase("pg_catalog"))) continue;
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        Collections.sort(tables, String.CASE_INSENSITIVE_ORDER);
        return tables;
    }

    /** Delete every row of every table (foreign keys off). */
    public static void wipe(Connection c) throws Exception {
        c.setAutoCommit(false);
        String product = c.getMetaData().getDatabaseProductName().toLowerCase();
        try (Statement st = c.createStatement()) {
            if (product.contains("mysql")) st.execute("SET FOREIGN_KEY_CHECKS=0");
            if (product.contains("h2")) st.execute("SET REFERENTIAL_INTEGRITY FALSE");
            if (product.contains("postgres")) st.execute("SET session_replication_role = replica");
            for (String t : tables(c)) st.executeUpdate("delete from " + t);
            if (product.contains("mysql")) st.execute("SET FOREIGN_KEY_CHECKS=1");
            if (product.contains("h2")) st.execute("SET REFERENTIAL_INTEGRITY TRUE");
            if (product.contains("postgres")) st.execute("SET session_replication_role = DEFAULT");
        }
        c.commit();
    }

    /** Insert golden rows (from dumpRows) into the tables of an empty schema. */
    public static void insertRows(Connection c, Map<String, String> golden) throws Exception {
        Map<String, String> actual = new HashMap<>();
        for (String t : tables(c)) actual.put(t.toLowerCase(), t);
        c.setAutoCommit(false);
        try (Statement st = c.createStatement()) {
            String product = c.getMetaData().getDatabaseProductName().toLowerCase();
            if (product.contains("mysql")) st.execute("SET FOREIGN_KEY_CHECKS=0");
            if (product.contains("h2")) st.execute("SET REFERENTIAL_INTEGRITY FALSE");
            if (product.contains("postgres")) st.execute("SET session_replication_role = replica");
        }
        for (Map.Entry<String, String> e : golden.entrySet()) {
            if (!e.getKey().endsWith(".count")) continue;
            String table = e.getKey().substring(0, e.getKey().length() - ".count".length());
            int n = Integer.parseInt(e.getValue());
            for (int r = 0; r < n; r++) {
                String prefix = table + "." + r + ".";
                List<String> cols = new ArrayList<>();
                List<String> vals = new ArrayList<>();
                for (Map.Entry<String, String> g : golden.entrySet()) {
                    if (g.getKey().startsWith(prefix) && g.getKey().indexOf('.', prefix.length()) < 0) {
                        cols.add(g.getKey().substring(prefix.length()));
                        vals.add(g.getValue());
                    }
                }
                String tn = actual.get(table);
                if (tn == null) throw new IllegalStateException("4.0.0 schema has no table " + table);
                StringBuilder sql = new StringBuilder("insert into ").append(tn).append(" (");
                StringBuilder q = new StringBuilder();
                for (int k = 0; k < cols.size(); k++) {
                    sql.append(k == 0 ? "" : ",").append(quote(c, tn, cols.get(k)));
                    q.append(k == 0 ? "?" : ",?");
                }
                sql.append(") values (").append(q).append(")");
                try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                    for (int k = 0; k < vals.size(); k++) bind(ps, k + 1, unesc(vals.get(k)));
                    ps.executeUpdate();
                }
            }
        }
        try (Statement st = c.createStatement()) {
            String product = c.getMetaData().getDatabaseProductName().toLowerCase();
            if (product.contains("mysql")) st.execute("SET FOREIGN_KEY_CHECKS=1");
            if (product.contains("h2")) st.execute("SET REFERENTIAL_INTEGRITY TRUE");
            if (product.contains("postgres")) st.execute("SET session_replication_role = DEFAULT");
        }
        c.commit();
    }

    /** The column as the database stores its name (H2 upper-cases unquoted names, keeps quoted ones). */
    static String quote(Connection c, String table, String col) throws SQLException {
        String q = c.getMetaData().getIdentifierQuoteString();
        try (ResultSet rs = c.getMetaData().getColumns(c.getCatalog(), null, table, "%")) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                if (name.equalsIgnoreCase(col)) return q + name + q;
            }
        }
        throw new IllegalStateException("4.0.0 schema has no column " + table + "." + col);
    }

    static String unesc(String v) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length(); i++) {
            char ch = v.charAt(i);
            if (ch == '\\' && i + 1 < v.length()) {
                char n = v.charAt(++i);
                sb.append(n == 'n' ? '\n' : n == 'r' ? '\r' : n);
            } else sb.append(ch);
        }
        return sb.toString();
    }

    static void bind(PreparedStatement ps, int k, String v) throws Exception {
        if (v.equals("null")) {
            ps.setObject(k, null);
            return;
        }
        String body = v.substring(2);
        switch (v.charAt(0)) {
            case 'B': ps.setBytes(k, Base64.getDecoder().decode(body)); break;
            case 'T': ps.setTimestamp(k, Timestamp.valueOf(body)); break;
            case 'Z': ps.setBoolean(k, Boolean.parseBoolean(body)); break;
            case 'J': ps.setLong(k, Long.parseLong(body)); break;
            case 'D': ps.setDouble(k, Double.parseDouble(body)); break;
            default: ps.setString(k, body);
        }
    }

    // ------------------------------------------------------------------ read back

    /** What the code reads back: every PSO and its deserialized payload, field by field. */
    @SuppressWarnings("rawtypes")
    public static Map<String, String> readBack() throws Exception {
        registerTypes();
        Map<String, String> out = new TreeMap<>();
        DMSSession s = new DMSSession(false);
        try {
            List<Object> rows = new ArrayList<>();
            s.getObjects(PersistedSerializedObject.class, "where id > 0", rows);
            rows.sort(Comparator.comparing(o -> ((PersistedSerializedObject) o).getGuid()));
            out.put("pso.count", Integer.toString(rows.size()));
            for (Object o : rows) {
                PersistedSerializedObject pso = (PersistedSerializedObject) o;
                String k = "pso." + pso.getGuid() + ".";
                out.put(k + "name", String.valueOf(pso.getName()));
                out.put(k + "collection", Integer.toString(pso.getCollectionId()));
                out.put(k + "priority", Integer.toString(pso.getPriority()));
                out.put(k + "executor", String.valueOf(pso.getExecutor()));
                out.put(k + "effectiveFrom", Long.toString(pso.getEffectiveFrom().getTime()));
                Object payload;
                try {
                    payload = pso.getSerializableObject(s);
                } catch (Throwable t) {
                    payload = "!" + t.getClass().getSimpleName();
                }
                out.put(k + "payload", esc(describe(payload, 0)));
                if (payload instanceof TransformJobParameters) {
                    // the stored transformer arguments, parsed as the transformer methods parse them
                    String args = ((TransformJobParameters) payload).getTransformerMethodArgs();
                    out.put(k + "args.qa", esc(parsed(com.hitorro.basedms.transformer.methods.DocumentQATransformer.class, args)));
                    out.put(k + "args.summary", esc(parsed(com.hitorro.basedms.transformer.methods.DocumentSummarizerTransformer.class, args)));
                    out.put(k + "args.sheet", esc(parsed(com.hitorro.basedms.transformer.methods.SpreadsheetToJSONTransformer.class, args)));
                    for (String key : new String[]{"question", "questions", "format", "n", "deep", "sheetIndex", "missing"}) {
                        out.put(k + "args.get." + key, esc(getParameter(args, key)));
                    }
                }
            }
            for (Class<?> c : new Class<?>[]{Store.class, ContentType.class, ScheduledJob.class, Category.class, DomainInfo.class}) {
                List<Object> es = new ArrayList<>();
                s.getObjects(c, "where id > 0", es);
                out.put("entity." + c.getSimpleName() + ".count", Integer.toString(es.size()));
                List<String> ds = new ArrayList<>();
                for (Object e : es) ds.add(describeEntity(e));
                Collections.sort(ds);
                for (int n = 0; n < ds.size(); n++) out.put("entity." + c.getSimpleName() + "." + n, esc(ds.get(n)));
            }
        } finally {
            s.rollback();
        }
        return out;
    }

    /** An entity's persistent fields (collections and references by size/guid; clock dates omitted). */
    static String describeEntity(Object o) throws Exception {
        StringBuilder sb = new StringBuilder(o.getClass().getSimpleName()).append("{");
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = o.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) continue;
                fields.add(f);
            }
        }
        fields.sort(Comparator.comparing(f -> f.getDeclaringClass().getName() + "." + f.getName()));
        for (Field f : fields) {
            f.setAccessible(true);
            Object v = f.get(o);
            String d;
            if (v == null) d = "null";
            else if (v instanceof java.util.Date) d = "Date";
            else if (v instanceof Collection) d = "size:" + ((Collection<?>) v).size();
            else if (v instanceof String || v instanceof Number || v instanceof Boolean || v instanceof Enum) d = v.getClass().getSimpleName() + ":" + v;
            else d = v.getClass().getSimpleName();
            sb.append(f.getName()).append('=').append(d).append(',');
        }
        return sb.append('}').toString();
    }

    static String parsed(Class<?> transformer, String args) {
        try {
            Object t = transformer.getDeclaredConstructor().newInstance();
            java.lang.reflect.Method m = transformer.getDeclaredMethod("parseParameters", String.class);
            m.setAccessible(true);
            return describe(m.invoke(t, args), 0);
        } catch (Throwable e) {
            return "!" + e.getClass().getSimpleName();
        }
    }

    static String getParameter(String args, String key) {
        try {
            java.lang.reflect.Method m = com.hitorro.basedms.transformer.methods.BaseTransformMethod.class
                    .getDeclaredMethod("getParameter", String.class, String.class, String.class);
            m.setAccessible(true);
            return String.valueOf(m.invoke(new com.hitorro.basedms.transformer.methods.SpreadsheetToJSONTransformer(), args, key, "<default>"));
        } catch (Throwable e) {
            return "!" + e.getClass().getSimpleName();
        }
    }

    static String describe(Object o, int depth) throws Exception {
        if (o == null) return "null";
        if (o instanceof String || o instanceof Number || o instanceof Boolean || o instanceof Character || o instanceof Enum) return o.getClass().getSimpleName() + ":" + o;
        if (o instanceof java.util.Date) return "Date:" + ((java.util.Date) o).getTime();
        if (o instanceof Collection) {
            StringBuilder sb = new StringBuilder("[");
            for (Object e : (Collection<?>) o) sb.append(describe(e, depth + 1)).append(";");
            return sb.append("]").toString();
        }
        if (depth > 3) return o.getClass().getName() + "{...}";
        StringBuilder sb = new StringBuilder(o.getClass().getName()).append("{");
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = o.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                fields.add(f);
            }
        }
        fields.sort(Comparator.comparing(f -> f.getDeclaringClass().getName() + "." + f.getName()));
        for (Field f : fields) {
            f.setAccessible(true);
            Object v = f.get(o);
            if (v instanceof DMSSession || (v != null && v.getClass().getName().contains("Session"))) continue;
            sb.append(f.getName()).append("=").append(describe(v, depth + 1)).append(",");
        }
        return sb.append("}").toString();
    }
}
