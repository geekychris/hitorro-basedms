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

import com.hitorro.base.typesystem.accessors.GuidAccessor;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.BaseType;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;
import com.hitorro.util.typesystem.annotation.UiProperties;
import com.hitorro.util.typesystem.annotation.UiTypeProperties;
import com.hitorro.util.typesystem.annotation.ViewClassReference;

import jakarta.persistence.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 */
@Entity
@Table(name = "category")
@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.Category,
        isView = false,
        isPersisted = true,
        schemaVersion = Category.SerializationVersion,
        softLinkField = "domain:value",
        guidAccessor = GuidAccessor.class)
@UiTypeProperties(name = "Category",
        views = {@ViewClassReference(name = ViewClassReference.ListView, viewClass = Category.CategoryListView.class),
                @ViewClassReference(name = ViewClassReference.EditView, viewClass = Category.CategoryEditView.class)})

public class Category extends BaseType {
    public static final int SerializationVersion = 2;
    public static final String HTEXTERNALCATEGORY = "htcategoryexternal";
    public static final String HTCATEGORY = "htcategory";
    
    @Column(name = "domain")
    private String domain;
    
    @Column(name = "`value`")
    private String value;
    
    @Column(name = "displayName")
    private String displayName;
    
    @Column(name = "description")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;
    
    @Column(name = "externalId")
    private String externalId;
    
    @Column(name = "adapterSource")
    private String adapterSource;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Category> children = new HashSet<Category>();

    public Category() {

    }

    public Category(String domain, String value, String displayName, Category parent) {
        setDomain(domain);
        setValue(value);
        setDisplayName(displayName);
        setParent(parent);
    }


    public Category(String domain, String value, String displayName, String description, Category parent, String externalId, String adapterSource) {
        setDomain(domain);
        setValue(value);
        setDisplayName(displayName);
        setDescription(description);
        setParent(parent);
        setExternalId(externalId);
        setAdapterSource(adapterSource);
    }


    @UiProperties(displayName = "Name", displayType = UiProperties.TextFieldDisplay, order = 10)
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String name) {
        displayName = name;
    }

    public Category getParent() {
        return parent;
    }

    public void setParent(Category s) {
        parent = s;
    }

    @UiProperties(displayName = "Domain", displayType = UiProperties.TextFieldDisplay, order = 20)
    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = StringUtil.lowerCaseIfNotNull(domain);
    }

    @UiProperties(displayName = "Value", displayType = UiProperties.TextFieldDisplay, order = 30)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = StringUtil.lowerCaseIfNotNull(value);
    }

    public Set<Category> getChildren() {
        return children;
    }

    public void setChildren(Set<Category> children) {
        this.children = children;
    }


    @UiProperties(displayName = "Description", displayType = UiProperties.TextFieldDisplay, order = 40)
    public String getDescription() {
        return description;
    }


    public void setDescription(String description) {
        this.description = description;
    }


    public String getExternalId() {
        return externalId;
    }


    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }


    public void serialize(HTObjectOutputStream os)
            throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeString(adapterSource);
        os.writeString(domain);
        os.writeString(value);
        os.writeString(displayName);
        os.writeString(description);
        os.writeVersionedObject(parent);
        os.writeSetOfBaseType(children);
        os.writeString(externalId);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);
        switch (version) {
            case 2:
                adapterSource = os.readString();
            case 1:
                domain = os.readString();
                value = os.readString();
                displayName = os.readString();
                description = os.readString();
                parent = (Category) os.readVersionedObject();
                os.readSetOfHTSerializable(children);
                externalId = os.readString();
        }
    }

    public int getSerializationVersion() {
        return SerializationVersion;
    }

    public boolean hasGuid() {
        return false;
    }

    public String getAdapterSource() {
        return adapterSource;
    }

    public void setAdapterSource(String adapterSource) {
        this.adapterSource = adapterSource;
    }

    public boolean upgradeAllInstances(long currentSchemaVersion) {
        switch ((int) currentSchemaVersion) {
            case 1:
                //upgrade 2-3
                return true;
            default:
                return false;
        }
    }

    /**
     * View class enumerating which fields to show when listing
     */
    @TypeClassMetaInfo(shortTypeName = "CategoryListView",
            isView = true,
            isPersisted = false,
            schemaVersion = Category.SerializationVersion)
    public abstract static class CategoryListView {
        public abstract String getName();

        public abstract String getDomain();

        public abstract String getValue();

        public abstract String getDescription();
    }

    /**
     * View class enumerating which fields to show when editing
     */
    @TypeClassMetaInfo(shortTypeName = "CategoryEditView",
            isView = true,
            isPersisted = false,
            schemaVersion = Category.SerializationVersion)
    public abstract static class CategoryEditView {
        public abstract String getName();

        public abstract String getDomain();

        public abstract String getValue();

        public abstract String getDescription();
    }
}
