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
import com.hitorro.base.typesystem.btadapter.rssadapters.VersionableObjectRssItem;
import com.hitorro.basedms.Log;
import com.hitorro.basedms.cache.ContentTypeCache;
import com.hitorro.basedms.contentconstraints.FileNameMatchContentConstraint;
import com.hitorro.basedms.contentconstraints.TagConstraint;
import com.hitorro.basedms.exceptions.CategoryException;
import com.hitorro.basetext.indexer.DomainValueIndexerAdapter;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.core.Constants;
import com.hitorro.util.core.ListUtil;
import com.hitorro.util.core.ListValue;
import com.hitorro.util.core.ListValue.ListValueSource;
import com.hitorro.util.core.opers.HTPredicate;
import com.hitorro.util.core.opers.LogicalOrOperator;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.core.valuemap.DomainValueIntf;
import com.hitorro.util.core.valuemap.ValueMap;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.versioning.VersioningUtil;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Entity
@Table(name = "versionable_object")
@Inheritance(strategy = InheritanceType.JOINED)
@FilterDef(name = "statusFilter", parameters = @ParamDef(name = "statusParam", type = String.class))
@Filter(name = "statusFilter", condition = ":statusParam=realm")
@com.hitorro.util.typesystem.annotation.TypeClassMetaInfo(shortTypeName = com.hitorro.util.typesystem.annotation.TypeClassMetaInfo.VersionableObject,
        onTriggers = {@com.hitorro.util.typesystem.annotation.ImplClassMeta(className = VersionableObjectOnTriggerGeneric.class, trigger = com.hitorro.util.typesystem.OnTrigger.TriggerType.OnNew),
                @com.hitorro.util.typesystem.annotation.ImplClassMeta(className = VersionableObjectOnTriggerGeneric.class, trigger = com.hitorro.util.typesystem.OnTrigger.TriggerType.BeforeSave),
                @com.hitorro.util.typesystem.annotation.ImplClassMeta(className = VersionableObjectOnTriggerGeneric.class, trigger = com.hitorro.util.typesystem.OnTrigger.TriggerType.BeforeDelete),
                @com.hitorro.util.typesystem.annotation.ImplClassMeta(className = VersionableObjectOnTriggerGeneric.class, trigger = com.hitorro.util.typesystem.OnTrigger.TriggerType.BeforePersist),
                @com.hitorro.util.typesystem.annotation.ImplClassMeta(className = VersionableObjectOnTriggerGeneric.class, trigger = com.hitorro.util.typesystem.OnTrigger.TriggerType.OnLoad)},
        adapters = {@com.hitorro.util.typesystem.annotation.AdapterClassMeta(className = VersionableObjectRssItem.class, adapterGroup = VersionableObjectRssItem.AdapterGroup)},
        isView = false,
        isPersisted = true,
        schemaVersion = VersionableObject.SerializationVersion,
        softLinkField = "guid",
        guidAccessor = GuidAccessor.class)
@com.hitorro.util.typesystem.annotation.UiTypeProperties(name = "System Object",
        views = {@com.hitorro.util.typesystem.annotation.ViewClassReference(name = com.hitorro.util.typesystem.annotation.ViewClassReference.SearchView, viewClass = VersionableObject.VersionableObjectSearchView.class)})
public class VersionableObject extends GuidBaseType implements com.hitorro.util.typesystem.HTSerializableClone<VersionableObject>, com.hitorro.basedms.CategoryBaseInterface, ListValueSource {
    public static final String TypeOrSubtypeKey = "objectTypeSubclasses";
    public static final String TypeKey = "objectType";

    public static final String CreateDateKey = "createDate";
    public static final String ModifiedDateKey = "modifiedDate";
    public static final String AuthoredDateKey = "authoredDate";


    public static final int SerializationVersion = 4;
    // CATEGORIES
    public static final String CategoriesKey = "categories";
    
