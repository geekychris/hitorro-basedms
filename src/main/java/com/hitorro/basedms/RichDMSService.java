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

import com.hitorro.base.objects.BaseDMSService;
import com.hitorro.basedms.commands.DumpVersionTree;
import com.hitorro.util.integrationevents.IntegrationEventsContext;
import com.hitorro.util.integrationevents.ListIntegrationEvents;
import com.hitorro.util.integrationevents.RunIntegrationEvent;
import com.hitorro.util.json.keys.propaccess.PropaccessError;
import com.hitorro.util.startupframework.phases.ServiceDefinition;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved.
 * <p/>
 * User: chris
 */
@ServiceDefinition(dependentService = {BaseDMSService.class},
        shortName = "richdms",
        description = "rich document management service",
        debugCommands = {DumpVersionTree.class, ListIntegrationEvents.class, RunIntegrationEvent.class},
        typeManagedClasses = {},
        uiDirectories = {})
public class RichDMSService {
    public static final String IndexIntegrationEventName = "index";


    public String init(boolean dbInit, final boolean upgrading, final long currentVersion, final long targetVersion) {
        if (dbInit) {
            try {
                IntegrationEventsContext.getContext().runEvent(IndexIntegrationEventName);
            } catch (PropaccessError propaccessError) {
                return propaccessError.getMessage();
            }
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
}
