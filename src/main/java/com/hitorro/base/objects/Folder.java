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


import com.hitorro.basedms.session.DMSSession;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;
import com.hitorro.util.typesystem.annotation.UiProperties;
import com.hitorro.util.typesystem.annotation.UiTypeProperties;
import com.hitorro.util.typesystem.annotation.ViewClassReference;
import org.hibernate.query.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Basic, raw, container of arbitrary objects. This kind of container is used in our current rudimentary workflow.
 */
@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.Folder,
        isView = false,
        isPersisted = true,
        schemaVersion = Folder.SerializationVersion)
@UiTypeProperties(name = "Folder",
        views = {@ViewClassReference(name = ViewClassReference.ListView, viewClass = Folder.FolderListView.class),
                @ViewClassReference(name = ViewClassReference.EditView, viewClass = Folder.FolderEditView.class)})
public class Folder extends Container {
    public static final int SerializationVersion = 1;
    private static final String FolderForNameQuery = "select bl from " + Folder.class.getName() + " as bl where name = :name";
    private static final String FolderForNameIsRootQuery = "select bl from " + Folder.class.getName() + " as bl where name = :name and isRootLevel=true";
    private static final String AllFolderNamesQuery = "select bl.folderName from " + Folder.class.getName() + " as bl";
    private String name;
    private boolean isRootLevel;

    public Folder() {
        super(VersionableObject.class, "id");
    }

    /**
     * Fetch a Folder by its name. This method assumes that a transaction is currently open
     *
     * @param session    the hibernate session
     * @param folderName the name of the folder to fetch
     * @return the Folder, which will be null if not found
     */
    public static Folder getFolderForName(DMSSession session, String folderName) {
        return getFolderForNameRoot(session, folderName, FolderForNameQuery);
    }

    private static Folder getFolderForNameRoot(DMSSession session, String folderName, String query) {
        Query qq = session.createQuery(query);
        Folder bl = null;
        qq.setParameter("name", folderName);
        List result = qq.list();
        if (result != null && result.size() > 0) {
            bl = (Folder) result.get(0);
        }
        return bl;
    }

    public static Folder getFolderForName(DMSSession session, String folderName, boolean root) {
        if (root) {
            return getFolderForNameRoot(session, folderName, FolderForNameIsRootQuery);
        } else {
            return getFolderForName(session, folderName);
        }
    }

    /**
     * Get all available folder names.
     *
     * @param session the session used to talk to the database
     * @return an array containing all folder names
     */
    public static List<String> getFolderNames(DMSSession session) {
        List<String> result = new ArrayList<String>();
        Iterator itr = session.getIteratorFromQuery(AllFolderNamesQuery);
        while (itr.hasNext()) {
            Object obj = itr.next();
            result.add((String) obj);
        }

        return result;
    }

    public void copy(VersionableObject orig) {
        super.copy(orig);
        Folder other = (Folder) orig;
        name = other.name;
    }

    public void delete(DMSSession session) {
        removeCategoryFromAllSubordinates(session);
        super.delete(session);
    }

    @UiProperties(displayName = "Name", displayType = UiProperties.TextFieldDisplay)
    public String getName() {
        return name;
    }

    public void setName(String folderName) {
        name = folderName;
    }

    public boolean getIsRootLevel() {
        return isRootLevel;
    }

    public void setIsRootLevel(boolean flag) {
        isRootLevel = flag;
    }

    @UiProperties(displayName = "Entries", displayType = UiProperties.DetailListDisplay)
    public List<VersionableObject> getFolderEntries() {
        return super.getList();
    }

    public void serialize(HTObjectOutputStream os)
            throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);

        os.writeString(name);
        os.writeBoolean(isRootLevel);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);
        switch (version) {
            case 1:
                name = os.readString();
                isRootLevel = os.readBoolean();
        }
    }

    /**
     * View class enumerating which fields to show when listing.
     */
    @TypeClassMetaInfo(shortTypeName = "FolderListView",
            isView = true,
            isPersisted = false,
            schemaVersion = Folder.SerializationVersion)
    public abstract static class FolderListView {
        public abstract String getName();
    }

    /**
     * View class enumerating which fields to show when editing.
     */
    @TypeClassMetaInfo(shortTypeName = "FolderEditView",
            isView = true,
            isPersisted = false,
            schemaVersion = Folder.SerializationVersion)
    public abstract static class FolderEditView {
        public abstract String getName();
        //public abstract List<VersionableObject> getFolderEntries ();
    }
}
