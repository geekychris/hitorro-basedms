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

import com.hitorro.basedms.BasePersistenceService;
import com.hitorro.basedms.auth.BaseUserAuthMethod;
import com.hitorro.basedms.commands.DumpVersionTree;
import com.hitorro.basedms.commands.TestStreams;
import com.hitorro.basedms.db.HibernateService;
import com.hitorro.basedms.scheduler.SchedulerService;
import com.hitorro.basedms.workflow.WorkflowService;
import com.hitorro.network.resourcecache.ResourceService;
import com.hitorro.util.auth.AuthenticationService;
import com.hitorro.util.integrationevents.IntegrationEventsContext;
import com.hitorro.util.integrationevents.ListIntegrationEvents;
import com.hitorro.util.integrationevents.RunIntegrationEvent;
import com.hitorro.util.json.keys.propaccess.PropaccessError;
import com.hitorro.util.startupframework.phases.ServiceDefinition;

@ServiceDefinition(dependentService = {HibernateService.class,
        BasePersistenceService.class,
        SchedulerService.class,
        WorkflowService.class,
        ResourceService.class},
        shortName = "basedms",
        description = "Base document management system service",
        debugCommands = {DumpVersionTree.class,
                ListIntegrationEvents.class,
                RunIntegrationEvent.class,
                TestStreams.class},
        typeManagedClasses = {Document.class,
                Forum.class,
                RssFeedIn.class,
                Post.class,
                SubjectArea.class,
                Client.class},
        uiDirectories = {})
public class BaseDMSService {
    public static final String StoreIntegrationEventName = "stores";
    public static final String UsersIntegrationEventName = "users";
    public static final String DomainInfoIntegrationEventName = "domaininfo";
    public static boolean s_initialized = false;

    public String init(boolean dbInit, final boolean upgrading, final long currentVersion, final long targetVersion) {
        if (dbInit) {
            try {
                IntegrationEventsContext.getContext().runEvent(StoreIntegrationEventName);
                IntegrationEventsContext.getContext().runEvent(DomainInfoIntegrationEventName);
            } catch (PropaccessError propaccessError) {
                return propaccessError.getMessage();
            }

        }
        AuthenticationService.getService().registerAuthMethod(new BaseUserAuthMethod(), true);
        s_initialized = true;
        return null;
    }

    public String run() {
        return null;
    }

    public String deInit() {
        s_initialized = false;
        return null;
    }

    public String start(boolean dbInit) {
        return null;
    }
}
