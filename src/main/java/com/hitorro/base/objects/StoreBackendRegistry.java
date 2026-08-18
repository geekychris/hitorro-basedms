/*
 * Copyright (c) 2006-2026 Chris Collins
 */
package com.hitorro.base.objects;

import java.util.EnumMap;
import java.util.Map;

/**
 * Registry of {@link StoreBackend} instances keyed by {@link StoreType}.
 * Populated with built-in backends at class load; a future
 * {@code hitorro-basedms-kvstore} module could self-register via a
 * static initialiser or ServiceLoader entry instead of the current
 * always-loaded compile-time wiring.
 *
 * <p>Thread-safe by construction — the map is populated once and
 * never mutated after. Callers get the backend, dispatch to it, done.</p>
 */
public final class StoreBackendRegistry {

    private static final Map<StoreType, StoreBackend> BY_TYPE = new EnumMap<>(StoreType.class);

    static {
        // Built-in backends. If/when KVStore moves to its own module,
        // its registration moves out of here into a ServiceLoader hook
        // and the register(kvBackend) call disappears from this list.
        register(new LinkBackend());
        register(new FileBackend(false));      // handles StoreType.File
        register(new FileBackend(true));       // handles StoreType.Unmanaged (read-only)
        register(new BlobBackend());
        register(new KvStoreBackendAdapter());
    }

    private StoreBackendRegistry() {}

    /** Register (or replace) a backend for its declared {@link StoreType}. */
    public static synchronized void register(StoreBackend backend) {
        BY_TYPE.put(backend.type(), backend);
    }

    /** Look up the backend for a {@link StoreType}. Throws when
     *  no backend is registered — reachable only if the enum grew
     *  a value without a matching registration. */
    public static StoreBackend forType(StoreType type) {
        StoreBackend b = BY_TYPE.get(type);
        if (b == null) {
            throw new IllegalStateException(
                    "no StoreBackend registered for type " + type
                    + " — call StoreBackendRegistry.register(...) at boot");
        }
        return b;
    }
}
