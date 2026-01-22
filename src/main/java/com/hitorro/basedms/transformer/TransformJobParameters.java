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
package com.hitorro.basedms.transformer;

import com.hitorro.base.objects.Content;
import com.hitorro.base.objects.ContentSetter;
import com.hitorro.util.core.opers.HTPredicate;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.job.JobParameters;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.HTSerializable;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;

import java.io.IOException;

@TypeClassMetaInfo(shortTypeName = "TransformJobParams", isView = false, isPersisted = false, schemaVersion = TransformJobParameters.SerializationVersion)
public class TransformJobParameters extends JobParameters implements HTSerializable {
    public static final int SerializationVersion = 3;

    private ContentSetter contentSetter;
    private HTPredicate<Content> contentConstraint;
    private String jobGuid;
    private String jobId;
    private String transformer;
    private String transformerMethod;
    private String transformerMethodArgs;
    private boolean addContentAsChildOfContent;
    private String templateGuid;

    /**
     * When you create the job, you must provision a job id, this id is used to
     * track the files on disk as they go
     * through the transform process.
     */
    public void provisionId() {
        setJobId(TransformJob.getId());
    }

    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);

        os.writeBoolean(addContentAsChildOfContent);
        os.writeVersionedObject(getContentSetter());
        os.writeVersionedObject((HTSerializable) getContentConstraint());
        os.writeString(getJobGuid());
        os.writeString(getJobId());
        os.writeString(transformer);
        os.writeString(transformerMethod);
        os.writeString(transformerMethodArgs);
        os.writeString(templateGuid);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);
        switch (version) {
            case 3:
                addContentAsChildOfContent = os.readBoolean();
                setContentSetter((ContentSetter) os.readVersionedObject());
                setContentConstraint((HTPredicate<Content>) os.readVersionedObject());
                setJobGuid(os.readString());
                setJobId(os.readString());
                transformer = os.readString();
                transformerMethod = os.readString();
                transformerMethodArgs = os.readString();
                templateGuid = os.readString();
                break;
            case 2:
                addContentAsChildOfContent = os.readBoolean();
            case 1:
                setContentSetter((ContentSetter) os.readVersionedObject());
                setContentConstraint((HTPredicate<Content>) os.readVersionedObject());
                setJobGuid(os.readString());
                setJobId(os.readString());
                transformer = os.readString();
                transformerMethod = os.readString();
                transformerMethodArgs = os.readString();
                break;
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

    public ContentSetter getContentSetter() {
        return contentSetter;
    }

    public void setContentSetter(ContentSetter contentSetter) {
        this.contentSetter = contentSetter;
    }

    public HTPredicate<Content> getContentConstraint() {
        return contentConstraint;
    }

    public void setContentConstraint(HTPredicate<Content> contentConstraint) {
        this.contentConstraint = contentConstraint;
    }

    public String getJobGuid() {
        return jobGuid;
    }

    public void setJobGuid(String jobGuid) {
        this.jobGuid = jobGuid;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getTranformer() {
        return transformer;
    }

    public void setTranformer(String transformer) {
        this.transformer = transformer;
    }

    public String getTransformerMethod() {
        return transformerMethod;
    }

    public void setTransformerMethod(String transformerMethod) {
        this.transformerMethod = transformerMethod;
    }

    public String getTransformerMethodArgs() {
        return transformerMethodArgs;
    }

    public void setTransformerMethodArgs(String transformerMethodArgs) {
        this.transformerMethodArgs = transformerMethodArgs;
    }

    public String getJobName() {
        return TransformJob.TransformerAppJob;
    }

    public boolean getAddContentAsChildOfContent() {
        return addContentAsChildOfContent;
    }

    public void setAddContentAsChildOfContent(boolean flag) {
        addContentAsChildOfContent = flag;
    }

    public String getTemplateGuid() {
        return templateGuid;
    }

    public void setTemplateGuid(String templateGuid) {
        this.templateGuid = templateGuid;
    }
}
