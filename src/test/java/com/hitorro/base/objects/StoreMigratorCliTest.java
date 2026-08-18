/*
 * Copyright (c) 2006-2026 Chris Collins
 */
package com.hitorro.base.objects;

import com.hitorro.util.basefile.fs.BaseFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static com.hitorro.base.objects.StoreMigratorCli.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link StoreMigratorCli}. Uses the {@link #run(String[],
 * PrintStream, PrintStream)} testable entry so we can capture stdout/
 * stderr and assert on exit codes without spawning a subprocess.
 */
class StoreMigratorCliTest {

    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(stdout);
    private final PrintStream err = new PrintStream(stderr);

    @AfterEach
    void tearDown() {
        KvStoreBackend.closeAll();
    }

    private String out()  { return stdout.toString(StandardCharsets.UTF_8); }
    private String err_() { return stderr.toString(StandardCharsets.UTF_8); }

    // ---- Usage / arg parsing ----

    @Test
    void noArgs_printsUsage_returnsUsageError() {
        int rc = run(new String[]{}, out, err);
        assertEquals(EXIT_USAGE, rc);
        assertTrue(out().contains("Usage:"));
    }

    @Test
    void helpFlag_printsUsage_returnsOk() {
        int rc = run(new String[]{"--help"}, out, err);
        assertEquals(EXIT_OK, rc);
        assertTrue(out().contains("Usage:"));
    }

    @Test
    void missingRequiredFlag_returnsUsage() {
        int rc = run(new String[]{
                "--source-type", "File", "--source-root", "/tmp/x"
                // no --dest-*
        }, out, err);
        assertEquals(EXIT_USAGE, rc);
        assertTrue(err_().contains("missing required flag"));
    }

    @Test
    void flagWithoutValue_returnsUsage() {
        int rc = run(new String[]{"--source-type", "--dest-type", "File"}, out, err);
        assertEquals(EXIT_USAGE, rc);
    }

    @Test
    void positionalArg_returnsUsage() {
        int rc = run(new String[]{"migrate", "--source-type", "File"}, out, err);
        assertEquals(EXIT_USAGE, rc);
        assertTrue(err_().contains("unexpected positional"));
    }

    @Test
    void parseFlags_extractsKeyValuePairs() {
        var m = parseFlags(new String[]{"--a", "1", "--b", "two"});
        assertEquals("1",   m.get("--a"));
        assertEquals("two", m.get("--b"));
    }

    // ---- End-to-end runs ----

    @Test
    void fileToKv_endToEnd_copiesEveryContent(@TempDir Path tmp) throws Exception {
        // Seed a File-backed source with two contents.
        Path srcRoot = tmp.resolve("src");
        Store src = StoreMigratorCli.buildStore("File", srcRoot.toString(), "src");
        seedFile(src, 100L, "alpha");
        seedFile(src, 200L, "beta");

        int rc = run(new String[]{
                "--source-type", "File",    "--source-root", srcRoot.toString(),
                "--dest-type",   "KVStore", "--dest-root",   tmp.resolve("dst").toString(),
        }, out, err);

        assertEquals(EXIT_OK, rc, "expected clean exit; err=" + err_());
        String o = out();
        assertTrue(o.contains("copied:  2"));
        assertTrue(o.contains("errors:  0"));
    }

    @Test
    void kvToFile_endToEnd_copiesEveryContent(@TempDir Path tmp) throws Exception {
        Path srcRoot = tmp.resolve("src");
        Store src = StoreMigratorCli.buildStore("KVStore", srcRoot.toString(), "src");
        KvStoreBackend.writeContent(src, "a.txt", 1L,
                new java.io.ByteArrayInputStream("alpha".getBytes(StandardCharsets.UTF_8)));
        KvStoreBackend.writeContent(src, "b.txt", 2L,
                new java.io.ByteArrayInputStream("beta".getBytes(StandardCharsets.UTF_8)));
        // Close so the reopen below sees the flushed data.
        KvStoreBackend.closeAll();

        int rc = run(new String[]{
                "--source-type", "KVStore", "--source-root", srcRoot.toString(),
                "--dest-type",   "File",    "--dest-root",   tmp.resolve("dst").toString(),
        }, out, err);

        assertEquals(EXIT_OK, rc, "expected clean exit; err=" + err_());
        assertTrue(out().contains("copied:  2"));
    }

    @Test
    void filePrefix_isAccepted_andPassedThrough(@TempDir Path tmp) throws Exception {
        // Rootpath supplied with an explicit "file:" prefix should work
        // identically to a bare path.
        Path srcRoot = tmp.resolve("src");
        Store src = StoreMigratorCli.buildStore("File", srcRoot.toString(), "src");
        seedFile(src, 42L, "hello");

        int rc = run(new String[]{
                "--source-type", "File", "--source-root", "file:" + srcRoot,
                "--dest-type", "KVStore", "--dest-root", "file:" + tmp.resolve("dst"),
        }, out, err);
        assertEquals(EXIT_OK, rc);
        assertTrue(out().contains("copied:  1"));
    }

    @Test
    void unsupportedPair_returnsMigrateFail(@TempDir Path tmp) {
        // Blob as destination — StoreMigrator throws. CLI translates to
        // EXIT_MIGRATE_FAIL + a diagnostic to stderr.
        int rc = run(new String[]{
                "--source-type", "File",  "--source-root", tmp.resolve("src").toString(),
                "--dest-type",   "Blob",  "--dest-root",   "irrelevant",
        }, out, err);
        assertEquals(EXIT_MIGRATE_FAIL, rc);
        assertTrue(err_().contains("migration failed"));
    }

    // ---- Helper ----

    private static void seedFile(Store store, long id, String payload) throws Exception {
        String fn = com.hitorro.util.io.FileUtil.idToHexPath(id);
        BaseFile out = store.getRootPathPath().getChild(fn);
        assertTrue(out.mkParentDir());
        try (OutputStream os = out.getDataOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }
    }
}
