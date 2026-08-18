/*
 * Copyright (c) 2006-2026 Chris Collins
 */
package com.hitorro.base.objects;

import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.io.IOUtil;
import com.hitorro.util.io.StoreException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * {@link StoreType#File} + {@link StoreType#Unmanaged}. Two instances
 * of the same class handle both because their read path is identical
 * — only Unmanaged rejects writes (external bytes; DMS is a reader).
 */
final class FileBackend implements StoreBackend {

    /** true for {@link StoreType#Unmanaged} — read-only view. */
    private final boolean unmanaged;

    FileBackend(boolean unmanaged) {
        this.unmanaged = unmanaged;
    }

    @Override
    public StoreType type() {
        return unmanaged ? StoreType.Unmanaged : StoreType.File;
    }

    @Override
    public InputStream read(Store store, Content content) throws StoreException, IOException {
        BaseFile file = store.getRootPathPath().getChild(content.getFileName());
        if (!BaseFile.notNullAndExists(file)) return null;
        return file.getDataInputStream();
    }

    @Override
    public boolean write(Store store, Content content, String originalFileName,
                         InputStream is, long assignedId) throws StoreException, IOException {
        if (unmanaged) {
            throw new StoreException(
                    "Cannot set content for a content object with store type of UnmanagedLink");
        }
        String fn = FileUtil.idToHexPath(assignedId);
        if (store.getIsPubliclyVisible()) {
            String ext = FileUtil.getFileExtension(originalFileName);
            if (!StringUtil.nullOrEmptyOrBlankString(ext)) {
                fn = Fmt.S("%s.%s", fn, ext);
            }
        }
        content.setFileName(fn);
        BaseFile destFile = store.getRootPathPath().getChild(fn);
        if (!destFile.mkParentDir()) {
            throw new StoreException(Fmt.S("Unable to write file %s", destFile.getAbsolutePath()));
        }
        try (OutputStream os = destFile.getDataOutputStream()) {
            IOUtil.copyStream(is, os);
            os.flush();
        }
        content.setContentSize(destFile.length());
        return true;
    }
}
