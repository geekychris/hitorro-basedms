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

import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.BaseType;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;

import jakarta.persistence.*;
import java.io.IOException;

/**
 * Represents an extracted relation between entities found in text/content.
 * Used for natural language processing and relationship extraction.
 */
@Entity
@Table(name = "ExtractedRelation")
@TypeClassMetaInfo(
    shortTypeName = "ER",
    isView = false,
    isPersisted = true,
    schemaVersion = ExtractedRelation.SerializationVersion
)
public class ExtractedRelation extends BaseType {
    public static final int SerializationVersion = 1;
    
    @Column(name = "sourceGuid")
    private String sourceGuid;
    
    @Column(name = "subjectEntity")
    private String subjectEntity;
    
    @Column(name = "predicateRelation")
    private String predicateRelation;
    
    @Column(name = "objectEntity")
    private String objectEntity;
    
    @Column(name = "confidence")
    private double confidence;
    
    @Column(name = "extractionMethod")
    private String extractionMethod;

    public ExtractedRelation() {
    }

    public String getSourceGuid() {
        return sourceGuid;
    }

    public void setSourceGuid(String sourceGuid) {
        this.sourceGuid = sourceGuid;
    }

    public String getSubjectEntity() {
        return subjectEntity;
    }

    public void setSubjectEntity(String subjectEntity) {
        this.subjectEntity = subjectEntity;
    }

    public String getPredicateRelation() {
        return predicateRelation;
    }

    public void setPredicateRelation(String predicateRelation) {
        this.predicateRelation = predicateRelation;
    }

    public String getObjectEntity() {
        return objectEntity;
    }

    public void setObjectEntity(String objectEntity) {
        this.objectEntity = objectEntity;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getExtractionMethod() {
        return extractionMethod;
    }

    public void setExtractionMethod(String extractionMethod) {
        this.extractionMethod = extractionMethod;
    }

    @Override
    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeString(sourceGuid);
        os.writeString(subjectEntity);
        os.writeString(predicateRelation);
        os.writeString(objectEntity);
        os.writeDouble(confidence);
        os.writeString(extractionMethod);
    }

    @Override
    public void deserialize(HTObjectInputStream is) throws IOException, ClassNotFoundException, StoreException {
        int version = is.readInt();
        super.deserialize(is);
        switch (version) {
            case 1:
                sourceGuid = is.readString();
                subjectEntity = is.readString();
                predicateRelation = is.readString();
                objectEntity = is.readString();
                confidence = is.readDouble();
                extractionMethod = is.readString();
                break;
        }
    }

    @Override
    public int getSerializationVersion() {
        return SerializationVersion;
    }
}
