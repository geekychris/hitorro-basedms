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

import com.hitorro.base.typesystem.btadapter.rssadapters.DocumentRssItemAdapter;
import com.hitorro.base.typesystem.btadapter.rssadapters.VersionableObjectRssItem;
import com.hitorro.util.core.hash.FPHash64;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;

import jakarta.persistence.*;

import java.io.IOException;

@Entity
@Table(name = "document")
@PrimaryKeyJoinColumn(name = "system_id")
@com.hitorro.util.typesystem.annotation.TypeClassMetaInfo(shortTypeName = com.hitorro.util.typesystem.annotation.TypeClassMetaInfo.Document,
        adapters = {@com.hitorro.util.typesystem.annotation.AdapterClassMeta(className = DocumentRssItemAdapter.class, adapterGroup = VersionableObjectRssItem.AdapterGroup)},
        isView = false,
        isPersisted = true,
        schemaVersion = Document.SerializationVersion)
@com.hitorro.util.typesystem.annotation.UiTypeProperties(name = "Document",
        views = {@com.hitorro.util.typesystem.annotation.ViewClassReference(name = com.hitorro.util.typesystem.annotation.ViewClassReference.SearchView, viewClass = Post.DocumentSearchView.class)})
public class Document extends VersionableObject implements Cloneable {
    public static final int SerializationVersion = 2;

    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "titleHash")
    private long titleHash;
    
    // the author of the post (if known)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id")
    private User author;

    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "System Id", displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextFieldDisplay)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void copy(VersionableObject orig) {
        super.copy(orig);
        Document origD = (Document) orig;
        // title
        title = origD.title;
        titleHash = origD.titleHash;
        author = origD.author;
    }

    public long getTitleHash() {
        return titleHash;
    }

    protected void setTitleHash(long hash) {
        titleHash = hash;
    }

    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "Author", displayType = com.hitorro.util.typesystem.annotation.UiProperties.VersionableObjectDisplay)
    public User getAuthor() {
        return author;
    }

    public void setAuthor(User val) {
        author = val;
    }

    @com.hitorro.util.typesystem.annotation.FullTextAttributeMetaInfo(displayName = "title",
            isFullTextIndexable = true, luceneIndexingFilters = "STANDARD,CASE,PORTERSTEM",
            luceneFieldName = "title", stringLiteral = false, allField = true)
    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "Title", displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextFieldDisplay, order = 20)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        setTitleHash(FPHash64.getFP(title));
    }

    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        // version 2
        os.writeLong(titleHash);
        os.writeVersionedObject(author);

        // version 1
        os.writeString(title);
    }

    /**
     * @param os
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);

        switch (version) {
            case 2:
                titleHash = os.readLong();
                author = (User) os.readVersionedObject();
            case 1:

                // read content
                title = os.readString();
        }
    }

    public boolean upgradeAllInstances(long currentSchemaVersion) {
        switch ((int) currentSchemaVersion) {
            case 2:
                //upgrade 2-3
                return true;
            default:
                return false;
        }
    }

    @com.hitorro.util.typesystem.annotation.TypeClassMetaInfo(shortTypeName = "DocumentSearchView",
            isView = true,
            isPersisted = false,
            schemaVersion = Document.SerializationVersion)
    public abstract static class DocumentSearchView extends VersionableObjectSearchView {
        @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "Title", displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextFieldDisplay)
        public abstract String getTitle();

        @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "Content", displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextFieldDisplay)
        public abstract String getContent();
    }
}