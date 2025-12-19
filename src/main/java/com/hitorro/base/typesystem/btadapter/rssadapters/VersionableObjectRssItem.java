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
package com.hitorro.base.typesystem.btadapter.rssadapters;

import com.hitorro.basedms.contentconstraints.TagConstraint;
import com.hitorro.basedms.xml.RssItemInterface;
import com.hitorro.util.core.ListUtil;
import com.hitorro.util.core.valuemap.DomainValueIntf;
import com.hitorro.util.typesystem.Type;
import com.hitorro.util.typesystem.TypeManager;
import com.hitorro.util.typesystem.btadapter.BaseTypeAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * <p/>
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Sep 6, 2005 Time: 4:06:37 PM
 * <p/>
 * Abstract item that will generate an RssItemInterface compatible accessor.  In this case
 */
public class VersionableObjectRssItem<E extends com.hitorro.base.objects.VersionableObject> implements RssItemInterface,
        BaseTypeAdapter<E, RssItemInterface> {
    public static final String AdapterGroup = "RssItem";
    protected E obj;
    private List<com.hitorro.base.objects.ExternalContent> enclosures = null;
    private List<String> topWords = new ArrayList<String>();

    public Class getHonoringClass() {
        return com.hitorro.base.objects.VersionableObject.class;
    }

    public String getAdapterGroup() {
        return AdapterGroup;
    }

    public RssItemInterface getObject(E obj) {
        VersionableObjectRssItem o = getNew();
        o.setObject(obj);
        return o;
    }

    public void setObject(E obj) {
        this.obj = obj;
    }

    protected VersionableObjectRssItem<E> getNew() {
        return new VersionableObjectRssItem();
    }

    public List<String> getCategoriesList() {
        List<String> cats = new ArrayList<String>();
        Set<DomainValueIntf> set = obj.getCategories();
        for (DomainValueIntf dv : set) {
            String domain = dv.getDomain();
            if (domain.equals(com.hitorro.base.objects.Category.HTCATEGORY) || domain.equals(com.hitorro.base.objects.Category.HTEXTERNALCATEGORY)) {
                cats.add(dv.getValue());
            }
        }
        return cats;
    }

    public String getPostId() {
        return null;
    }

    public boolean hasAd() {
        return false;
    }

    public String getAdUrl() {
        return "";  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getAdText() {
        return "";  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getType() {
        Type type = TypeManager.getTypeManager().getTypeFromGuid(obj.getGuid());
        if (type == null) {
            return "";
        }
        return type.getName();
    }

    public String getSponsored() {
        return "";
    }

    public String getTLD() {
        return "";
    }

    // NOT EXPECTED TO BE CHANGED

    public String getShow() {
        return "";
    }

    public String getAlternateQuery() {
        return "";
    }

    public String getGuid() {
        return obj.getGuid();
    }

    public String getFeedContactAuthorDisplayName() {
        return getAuthorDisplayName();
    }

    public Date getPostDate() {
        return obj.getAuthoredDate();
    }

    public String getReferringAuthorDisplayName() {
        return obj.getEffectiveUser();
    }

    public String getAuthorDisplayName() {
        return obj.getEffectiveUser();
    }

    public String getComments() {
        return obj.getContent();
    }

    public int getDegree() {
        return -1;
    }

    public boolean hasEnclosure() {
        return !ListUtil.nullOrEmpty(getEnclosures());
    }

    public List<com.hitorro.base.objects.ExternalContent> getEnclosures() {
        if (enclosures == null) {
            enclosures = obj.getExternalContentByTag(com.hitorro.base.objects.Post.HTContentAssetTag, com.hitorro.base.objects.Post.EnclosureLabel, com.hitorro.base.objects.Post.AudioTag, com.hitorro.base.objects.Post.VideoTag);
        }
        return enclosures;
    }

    public String getImageUrl() {
        com.hitorro.base.objects.Content c = obj.getContentByConstraint(new TagConstraint(com.hitorro.base.objects.Post.HTContentAssetTag, com.hitorro.base.objects.Post.LargeImageTag), true);
        if (c != null) {
            return c.getExternalURL();
        }
        return null;

    }

    // EXPECTED TO BE OVERIDEN

    /**
     * For an rss feed we really want the body from the feed and not the permalink.
     *
     * @return
     */
    public String getDescription() {
        String b = obj.getStringContent(com.hitorro.base.objects.Post.DocPartsDomain, com.hitorro.base.objects.Post.BodyLabel);
        if (b != null) {
            return b;
        }
        return obj.getStringContent(com.hitorro.base.objects.Post.DocPartsDomain, com.hitorro.base.objects.Post.BodyFromPermaLinkLabel);
    }

    public String getTitle() {
        return "";
    }

    public boolean hasExternalUrl() {
        return false;
    }

    public String getClickThroughText() {
        return "";
    }

    public String getClickThroughUrl() {
        return null;
    }

    public String getLink() {
        return "";
    }

    public List<String> getTopWords() {
        return topWords;
    }

    public void setTopWords(List<String> words) {
        topWords = words;
    }

}
