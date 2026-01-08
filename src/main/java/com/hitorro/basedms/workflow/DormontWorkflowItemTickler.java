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
package com.hitorro.basedms.workflow;

import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.Env;
import com.hitorro.util.json.keys.IntegerProperty;
import com.hitorro.util.typesystem.BaseSession;

import java.util.Iterator;

/**

 * Looks for workflow items that havent moved in a while and looks to see if they can be moved on.
 * <p/>
 * This notifies any non modified wfi's to see if they have a validator that tickles them to the next state. For
 * example.  Send for approval doesnt get an approve or deny in n hours.
 */
public class DormontWorkflowItemTickler implements Runnable {
    public static IntegerProperty MinutesToWaitBetweenPoll = new IntegerProperty("workflow.dormentcheckinterval",
            "Time in minutes to check for dormont workflow items",
            60);

    public static IntegerProperty OlderThanLastModifiedTimeMinutes = new IntegerProperty("workflow.olderthan",
            "How much older the workflow item must be for it to be looked at.",
            60);


    public void run() {
        while (true) {
            BaseSession session = DMSSessionFactory.getFactory().getSession();
            try {
                Iterator<String> iter = WorkFlowItem.getAllWFGuidsNonFinishedItemsNotTouched(OlderThanLastModifiedTimeMinutes.apply(), session);
                while (iter.hasNext()) {
                    String guid = iter.next();
                    BaseSession notifySession = DMSSessionFactory.getFactory().getSession();
                    boolean good = false;
                    // have a guid, lets notify it.
                    try {
                        WorkFlowItem wfi = (WorkFlowItem) notifySession.getObjectFromGuid(guid);
                        if (wfi != null) {
                            good = wfi.notify(null, notifySession);
                        }
                    } finally {
                        if (good) {
                            DMSSessionFactory.getFactory().commitAndClose(notifySession);

                        } else {
                            DMSSessionFactory.getFactory().rollbackClose(notifySession);
                        }
                    }
                }
            } finally {
                DMSSessionFactory.getFactory().rollbackClose(session);
            }
            Env.sleepNSeconds(60 * MinutesToWaitBetweenPoll.apply());
        }
    }
}
