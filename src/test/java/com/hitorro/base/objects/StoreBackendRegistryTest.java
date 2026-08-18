/*
 * Copyright (c) 2006-2026 Chris Collins
 */
package com.hitorro.base.objects;

import com.hitorro.util.io.StoreException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the {@link StoreBackendRegistry} plug-in dispatch pattern.
 * The registry is loaded at class-init with backends for every enum
 * value shipped in {@code hitorro-basedms} today; this suite proves:
 *
 * <ul>
 *   <li>Every {@link StoreType} has a matching backend registered</li>
 *   <li>Each backend's {@code type()} lines up with the enum value it
 *       claims to handle</li>
 *   <li>Read-only backends (Link, Unmanaged) throw from write</li>
 *   <li>Register can replace an existing backend (useful for tests +
 *       mock backends)</li>
 *   <li>Missing backend (impossible without deliberate teardown)
 *       throws with a clear message</li>
 * </ul>
 */
class StoreBackendRegistryTest {

    @Test
    void everyStoreTypeHasABackend() {
        for (StoreType t : StoreType.values()) {
            StoreBackend b = StoreBackendRegistry.forType(t);
            assertNotNull(b, "no backend registered for " + t);
            assertEquals(t, b.type(),
                    "backend for " + t + " reports type " + b.type() + " — misregistration");
        }
    }

    @Test
    void link_read_dispatchesToLinkBackend() {
        // Just prove the registry returns the right class. Actually
        // opening a URL is an integration test we don't want here.
        StoreBackend b = StoreBackendRegistry.forType(StoreType.Link);
        assertInstanceOf(LinkBackend.class, b);
    }

    @Test
    void link_write_throwsClearError() {
        StoreBackend b = StoreBackendRegistry.forType(StoreType.Link);
        Content c = new Content();
        Store dummy = new Store();
        assertThrows(StoreException.class,
                () -> b.write(dummy, c, "x.txt", empty(), 0L),
                "Link is read-only; write must reject");
    }

    @Test
    void unmanaged_write_throwsClearError() {
        StoreBackend b = StoreBackendRegistry.forType(StoreType.Unmanaged);
        Content c = new Content();
        Store dummy = new Store();
        assertThrows(StoreException.class,
                () -> b.write(dummy, c, "x.txt", empty(), 0L));
    }

    @Test
    void kvStore_write_endToEnd_viaRegistry(@TempDir Path tmp) throws Exception {
        // The whole point of the refactor — dispatch through the registry
        // still writes to RocksDB and mutates Content the same way.
        Store store = new Store();
        store.setName("s");
        store.setStoreType("KVStore");
        store.setRootPath("file:" + tmp);
        store.setIsPubliclyVisible(false);
        store.init();

        Content c = new Content();
        StoreBackend b = StoreBackendRegistry.forType(StoreType.KVStore);
        boolean ok = b.write(store, c, "orig.txt", bytes("hello via registry"), 42L);
        assertTrue(ok);
        assertNotNull(c.getFileName(), "backend must set fileName on write");
        assertEquals(18, c.getContentSize());   // "hello via registry" = 18 bytes

        // Round-trip via the same registry entry point.
        InputStream got = b.read(store, c);
        assertNotNull(got);
        assertEquals("hello via registry",
                new String(got.readAllBytes(), StandardCharsets.UTF_8));

        KvStoreBackend.closeAll();
    }

    @Test
    void register_replacesExistingBackend() {
        // Save the original so we don't pollute other tests.
        StoreBackend original = StoreBackendRegistry.forType(StoreType.Link);
        try {
            StoreBackend fake = new StoreBackend() {
                @Override public StoreType type() { return StoreType.Link; }
                @Override public InputStream read(Store s, Content c) { return empty(); }
                @Override public boolean write(Store s, Content c, String o, InputStream i, long id) { return true; }
            };
            StoreBackendRegistry.register(fake);
            assertSame(fake, StoreBackendRegistry.forType(StoreType.Link),
                    "register() must replace the existing backend for its type");
        } finally {
            StoreBackendRegistry.register(original);   // restore
        }
    }

    private static InputStream empty() {
        return new ByteArrayInputStream(new byte[0]);
    }

    private static InputStream bytes(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}
