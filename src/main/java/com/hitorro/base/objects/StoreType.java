/*
 * Copyright (c) 2006-2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hitorro.base.objects;

import java.util.HashMap;
import java.util.Map;


public enum StoreType {
    Blob(false, true, false, false),
    File(true, false, false, false),
    Unmanaged(false, false, false, true),
    Link(false, false, true, false);

    private static Map<String, StoreType> s_map = null;

    private boolean isFileStore;
    private boolean isUnmanagedFileStore;
    private boolean isBlobStore;
    private boolean isLinkStore;

    StoreType(boolean file, boolean blob, boolean link, boolean unmanaged) {
        isFileStore = file;
        isBlobStore = blob;
        isLinkStore = link;
        isUnmanagedFileStore = unmanaged;
    }

    public static StoreType get(String type) {
        if (s_map == null) {
            s_map = new HashMap<String, StoreType>();
            for (StoreType s : StoreType.values()) {
                s_map.put(s.name().toLowerCase(), s);
            }
        }
        return s_map.get(type.toLowerCase());
    }

    private void init() {

    }

    public boolean isFileStore() {
        return isFileStore;
    }

    public boolean isUnmanagedFileStore() {
        return isUnmanagedFileStore;
    }

    public boolean isBlobStore() {
        return isBlobStore;
    }

    public boolean isLinkStore() {
        return isLinkStore;
    }
}
