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
package com.hitorro.util.job;

import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.HTSerializable;

import java.io.IOException;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Jan 25, 2005 Time: 6:06:16 PM
 */
public abstract class JobParameters implements HTSerializable {
    public static final int SerializationVersion = 1;
    // dms session used by object when editing
    private BaseSession _sessionForEditing;

    private String notifyGuid;
    private String notifyGuidState;

    public abstract String getJobName();

    public BaseSession getEditingSession() {
        return _sessionForEditing;
    }

    public void setEditingSession(BaseSession val) {
        _sessionForEditing = val;
    }

    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(JobParameters.SerializationVersion);
        os.writeString(notifyGuid);
        os.writeString(notifyGuidState);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();

        switch (version) {
            case 1:
                notifyGuid = os.readString();
                notifyGuidState = os.readString();
        }

    }

    public String getNotifyGuid() {
        return notifyGuid;
    }

    public void setNotifyGuid(String notifyGuid) {
        this.notifyGuid = notifyGuid;
    }

    public String getNotifyGuidState() {
        return notifyGuidState;
    }

    public void setNotifyGuidState(String notifyGuidState) {
        this.notifyGuidState = notifyGuidState;
    }
}
