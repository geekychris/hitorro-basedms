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
package com.hitorro.basedms;

import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.base.typesystem.commands.DumpForUpgrade;
import com.hitorro.basedms.cache.ContentTypeCache;
import com.hitorro.basedms.commands.ChangePasswordCommand;
import com.hitorro.basedms.commands.RemoveOrphanedFiles;
import com.hitorro.basedms.commands.TouchObject;
import com.hitorro.basedms.commands.UserManagerCommand;
import com.hitorro.basedms.contentconstraints.FileNameMatchContentConstraint;
import com.hitorro.basedms.contentconstraints.HasRenditionConstraint;
import com.hitorro.basedms.contentconstraints.MimeTypeContentConstraint;
import com.hitorro.basedms.contentconstraints.TagConstraint;
import com.hitorro.basedms.contentpropertyextractor.ContentExtractorContext;
import com.hitorro.basedms.contentpropertyextractor.JPEGImageExtractor;
import com.hitorro.basedms.contentpropertyextractor.MP3Extractor;
import com.hitorro.basedms.db.HibernateService;
import com.hitorro.basedms.job.JobService;
import com.hitorro.basedms.servlets.ContentSystemFileReaderServlet;
import com.hitorro.jsontypesystem.predicates.JVS2JsonPredicate;
import com.hitorro.jsontypesystem.predicates.PathMatch;
import com.hitorro.network.rpc.RPCService;
import com.hitorro.network.rpc.cluster.ClusterService;
import com.hitorro.network.rpc.cluster.IDef;
import com.hitorro.network.rpc.cluster.group.BasicQueueDefinitionRealmSelector;
import com.hitorro.network.rpc.cluster.group.GroupSelector;
import com.hitorro.network.servlet.ServletService;
import com.hitorro.util.objects.DomainValue;
import com.hitorro.util.objects.ObjectVersions;
import com.hitorro.util.startupframework.OpersService;
import com.hitorro.util.startupframework.ServiceContext;
import com.hitorro.util.startupframework.phases.ServiceDefinition;
import com.hitorro.util.typesystem.Bag;
import com.hitorro.util.typesystem.BaseType;

import java.util.function.Predicate;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Oct 17, 2006 Time: 1:48:00 PM
 */
@ServiceDefinition(dependentService = {HibernateService.class, ServletService.class, OpersService.class},
        shortName = "basepersistence",
        description = "Base persistence Service",
        debugCommands = {DumpForUpgrade.class, ChangePasswordCommand.class, RemoveOrphanedFiles.class, UserManagerCommand.class, TouchObject.class},
        typeManagedClasses = {BaseType.class, Bag.class, ObjectVersions.class, com.hitorro.base.objects.VersionableObject.class, com.hitorro.base.objects.User.class,
                com.hitorro.base.objects.Permission.class, com.hitorro.base.objects.Role.class, com.hitorro.base.objects.NamedLongEntry.class, com.hitorro.base.objects.Store.class, com.hitorro.base.objects.Content.class,
                com.hitorro.base.objects.ContentType.class, com.hitorro.base.objects.Extension.class, com.hitorro.base.objects.Container.class, com.hitorro.base.objects.DomainInfo.class, DomainValue.class,
                com.hitorro.base.objects.Category.class, com.hitorro.base.objects.PersistedSerializedObject.class, FileNameMatchContentConstraint.class,
                MimeTypeContentConstraint.class, TagConstraint.class, PersistableList.class,
                HasRenditionConstraint.class, com.hitorro.base.objects.Reference.class, com.hitorro.base.objects.UserPreferences.class, com.hitorro.base.objects.UserMark.class,
                com.hitorro.base.objects.ExternalContent.class, com.hitorro.base.objects.ObjectFetcher.class, com.hitorro.base.objects.ContentSetter.class, com.hitorro.base.objects.EnqueueElement.class},
        uiDirectories = {})
public class BasePersistenceService {

    public String init(boolean dbInit, final boolean upgrading, final long currentVersion, final long targetVersion) {
        ContentTypeCache cache = ContentTypeCache.getCache();


        ServletService ss = (ServletService) ServiceContext.getSC().getInitializedModule(ServletService.class);
        if (ss == null) {
            return "ServletService not initialized";
        }
        ContentSystemFileReaderServlet servlet = new ContentSystemFileReaderServlet();
        // register ourselfs with the servlet framework
        ss.addExternalServlet(servlet, "/dmscontent/*");

        ContentExtractorContext.getContext().addExtractor(new MP3Extractor());
        ContentExtractorContext.getContext().addExtractor(new JPEGImageExtractor());

        RPCService.addHandler(new ObjectFetcherRPC());
        if (dbInit) {
        }

        // Cluster DB Group
        ClusterService cs = ClusterService.getService();
        if (cs.getShouldInitDBGroup()) {
            initGroupSelector();
        }
        return null;
    }

    public String run() {
        return null;
    }


    public String deInit() {
        return null;
    }

    public String start(boolean dbInit) {
        return null;
    }

    /**
     * Initialize any database group dynamics since we are in a cluster and have a shared db
     */
    private void initGroupSelector() {
        GroupSelector gs = GroupSelector.getSelector();
        PathMatch<String> master = new PathMatch<String>(IDef.databaseIdKey, "c1", PathMatch.stringEquals);
        gs.setMasterGroupOperator(new JVS2JsonPredicate(master));
        addQueueRealm(gs, JobService.TranscoderKey);
        addQueueRealm(gs, JobService.JobKey);
        addQueueRealm(gs, JobService.HTMLFetcherKey);
        addQueueRealm(gs, JobService.WorkflowKey);
    }

    private void addQueueRealm(final GroupSelector gs, String capability) {
        BasicQueueDefinitionRealmSelector selector = new BasicQueueDefinitionRealmSelector();
        Predicate<JsonNode> pred = IDef.getCapabilityPredicate(capability, "");
        selector.setDefinitionConstraint(new JVS2JsonPredicate(pred));
        gs.setDefinitionConstraint(capability, selector);
        selector.setRealm(capability);
    }
}
