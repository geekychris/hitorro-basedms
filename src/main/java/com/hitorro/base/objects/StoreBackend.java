/*
 * Copyright (c) 2006-2026 Chris Collins
 */
package com.hitorro.base.objects;

import com.hitorro.util.io.StoreException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Pluggable backend for a {@link StoreType}. Replaces the inline
 * switch dispatch in {@link Content} — each type gets its own class
 * so adding a sixth backend is "new class + registry entry" instead
 * of "modify two switch statements in an 800-line file", and moving
 * a backend to an optional module (e.g. splitting KVStore into
 * {@code hitorro-basedms-kvstore}) is a mechanical file move plus a
 * ServiceLoader entry.
 *
 * <p>Contract:</p>
 * <ul>
 *   <li>{@link #read} — return the bytes for {@code content}; null
 *       when the content is missing (matches the historical
 *       "no such file" behaviour of the File backend). Throw
 *       {@link StoreException} on real I/O failures.</li>
 *   <li>{@link #write} — copy every byte of {@code is} to the
 *       backend under an {@code assignedId} the caller supplies
 *       (id allocation is DMS orchestration, not backend concern).
 *       MUTATES {@code content} in-place: sets {@code fileName} /
 *       {@code contentSize} / {@code blobContent} as appropriate for
 *       the backend. Returns true on success.</li>
 * </ul>
 *
 * <p>Read-only backends ({@link StoreType#Link},
 * {@link StoreType#Unmanaged}) throw from {@link #write}.</p>
 */
public interface StoreBackend {

    /** Which {@link StoreType} this backend handles. */
    StoreType type();

    /** Open a read stream for {@code content}'s bytes. Returns null
     *  when the underlying object is absent. */
    InputStream read(Store store, Content content) throws StoreException, IOException;

    /** Persist {@code is}'s bytes and mutate {@code content} to
     *  reflect what landed (fileName, size, blob ref, etc.). */
    boolean write(Store store, Content content, String originalFileName,
                  InputStream is, long assignedId) throws StoreException, IOException;
}
