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

import com.hitorro.util.core.ListValue;
import com.hitorro.util.core.ListValue.ListValueSource;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;
import com.hitorro.util.typesystem.annotation.UiProperties;
import com.hitorro.util.typesystem.annotation.UiTypeProperties;
import com.hitorro.util.typesystem.annotation.ViewClassReference;

import java.io.IOException;

/**
 * Parameters for the MessageJob scheduled task.
 */
@TypeClassMetaInfo(shortTypeName = "MessageJobParameters",
        isView = false,
        isPersisted = true,
        schemaVersion = 1)
@UiTypeProperties(name = "Message Job Parameters", views = {@ViewClassReference(name = ViewClassReference.ListView, viewClass = MessageJobParameters.MessageJobParametersListView.class),
        @ViewClassReference(name = ViewClassReference.EditView, viewClass = MessageJobParameters.MessageJobParametersEditView.class)})
public class MessageJobParameters extends JobParameters implements ListValueSource {
    // constants for message kinds
    public static final int ToConsoleMessage = 8;
    public static final int ToLogMessage = 9;
    private static final int SerializationVersion = 1;
    private String _message;
    private int _outputKind;

    public MessageJobParameters() {
        _message = "";
        _outputKind = ToConsoleMessage;
    }


    public String getJobName() {
        return MessageJob.MessageJob;
    }

    @UiProperties(displayName = "Message", displayType = UiProperties.TextFieldDisplay)
    public String getMessage() {
        return _message;
    }

    public void setMessage(String msg) {
        _message = msg;
    }

    @UiProperties(displayName = "Output type", displayType = UiProperties.SelectListDisplay)
    public int getOutputKind() {
        return _outputKind;
    }

    public void setOutputKind(int okind) {
        _outputKind = okind;
    }

    // ----------------- ListValueSource
    public ListValue[] getValues(Object obj, String fieldName, String tag) {
        if (!(obj instanceof MessageJobParameters)) {
            return null;
        }

        // assume the field name is "outputKind" because that's the only select listFiles we have
        ListValue[] result = new ListValue[2];
        result[0] = new ListValue("Console", ToConsoleMessage);
        result[1] = new ListValue("Log", ToLogMessage);

        return result;
    }

    // ----------------- HTSerializable

    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeInt(_outputKind);
        os.writeString(_message);
    }

    public void deserialize(HTObjectInputStream os) throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);
        switch (version) {
            case 1:
                _outputKind = os.readInt();
                _message = os.readString();
        }
    }

    public int getSerializationVersion() {
        return SerializationVersion;
    }

    public boolean isPersisted() {
        return false;
    }

    public boolean hasGuid() {
        return false;
    }

    public boolean hasSoftGuid() {
        return false;
    }

    /**
     * View class enumerating which fields to show when listing.
     */
    @TypeClassMetaInfo(shortTypeName = "MessageJobParametersListView",
            isView = true,
            isPersisted = false,
            schemaVersion = MessageJobParameters.SerializationVersion)
    public abstract static class MessageJobParametersListView {
        public abstract String getMessage();
    }

    /**
     * View class enumerating which fields to show when editing.
     */
    @TypeClassMetaInfo(shortTypeName = "MessageJobParametersEditView",
            isView = true,
            isPersisted = false,
            schemaVersion = MessageJobParameters.SerializationVersion)
    public abstract static class MessageJobParametersEditView {
        public abstract String getMessage();

        public abstract int getOutputKind();
    }
}
