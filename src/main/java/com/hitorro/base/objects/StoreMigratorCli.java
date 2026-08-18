/*
 * Copyright (c) 2006-2026 Chris Collins
 */
package com.hitorro.base.objects;

import com.hitorro.util.io.StoreException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Standalone entry point for {@link StoreMigrator} — bypasses the
 * usual Hibernate-loaded Store lifecycle so an operator can move
 * content between DMS backends without a running application.
 *
 * <p>Usage:</p>
 * <pre>{@code
 *   java -cp hitorro-basedms.jar:hitorro-kvstore.jar:... \
 *       com.hitorro.base.objects.StoreMigratorCli \
 *       --source-type File   --source-root /var/dms/legacy \
 *       --dest-type   KVStore --dest-root  /var/dms/rocksdb
 * }</pre>
 *
 * <p>Constructs both {@link Store}s from CLI args — no DB, no
 * Hibernate. After the byte-copy completes, the operator still needs
 * to run a SQL {@code UPDATE content SET store_id = ?} to point
 * existing Content rows at the new Store; this tool doesn't touch
 * DB rows (it just moves bytes).</p>
 *
 * <p>Supports the same direction matrix as {@link StoreMigrator}:
 * File ↔ KVStore, File → File, KVStore → KVStore. Blob targets
 * throw — Blob needs a JDBC session, out of scope for a standalone
 * mover.</p>
 */
public final class StoreMigratorCli {

    /** Exit codes — 0 success, 1 usage error, 2 migration error. */
    public static final int EXIT_OK           = 0;
    public static final int EXIT_USAGE        = 1;
    public static final int EXIT_MIGRATE_FAIL = 2;

    private StoreMigratorCli() {}

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /** Testable entry — parse args, run migration, return exit code.
     *  Prints progress + report to {@code out}, errors to {@code err}. */
    public static int run(String[] args, java.io.PrintStream out, java.io.PrintStream err) {
        if (args.length == 0 || hasFlag(args, "--help") || hasFlag(args, "-h")) {
            printUsage(out);
            return args.length == 0 ? EXIT_USAGE : EXIT_OK;
        }

        Map<String, String> flags;
        try {
            flags = parseFlags(args);
        } catch (IllegalArgumentException e) {
            err.println("error: " + e.getMessage());
            printUsage(err);
            return EXIT_USAGE;
        }

        String srcType  = require(flags, "--source-type", err);
        String srcRoot  = require(flags, "--source-root", err);
        String dstType  = require(flags, "--dest-type",   err);
        String dstRoot  = require(flags, "--dest-root",   err);
        if (srcType == null || srcRoot == null || dstType == null || dstRoot == null) {
            return EXIT_USAGE;
        }

        try {
            Store source = buildStore(srcType, srcRoot, "cli-source");
            Store dest   = buildStore(dstType, dstRoot, "cli-dest");

            out.println("StoreMigrator CLI");
            out.println("  source: " + srcType + "  " + srcRoot);
            out.println("  dest:   " + dstType + "  " + dstRoot);
            out.println("Running…");

            long t0 = System.currentTimeMillis();
            StoreMigrator.Report report = StoreMigrator.migrate(source, dest);
            long dtMs = System.currentTimeMillis() - t0;

            out.println();
            out.println("Result:");
            out.println("  copied:  " + report.copied());
            out.println("  skipped: " + report.skipped());
            out.println("  errors:  " + report.errors().size());
            out.println("  elapsed: " + dtMs + "ms");
            if (!report.errors().isEmpty()) {
                out.println();
                out.println("First errors (up to 10):");
                report.errors().stream().limit(10).forEach(e -> out.println("  - " + e));
                return EXIT_MIGRATE_FAIL;
            }
            KvStoreBackend.closeAll();  // release RocksDB locks before JVM exit
            return EXIT_OK;

        } catch (StoreException e) {
            err.println("migration failed: " + e.getMessage());
            return EXIT_MIGRATE_FAIL;
        } catch (Exception e) {
            err.println("unexpected error: " + e);
            return EXIT_MIGRATE_FAIL;
        }
    }

    /** Build a Store from CLI-supplied type + rootPath. Store.init() is
     *  invoked manually — same pattern as the integration tests. */
    static Store buildStore(String storeType, String rootPath, String name) {
        Store s = new Store();
        s.setName(name);
        s.setStoreType(storeType);
        // Accept both bare paths (auto-prefix "file:") and already-schemed URIs.
        s.setRootPath(rootPath.contains("://") || rootPath.startsWith("file:")
                ? rootPath : "file:" + rootPath);
        s.setIsPubliclyVisible(false);
        s.init();
        return s;
    }

    static Map<String, String> parseFlags(String[] args) {
        Map<String, String> out = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (!a.startsWith("--")) {
                throw new IllegalArgumentException("unexpected positional: " + a);
            }
            if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                throw new IllegalArgumentException("flag " + a + " needs a value");
            }
            out.put(a, args[++i]);
        }
        return out;
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String a : args) if (a.equals(flag)) return true;
        return false;
    }

    private static String require(Map<String, String> flags, String key, java.io.PrintStream err) {
        String v = flags.get(key);
        if (v == null || v.isBlank()) {
            err.println("missing required flag: " + key);
            return null;
        }
        return v;
    }

    private static void printUsage(java.io.PrintStream out) {
        out.println("Usage:");
        out.println("  java -cp <classpath> " + StoreMigratorCli.class.getName() + " \\");
        out.println("       --source-type <File|KVStore>  --source-root <path>  \\");
        out.println("       --dest-type   <File|KVStore>  --dest-root   <path>");
        out.println();
        out.println("Flags:");
        out.println("  --source-type   Source StoreType (File | KVStore)");
        out.println("  --source-root   Source rootPath — bare local path or file:/… URI");
        out.println("  --dest-type     Destination StoreType");
        out.println("  --dest-root     Destination rootPath");
        out.println("  --help, -h      Show this message");
        out.println();
        out.println("Exit codes: 0 = success, 1 = usage error, 2 = migration error.");
        out.println("Blob is not supported by this CLI (needs JDBC session). See ");
        out.println("StoreMigrator.migrate() from within an app for Blob paths.");
    }
}
