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

import com.hitorro.base.typesystem.GuidBaseType;
import com.hitorro.base.typesystem.accessors.GuidAccessor;
import com.hitorro.basedms.BaseTypeOnTriggerGeneric;
import com.hitorro.basedms.db.JDBCUtils;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.util.core.classes.ClassUtil;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.annotation.ImplClassMeta;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;

import jakarta.persistence.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <p/>
 * Persist a non persisted versioned object.  Can be used for such things as workflow, persisted queue, etc
 */
@Entity
@Table(name = "persistedserializedobject")
@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.PersistedSerializedObject,
        onTriggers = {@ImplClassMeta(className = BaseTypeOnTriggerGeneric.class, trigger = com.hitorro.util.typesystem.OnTrigger.TriggerType.OnNew),
                @ImplClassMeta(className = BaseTypeOnTriggerGeneric.class, trigger = com.hitorro.util.typesystem.OnTrigger.TriggerType.BeforeSave),
                @ImplClassMeta(className = BaseTypeOnTriggerGeneric.class, trigger = com.hitorro.util.typesystem.OnTrigger.TriggerType.BeforeDelete),
                @ImplClassMeta(className = BaseTypeOnTriggerGeneric.class, trigger = com.hitorro.util.typesystem.OnTrigger.TriggerType.BeforePersist),
                @ImplClassMeta(className = BaseTypeOnTriggerGeneric.class, trigger = com.hitorro.util.typesystem.OnTrigger.TriggerType.OnLoad)},
        isView = false,
        isPersisted = true,
        schemaVersion = PersistedSerializedObject.SerializationVersion,
        guidAccessor = GuidAccessor.class)
public class PersistedSerializedObject<T extends com.hitorro.util.typesystem.HTSerializable> extends GuidBaseType {

    public static final int CollectionID_Null = 0;

    public static final int CollectionID_MemeConversation = 10;
    public static final int CollectionID_Queue = 20;
    public static final int CollectionID_QueueItemDone = 21;

    public static final int CollectionID_ScheduledJob = 30;

    public static final int CollectionID_HTMLQueue = 40;

    public static final int CollectionID_TranscoderQueue = 50;

    public static final int CollectionID_NotificationQueue = 60;

    public static final int CollectionID_UIJobQueue = 70;


    public static final int SerializationVersion = 3;

    @Lob
    @Column(name = "blobContent")
    private Blob blobContent;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "collectionId")
    private int collectionId;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "effectiveFrom")
    private Date effectiveFrom = new Date();
    
    @Column(name = "executor")
    private String executor;

    @Transient
    private com.hitorro.util.typesystem.HTSerializable object = null;
    
    @Column(name = "priority")
    private int priority;


    public static com.hitorro.util.typesystem.HTSerializable getPSO(DMSSession session, String setName, int collectionId)
            throws SQLException, IOException, ClassNotFoundException, StoreException {
        Object obj = session.getObject(PersistedSerializedObject.class, "where collectionId= :a and name= :b",
                collectionId, setName);
        PersistedSerializedObject wrapper = (PersistedSerializedObject) obj;

        return wrapper.getSerializableObject(session);
    }

    public static List<PersistedSerializedObject> getObjectsByName(DMSSession session, String name) {
        List<PersistedSerializedObject> results = new ArrayList<PersistedSerializedObject>();
        session.getObjects(PersistedSerializedObject.class, "where name= :a", results, name);

        return results;
    }

    public Date getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Date date) {
        effectiveFrom = date;
    }

    public String getName() {
        return name;
    }

    /**
     * Convenience function to set the name by the class of the object serialized. We'll use the short version of the
     * class name as the name.
     *
     * @param clazz The class being
     */
    public void setName(Class clazz) {
        setName(ClassUtil.getBareName(clazz));
    }

    /**
     * The name of this object. Typically the names are <b>not</b> unique, but represent the kind of thing this is.
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    public void setSerializableObject(T pts, com.hitorro.util.typesystem.BaseSession session)
            throws IOException, StoreException {
        object = null;
        ByteArrayOutputStream baos = FileUtil.getByteArrayOutputStream(5 * 1024);
        com.hitorro.util.typesystem.HTObjectOutputStream os = new com.hitorro.util.typesystem.HTObjectOutputStreamImpl(session, baos,
                com.hitorro.util.typesystem.TypeManager.getTypeManager(),
                false);
        os.writeVersionedObject(pts);
        os.flush();
        baos.close();
        byte[] array = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(array);
        Blob blob = session.createBlob(bais, array.length);
        this.setBlobContent(blob);
    }


    public com.hitorro.util.typesystem.HTSerializable getObject()
            throws SQLException, IOException, ClassNotFoundException, StoreException {
        if (object == null) {
            object = getSerializableObject();
        }
        return object;
    }

    public com.hitorro.util.typesystem.HTSerializable getSerializableObject()
            throws SQLException, IOException, StoreException, ClassNotFoundException {
        return getSerializableObject(getSession());
    }

    public void setSerializableObject(T pts)
            throws IOException, StoreException {
        setSerializableObject(pts, getSession());
    }

    public T getSerializableObject(com.hitorro.util.typesystem.BaseSession session)
            throws SQLException, IOException, StoreException, ClassNotFoundException {
        Blob blob = getBlobContent();
        if (blob == null || blob.length() == 0) {
            return null;
        }

        InputStream bis = blob.getBinaryStream();
        com.hitorro.util.typesystem.HTObjectInputStream is = new com.hitorro.util.typesystem.HTObjectInputStreamImpl(bis,
                com.hitorro.util.typesystem.TypeManager.getTypeManager(),
                session);
        T pts = (T) is.readVersionedObject();
        bis.close();
        return pts;
    }

    public int getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(int id) {
        collectionId = id;
    }

    Blob getBlobContent() {
        return blobContent;
    }

    void setBlobContent(Blob blob) {
        this.blobContent = blob;
    }

    public int getSerializationVersion() {
        return SerializationVersion;
    }

    public String getExecutor() {
        return executor;
    }

    public void setExecutor(String executor) {
        this.executor = executor;
    }

    public boolean upgradeAllInstances(long currentSchemaVersion) {
        switch ((int) currentSchemaVersion) {
            case 2:
                //upgrade 1-2
                JDBCUtils.performSimpleJDBCUpdate("update persistedserializedobject set priority=0");
                return true;
            default:
                return false;
        }
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