    @Column(name = "creationDate", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    protected Date creationDate = new Date();
    
    @Column(name = "modifiedDate", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    protected Date modifiedDate = new Date();
    
    @Column(name = "authoredDate", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    protected Date authoredDate = new Date();
    
    @Column(name = "note")
    protected String note = null;
    
    @Column(name = "creator", length = 80)
    protected String creator = null;
    
    @Column(name = "effectiveUser", length = 80)
    protected String effectiveUser = null;
    
    @Column(name = "realm", length = 80)
    protected String realm = null;
    
    @Column(name = "versionLabel", nullable = false)
    protected String versionLabel = new String("1.0");
    
    @Column(name = "canonicalGuid", nullable = false)
    @org.hibernate.annotations.Index(name = "canonguid_idx")
    protected String canonicalGuid = null;
    
    // root version is the canonical version
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "canonical")
    protected VersionableObject canonical = null;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nextVersion")
    protected VersionableObject nextVersion = null;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branchVersion")
    protected VersionableObject branchVersion = null;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parentVersion")
    protected VersionableObject parentVersion = null;

    // index fields
    @Column(name = "shouldIndex")
    protected boolean shouldIndex = false;
    
    @Column(name = "isIndexed")
    protected boolean isIndexed = false;
    
    @Column(name = "indexName")
    protected String indexName = null;

    @Column(name = "identityHash")
    @org.hibernate.annotations.Index(name = "identityHash_idx")
    protected long identityHash;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "versionableobject_category", joinColumns = @JoinColumn(name = "system_id"))
    @AttributeOverrides({
        @AttributeOverride(name = "domain", column = @Column(name = "domain")),
        @AttributeOverride(name = "value", column = @Column(name = "value"))
    })
    protected Set<DomainValueIntf> categories = new HashSet<DomainValueIntf>();
    
    @Column(name = "state")
    private int state;
    
    @Column(name = "adapterSource")
    private String adapterSource;
    
    @Transient
    private String content;
    
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "versionableobject_contents",
        joinColumns = @JoinColumn(name = "system_id"),
        inverseJoinColumns = @JoinColumn(name = "content_id")
    )
    private Set<Content> contents = new HashSet<Content>();
    
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "versionableobject_container",
        joinColumns = @JoinColumn(name = "system_id"),
        inverseJoinColumns = @JoinColumn(name = "container_id")
    )
    private Set<Container> containers = new HashSet<Container>();
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owningContainer")
    private Container owningContainer = null;

    public VersionableObject() {
        //this(true);
        state = Constants.ActiveState;
    }

    public VersionableObject(boolean fireNewTrigger) {
        executeNewTrigger(fireNewTrigger);
        state = Constants.ActiveState;
    }

    @com.hitorro.util.typesystem.annotation.FullTextAttributeMetaInfo(displayName = "identityHash",
            isFullTextIndexable = true, luceneIndexingFilters = "STANDARD,CASE,PORTERSTEM",
            luceneFieldName = "identityHash", stringLiteral = true, allField = false, stored = true)
    public long getIdentityHash() {
        return identityHash;
    }

    public void setIdentityHash(long hash) {
        identityHash = hash;
    }

    @com.hitorro.util.typesystem.annotation.FullTextAttributeMetaInfo(displayName = TypeKey,
            isFullTextIndexable = true, luceneIndexingFilters = "STANDARD,CASE",
            luceneFieldName = TypeKey, stringLiteral = false, allField = false, stored = true)
    public String getObjectType() {
        com.hitorro.util.typesystem.Type t = com.hitorro.util.typesystem.TypeManager.getTypeManager().getTypeForClass(this.getClass());
        if (t == null) {
            return null;
        }
        return t.getName();
    }

    @com.hitorro.util.typesystem.annotation.FullTextAttributeMetaInfo(displayName = TypeOrSubtypeKey,
            isFullTextIndexable = true, luceneIndexingFilters = "STANDARD,CASE",
            luceneFieldName = TypeOrSubtypeKey, stringLiteral = false, allField = false, stored = true)
    public String getObjectTypeSubclasses() {
        com.hitorro.util.typesystem.Type t = com.hitorro.util.typesystem.TypeManager.getTypeManager().getTypeForClass(this.getClass());
        if (t == null) {
            return null;
        }
        return t.getSubclassString();
    }

    @com.hitorro.util.typesystem.annotation.FullTextAttributeMetaInfo(displayName = "indexName",
            isFullTextIndexable = true, luceneIndexingFilters = "STANDARD,CASE,PORTERSTEM",
            luceneFieldName = "indexName", stringLiteral = true, allField = false, stored = true)
    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String name) {
        indexName = name;
    }

    public boolean getIsIndexed() {
        return isIndexed;
    }

    public void setIsIndexed(boolean flag) {
        isIndexed = flag;
    }

    /**
     * Fluff all content up with any properties we can derive from the file itself.
     */
    public void fluffAllContent() {
        for (Content cont : contents) {
            cont.fluff();
        }
    }

    /**
     * Check to see if object is part of a specific realm or if the object is not realm "tied".  If that
     * is the case there is a test.
     *
     * @param realm
     * @return
     */
    public boolean isMemberOfRealm(String realm, boolean includeRealmlessObjects) {
        if (includeRealmlessObjects && realm == null) {
            return true;
        }
        return StringUtil.equals(realm, realm, true);
    }

    /**
     * @param constraint - content constraint to filter upon.
     */
    public void fluffContentByConstraint(HTPredicate<Content> constraint) {
        for (Content content : contents) {
            if (constraint.test(content)) {
                content.fluff();
            }
        }
    }

    public boolean getShouldIndex() {
        return shouldIndex;
    }

    public void setShouldIndex(boolean flag) {
        shouldIndex = flag;
    }

    /**
     * Deleting myself requires removing any references
     *
     * @param session
     */
    public void delete(com.hitorro.util.typesystem.BaseSession session) {
        for (Content c : contents) {
            c.decrementRefCount();
            if (c.getReferenceCount() == 0) {
                c.delete(session);
            }
        }
        super.delete(session);
    }

    @com.hitorro.util.typesystem.annotation.FullTextAttributeMetaInfo(displayName = CategoriesKey,
            isFullTextIndexable = true, luceneIndexingFilters = "STANDARD,CASE,PORTERSTEM",
            luceneFieldName = CategoriesKey, stringLiteral = true, allField = false, stored = true,
            indexerClass = DomainValueIndexerAdapter.class)
    public Set<DomainValueIntf> getCategories() {
        return categories;
    }

    public void setCategories(Set<DomainValueIntf> cat) {
        categories = cat;
    }

    /**
     * Remove a container from this set,
     *
     * @param c
     * @return if it exists return true
     */
    public boolean removeContainer(Container c) {
        return categories.remove(c);
    }

    public void addCategory(String domain, String value) throws CategoryException {
        com.hitorro.basedms.CategoryBaseUtil.addCategory(domain, value, this);
    }

    public Container getOwningContainer() {
        return owningContainer;
    }

    public void setOwningContainer(Container container) {
        owningContainer = container;
    }

    public void processUnique(ValueMap<Category> cats, String domain, String value) {
        if (cats.isUniqueOverSystemVersions()) {
            // if we are something like a label that has to be unique over all versions then we must look for
            // previous fodder.  For now I assume we cannot use a query since the tree may still be in memory and
            // not persisted yet.
            com.hitorro.basedms.UniqueLabelVersionableObjectVisitor visitor = new com.hitorro.basedms.UniqueLabelVersionableObjectVisitor(this, domain, value);
            visitVersionTree(visitor);
        }
    }

    /**
     * Evaluates if the domain value pair exists for this system object.
     *
     * @param domain
     * @param value
     * @return true if exists.
     */
    public boolean getCategoryValueExists(String domain, String value) {
        return com.hitorro.basedms.CategoryBaseUtil.getCategoryValueExists(domain, value, this);
    }

    /**
     * Remove a category if it exists.
     *
     * @param domain
     * @param value
     * @return true if a category was removed.
     */
    public boolean removeCategory(String domain, String value) {
        return com.hitorro.basedms.CategoryBaseUtil.removeCategory(domain, value, this);
    }

    private void executeNewTrigger(boolean fireNewTrigger) {
        if (fireNewTrigger) {
            com.hitorro.util.typesystem.TypeManager.executeTrigger(com.hitorro.util.typesystem.OnTrigger.TriggerType.OnNew, this, true);
        }
    }

    /**
     * Create a Major
     *
     * @return
     */
    public VersionableObject createMajorVersion() throws com.hitorro.basedms.SystemVersionException {
        VersionableObject newObject = createMajorMinorVersion();
        newObject.setVersionLabel(VersioningUtil.getMajorVersion(this.getVersionLabel()));
        return newObject;
    }

    public VersionableObject createMinorVersion() throws com.hitorro.basedms.SystemVersionException {
        VersionableObject newObject = createMajorMinorVersion();
        newObject.setVersionLabel(VersioningUtil.getMinorVersion(this.getVersionLabel()));
        return newObject;
    }

    public VersionableObject createBranchVersion() throws com.hitorro.basedms.SystemVersionException {
        if (branchVersion != null) {
            throw new com.hitorro.basedms.SystemVersionException("Already has branch");
        }
        VersionableObject newObject = (VersionableObject) com.hitorro.util.typesystem.TypeManager.getTypeManager().getCopy(this);
        newObject.setParentVersion(this);
        newObject.setCanonicalGuid(this.getCanonicalGuid());
        newObject.setCanonical(this.getCanonical());
        branchVersion = newObject;
        newObject.setVersionLabel(VersioningUtil.getBranch(this.getVersionLabel()));
        return newObject;
    }

    protected VersionableObject createMajorMinorVersion() throws com.hitorro.basedms.SystemVersionException {
        if (nextVersion != null) {
            throw new com.hitorro.basedms.SystemVersionException("Not at head version");
        }
        VersionableObject newObject = (VersionableObject) com.hitorro.util.typesystem.TypeManager.getTypeManager().getCopy(this);
        nextVersion = newObject;
        newObject.setParentVersion(this);
        newObject.setCanonicalGuid(this.getCanonicalGuid());
        newObject.setCanonical(this.getCanonical());
        return newObject;
    }

    public void copy(VersionableObject orig) {
        note = orig.note;
        versionLabel = orig.versionLabel;
        canonicalGuid = orig.canonicalGuid;
        canonical = orig.canonical;
        creationDate = new Date();
        nextVersion = null;
        branchVersion = null;
        parentVersion = orig;
        identityHash = orig.identityHash;
        state = orig.state;
        creator = orig.creator;
        effectiveUser = orig.effectiveUser;

        // content
        content = orig.content;

        // content
        HashSet<Content> temp = new HashSet<Content>();
        // have to copy this way because of hibernate
        for (Content c : orig.contents) {
            temp.add(c);
        }
        contents = temp;

        HashSet<Container> cont = new HashSet<Container>();
        for (Container c : orig.containers) {
            cont.add(c);
        }
        owningContainer = orig.owningContainer;

        for (Content c : orig.contents) {
            c.incrementRefCount();
        }

        this.containers = cont;
        this.adapterSource = orig.adapterSource;
    }


    /**
     * Cascade upgrade.  Deal with the lowest to highest current schema revision applying changes as you go.
     *
     * @param currentSchemaVersion
     * @return
     */
    public boolean upgradeAllInstances(long currentSchemaVersion) {
        switch ((int) currentSchemaVersion) {
            case 2:
                //upgrade 2-3
            case 3:
                //upgrade 3-4
                return true;
            default:
                return false;
        }
    }


    public String getVersionLabel() {
        return versionLabel;
    }

    void setVersionLabel(String version) {
        versionLabel = version;
    }

    public String getCanonicalGuid() {
        return canonicalGuid;
    }

    void setCanonicalGuid(String guid) {
        canonicalGuid = guid;
    }

    public VersionableObject getCanonical() {
        return canonical;
    }

    void setCanonical(VersionableObject so) {
        canonical = so;
    }

    public VersionableObject getNextVersion() {
        return nextVersion;
    }

    /**
     * Package private methods, ones that we only want package level objects to see. Note this is specific so that users
     * of these objects do not have access to api's that could break the referential integrity, or break the semantics
     * we wish to impose, such as ensuring content copy on write behavior
     */

    void setNextVersion(VersionableObject so) {
        nextVersion = so;
    }

    /**
     * The state of the object.
     *
     * @return a state constant (one of the constants on app.Constants)
     */
    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "State", displayType = com.hitorro.util.typesystem.annotation.UiProperties.SelectListDisplay, order = 25)
    public int getState() {
        return state;
    }

    public void setState(int st) {
        state = st;
    }

    public boolean isActive() {
        return state == Constants.ActiveState;
    }

    public VersionableObject getBranchVersion() {
        return branchVersion;
    }

    public void setBranchVersion(VersionableObject so) {
        branchVersion = so;
    }

    public VersionableObject getParentVersion() {
        return parentVersion;
    }

    void setParentVersion(VersionableObject so) {
        parentVersion = so;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Content addContentIfNotNull(String url, String domain, String value, ContentType ct, long playLength, long fileSize) throws CategoryException {
        if (!StringUtil.nullOrEmptyString(url)) {
            if (ct == null) {
                ct = ContentTypeCache.getCache().getTypeFromFileWithDefault(url);
            }
            Content c = setContentLink(url, ct);
            if (playLength != 0) {
                c.setDurationSeconds((int) (playLength / com.hitorro.util.core.Constants.MillisInSecond));
            }
            c.setContentSize(fileSize);
            c.addCategory(domain, value);
            return c;
        }
        return null;
    }

    public List<ExternalContent> getExternalContentByTag(String domain, String... values) {
        TagConstraint tc[] = new TagConstraint[values.length];
        for (int i = 0; i < values.length; i++) {
            tc[i] = new TagConstraint(domain, values[i]);
        }
        HTPredicate<Content> constraint = new LogicalOrOperator<Content>(tc);
        List<Content> contents = this.getAllContentByConstraint(constraint, true);
        if (ListUtil.nullOrEmpty(contents)) {
            return null;
        }
        List<ExternalContent> encs = new ArrayList<ExternalContent>();
        for (Content c : contents) {
            ExternalContent e = new ExternalContent();
            e.setType(c.getContentType().getMimeType());
            e.setUrl(c.getExternalURL());
            e.setPlayLength(c.getDurationSeconds());
            e.setFileLength(c.getContentSize());
            encs.add(e);
        }
        return encs;
    }

    /**
     * This field is to be overiden by subclasses that know how to get their "content".  For example, an Post would
     * return its BodyText part.
     * <p/>
     * For now this is a short hack.  Really this should be data driven, that perhaps generates a fulltext content
     * file.
     *
     * @return
     */

    @com.hitorro.util.typesystem.annotation.FullTextAttributeMetaInfo(displayName = "contenttext",
            isFullTextIndexable = true, luceneIndexingFilters = "STANDARD,CASE,PORTERSTEM",
            luceneFieldName = "contenttext", stringLiteral = false, allField = true, stored = false)

    public String getContentText() {
        return "";
    }

    @com.hitorro.util.typesystem.annotation.FullTextAttributeMetaInfo(displayName = CreateDateKey,
            isFullTextIndexable = true, luceneIndexingFilters = "STANDARD,CASE,PORTERSTEM",
            luceneFieldName = CreateDateKey, stringLiteral = false, isDate = true, allField = true, stored = false)
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * Dont set creation date.
     *
     * @param creationDate
     */
    protected void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void touch() {
        setModifiedDate(new Date());
    }

    @com.hitorro.util.typesystem.annotation.FullTextAttributeMetaInfo(displayName = "modifiedDate",
            isFullTextIndexable = true, luceneIndexingFilters = "STANDARD,CASE,PORTERSTEM",
            luceneFieldName = "modifiedDate", stringLiteral = false, isDate = true, allField = true, stored = false)

    public Date getModifiedDate() {
        return modifiedDate;
    }

    /**
     * Not to be called, should be called only from
     *
     * @param modifiedDate
     */
    protected void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    @com.hitorro.util.typesystem.annotation.FullTextAttributeMetaInfo(displayName = "authoredDate",
            isFullTextIndexable = true, luceneIndexingFilters = "STANDARD,CASE,PORTERSTEM",
            luceneFieldName = "authoredDate", stringLiteral = false, isDate = true, allField = true, stored = false)
    public Date getAuthoredDate() {
        return authoredDate;
    }

    public void setAuthoredDate(Date authoredDate) {
        this.authoredDate = authoredDate;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getEffectiveUser() {
        return effectiveUser;
    }

    public void setEffectiveUser(String effectiveUser) {
        this.effectiveUser = effectiveUser;
    }

    public boolean visitVersionTree(com.hitorro.basedms.VersionableObjectVisitor visitor) {
        // root version if (this.m != null)
        if (StringUtil.nullOrEmptyOrBlankString(canonicalGuid)) {
            return visitVersionTreeAux(visitor);
        } else {
            VersionableObject root = this.getCanonical();
            if (root == null) {
                return false;
            }
            return root.visitVersionTreeAux(visitor);
        }
    }

    protected boolean visitVersionTreeAux(com.hitorro.basedms.VersionableObjectVisitor visitor) {
        if (!visitor.visit(this)) {
            return false;
        }

        if (nextVersion != null) {
            if (nextVersion.visitVersionTreeAux(visitor)) {
                return false;
            }
        }

        if (branchVersion != null) {
            return !branchVersion.visitVersionTreeAux(visitor);
        }
        return true;
    }

    public void addContainer(Container c) {
        containers.add(c);
    }

    public Set<Container> getContainers() {
        return containers;
    }

    //************** CONTENT STUFF ******************************

    public void setContainers(Set<Container> containers) {
        this.containers = containers;
    }

    // ----------------- ListValueSource
    public ListValue[] getValues(Object obj, String fieldName, String tag) {
        if (fieldName.equals("state")) {
            return com.hitorro.basedms.Constants.getStateListValues();
        }

        return null;
    }

    public void serialize(com.hitorro.util.typesystem.HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);

        // version 4
        os.writeString(adapterSource);

        // version 3
        os.writeString(effectiveUser);
        os.writeString(creator);
        os.writeString(realm);

        // version 2
        os.writeLong(identityHash);

        // version 1
        os.writeDate(creationDate);
        os.writeDate(modifiedDate);
        os.writeDate(authoredDate);
        os.writeString(note);
        os.writeString(versionLabel);
        os.writeString(canonicalGuid);
        os.writeVersionedObject(canonical);
        os.writeVersionedObject(nextVersion);
        os.writeVersionedObject(branchVersion);
        os.writeVersionedObject(parentVersion);
        os.writeInt(state);

        os.writeString(content);
        os.writeSetOfBaseType(contents);
        os.writeSetOfBaseType(containers);
        os.writeVersionedObject(owningContainer);
        os.writeSetOfDomainValue(categories);

        os.writeBoolean(shouldIndex);
        // we should not say its indexed on the other side but this may be a simple reload.
        // need to figure out how to deal with reloads of different content
        os.writeBoolean(isIndexed);
        os.writeString(indexName);
    }

    /**
     * @param is
     * @return
     */
    public void deserialize(com.hitorro.util.typesystem.HTObjectInputStream is)
            throws IOException, ClassNotFoundException, StoreException {
        int version = is.readInt();
        super.deserialize(is);
        switch (version) {
            case 4:
                adapterSource = is.readString();
            case 3:
                effectiveUser = is.readString();
                creator = is.readString();
                realm = is.readString();

            case 2:
                identityHash = is.readLong();
            case 1:
                creationDate = is.readDate();
                modifiedDate = is.readDate();
                authoredDate = is.readDate();
                note = is.readString();
                versionLabel = is.readString();
                canonicalGuid = is.readString();
                canonical = (VersionableObject) is.readVersionedObject();
                nextVersion = (VersionableObject) is.readVersionedObject();
                branchVersion = (VersionableObject) is.readVersionedObject();
                parentVersion = (VersionableObject) is.readVersionedObject();

                state = is.readInt();
                // content
                content = is.readString();
                is.readSetOfHTSerializable(contents);
                is.readSetOfHTSerializable(containers);
                owningContainer = (Container) is.readVersionedObject();
                is.readSetOfDomainValue(categories);
                shouldIndex = is.readBoolean();
                isIndexed = is.readBoolean();
                indexName = is.readString();
                break;
        }
    }

    public int getSerializationVersion() {
        return SerializationVersion;
    }

    public Content setStringContent(String domain, String label, String filename, String value, String contentType) {
        ContentType htmlType = ContentTypeCache.getCache().getContentTypeByMimeType(contentType);
        return setStringContent(domain, label, filename, value, htmlType);
    }

    public Content setStringContent(String domain, String label, String filename, String value) {
        ContentType htmlType = ContentTypeCache.getCache().getContentTypeByMimeType("text/html");
        Content c = setStringContent(domain, label, filename, value, htmlType);
        this.getContents().add(c);
        return c;
    }

    public Content setHTSerializableContent(String domain,
                                            String label,
                                            String filename,
                                            com.hitorro.util.typesystem.HTSerializable pts,
                                            ContentType contentType) {
        return setHTSerializableContent(domain, label, filename, pts, contentType, null);
    }

    public Content setHTSerializableContent(String domain,
                                            String label,
                                            String filename,
                                            com.hitorro.util.typesystem.HTSerializable pts,
                                            ContentType contentType,
                                            Store store) {
        return setHTSerializableContent(domain, label, filename, pts, contentType, store, false);
    }

    /**
     * Persist a serialized object as content.
     *
     * @param domain
     * @param label
     * @param filename
     * @param pts
     * @param contentType
     * @return
     */
    public Content setHTSerializableContent(String domain,
                                            String label,
                                            String filename,
                                            com.hitorro.util.typesystem.HTSerializable pts,
                                            ContentType contentType,
                                            Store store,
                                            boolean reStore) {
        Content theContent = null;
        try {
            byte buff[] = com.hitorro.util.typesystem.HTSerializableUtil.getHTSerializableBuffer(pts);
            ByteArrayInputStream ins = new ByteArrayInputStream(buff);
            if (reStore) {
                // attempt to see if the content already exists
                theContent = getContentByConstraint(new TagConstraint(domain, label), true);
                if (theContent != null) {
                    theContent.setContent(filename, ins, contentType);
                    theContent.addCategory(domain, label);
                    return theContent;
                }
            }
            theContent = setContent(filename, contentType, ins, store);
            theContent.addCategory(domain, label);
        } catch (StoreException se) {
            Log.basedms.error("Setting content ", se);
            theContent = null;
        } catch (IOException ioe) {
            Log.basedms.error("Setting content ", ioe);
            theContent = null;
        } catch (CategoryException ce) {
            Log.basedms.error("Setting content ", ce);
            theContent = null;
        }
        return theContent;
    }

    public Content setStringContent(String domain,
                                    String label,
                                    String filename,
                                    String value,
                                    ContentType contentType) {
        return setStringContent(domain, label, filename, value, contentType, null);
    }

    public Content setStringContent(String domain,
                                    String label,
                                    String filename,
                                    String value,
                                    ContentType contentType,
                                    Store store) {
        Content theContent = null;
        try {
            byte[] bytes;
            if (value == null) {
                // we'll treat a null the same as an empty string
                bytes = new byte[0];
            } else {
                bytes = value.getBytes(StringUtil.UTF8Encoding);
            }
            ByteArrayInputStream ins = new ByteArrayInputStream(bytes);
            theContent = setContent(filename, contentType, ins, store);
            theContent.addCategory(domain, label);
        } catch (StoreException se) {
            Log.basedms.error("Setting content ", se);
            theContent = null;
        } catch (IOException ioe) {
            Log.basedms.error("Setting content ", ioe);
            theContent = null;
        } catch (CategoryException ce) {
            Log.basedms.error("Setting content ", ce);
            theContent = null;
        }
        return theContent;
    }

    public com.hitorro.util.typesystem.HTSerializable getHTSerializableContent(String domain, String label) {
        InputStream is = null;
        try {
            Content contRetrieved = getContentByConstraint(new TagConstraint(domain, label), true);
            if (contRetrieved == null) {
                // no such content
                return null;
            }
            is = contRetrieved.getContent();
            return com.hitorro.util.typesystem.HTSerializableUtil.readHTSerializableFromBuffer(is, null);
        } catch (StoreException se) {
            Log.basedms.error("Getting content ", se);
            return null;
        } catch (IOException ioe) {
            Log.basedms.error("Getting content ", ioe);
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.basedms.error("%s %e", e, e);
                }
            }
        }
    }

    public String getStringContent(String domain, String label) {
        Content contRetrieved = getContentByConstraint(new TagConstraint(domain, label), true);
        return (contRetrieved != null) ? contRetrieved.getStringValue() : null;
    }

    public Content getContentByFileName(String name, boolean ignoreCase) {
        return getContentByConstraint(new FileNameMatchContentConstraint(name, ignoreCase), false);
    }

    public Content getContentByConstraint(HTPredicate<Content> constraint, boolean recurse) {
        return getContentByConstraint(contents, constraint, recurse);
    }

    /**
     * Get all the content that matches the constraint.
     *
     * @param constraint
     * @param recurse
     * @return
     */
    public List<Content> getAllContentByConstraint(HTPredicate<Content> constraint, boolean recurse) {
        List<Content> list = new ArrayList<Content>();
        if (constraint == null) {
            return list;
        }
        getContentsByConstraint(contents, constraint, recurse, list);

        return list;
    }

    public Content setContent(String externalFileName,
                              ContentType type,
                              BaseFile file)
            throws IOException, StoreException {
        Content c = setContentAux(externalFileName);
        c.setContent(externalFileName, file, type);
        return c;
    }

    public Content setContent(String externalFileName,
                              ContentType type,
                              File file)
            throws IOException, StoreException {
        Content c = setContentAux(externalFileName);
        c.setContent(externalFileName, file, type);
        return c;
    }

    public Content createEmptyContent(String externalFileName,
                                      ContentType type,
                                      InputStream is,
                                      Store store)
            throws IOException, StoreException {
        Content c = createEmptyContent(externalFileName, type, store);
        if (c == null) {
            return null;
        }
        c.setContent(externalFileName, is, type);
        return c;
    }

    /**
     * Not the most efficient way to create a zero length file.
     *
     * @param externalFileName
     * @param type
     * @param store
     * @return
     * @throws IOException
     * @throws StoreException
     */
    public Content createZeroLengthContent(String externalFileName,
                                           ContentType type,
                                           Store store)
            throws IOException, StoreException {
        byte b[] = new byte[0];
        ByteArrayInputStream bios = new ByteArrayInputStream(b);
        return setContent(externalFileName, type, bios, store);

    }

    public Content setContent(String externalFileName,
                              ContentType type,
                              InputStream is,
                              Store store)
            throws IOException, StoreException {
        Content c = createEmptyContent(externalFileName, type, store);
        if (c == null) {
            return null;
        }
        c.setContent(externalFileName, is, type);
        return c;
    }

    private Content createEmptyContent(String externalFileName,
                                       ContentType type,
                                       Store store) {
        Content c = setContentAux(externalFileName);
        if (store != null) {
            c.setStoreName(store.getSoftGuid());
        }
        return c;
    }

    public Content setContentLink(String externalFileName, ContentType type) {
        Content c = setContentAux(externalFileName);
        c.setOriginalFileName(externalFileName);
        c.setStoreName(com.hitorro.basedms.StoreUtil.getLinkStore().getSoftGuid());
        c.setContentType(type);
        return c;
    }

    /**
     * Store content link file for an unmanaged file system. This means we have been provided a root to the file system
     * and a file.  We will mask off the root part of the file system and store only the trailing aspect
     *
     * @param file
     * @param type
     * @param fs
     * @return
     * @throws StoreException
     */
    public Content setContentLinkForUnmanagedFileStore(File file, ContentType type, Store fs)
            throws StoreException {
        if (fs == null) {
            throw new StoreException("Store not provided");
        }
        if (!fs.getStoreTypeType().isUnmanagedFileStore()) {
            throw new StoreException(Fmt.S("Store %s is not an unmanaged store", fs.getStoreType()));
        }
        String externalFileName = file.getAbsolutePath();
        String rootPath = fs.getRootPathPath().getAbsolutePath();
        if (!externalFileName.startsWith(rootPath)) {
            throw new StoreException(Fmt.S("File %s is not managed by %s is not an unmanaged store",
                    externalFileName, fs.getStoreType()));
        }
        Content c = setContentAux(externalFileName);
        c.setOriginalFileName(externalFileName);
        // the stored name excludes the full path.
        c.setFileName(externalFileName.substring(rootPath.length()));
        c.setStoreName(fs.getSoftGuid());
        return c;
    }

    @com.hitorro.util.typesystem.annotation.FullTextAttributeMetaInfo(displayName = "content",
            isFullTextIndexable = true, luceneIndexingFilters = "STANDARD,CASE,PORTERSTEM",
            luceneFieldName = "body", stringLiteral = false, allField = true)
    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "Content", displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextAreaDisplay)
    public String getContent() {
        return content;
    }

    public void setContent(String m_content) {
        this.content = m_content;
    }

    private Content setContentAux(String externalFileName) {
        Content c = getContentByFileName(externalFileName, true);
        if (c != null) {
            // we have to replace the current content and unlink it from this guy.
            // this should deal with the content object being orphaned, and
            c.decrementRefCount();
            contents.remove(c);
        }
        c = new Content();
        getContents().add(c);
        // increment ref count
        c.incrementRefCount();
        return c;
    }

    /**
     * get the first content that matches the criteria
     *
     * @param contents
     * @param constraint
     * @param recurse
     * @return
     */
    private Content getContentByConstraint(Set<Content> contents, HTPredicate<Content> constraint, boolean recurse) {
        if (contents == null) {
            return null;
        }
        Iterator<Content> cont = contents.iterator();
        while (cont.hasNext()) {
            Content c = cont.next();
            if (constraint.test(c)) {
                return c;
            }
            if (recurse) {
                c = getContentByConstraint(c.getRenditions(), constraint, recurse);
                if (c != null) {
                    return c;
                }
            }
        }
        return null;
    }

    /**
     * Get all content that matches a constraint
     *
     * @param contents
     * @param constraint
     * @param recurse
     * @return
     */
    private void getContentsByConstraint(Set<Content> contents, HTPredicate<Content> constraint, boolean recurse, List<Content> l) {
        if (contents == null) {
            return;
        }
        Iterator<Content> cont = contents.iterator();
        while (cont.hasNext()) {
            Content c = cont.next();
            if (constraint.test(c)) {
                l.add(c);
            }
            if (recurse) {
                getContentsByConstraint(c.getRenditions(), constraint, recurse, l);
            }
        }
    }

    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "Contents", displayType = com.hitorro.util.typesystem.annotation.UiProperties.ContentsDisplay, order = 400)
    public Set<Content> getContents() {
        return contents;
    }

    protected void setContents(Set<Content> m_contents) {
        this.contents = m_contents;
    }

    public int removeAllContentWithDomainValue(String domain, String label) {
        return removeAllContentByConstraint(new TagConstraint(domain, label), true);
    }

    public int removeAllContentByConstraint(HTPredicate<Content> constraint, boolean recurse) {
        return removeAllContentByConstraint(contents, constraint, recurse);
    }

    private int removeAllContentByConstraint(Set<Content> contents, HTPredicate<Content> constraint, boolean recurse) {
        int counter = 0;
        if (contents != null) {
            Iterator<Content> cont = contents.iterator();
            while (cont.hasNext()) {
                Content c = cont.next();
                if (constraint.test(c)) {
                    cont.remove();
                    counter++;
                }
                if (recurse) {
                    counter += removeAllContentByConstraint(c.getRenditions(), constraint, recurse);
                }

            }
        }
        return counter;
    }

    void setMinorVersion(VersionableObject so) {
        nextVersion = so;
    }

    public String getAdapterSource() {
        return adapterSource;
    }

    public void setAdapterSource(String adapterSource) {
        this.adapterSource = adapterSource;
    }

    @com.hitorro.util.typesystem.annotation.TypeClassMetaInfo(shortTypeName = "VersionableObjectSearchView",
            isView = true,
            isPersisted = false,
            schemaVersion = SerializationVersion)
    public abstract static class VersionableObjectSearchView {
        public abstract String getTitle();


    }
}
