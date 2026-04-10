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
package com.hitorro.basedms.workflow.debugcommand;

import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.basedms.workflow.OutstandingWorkflowItem;
import com.hitorro.basedms.workflow.WorkFlowItem;
import com.hitorro.basedms.workflow.constraints.OutstandingWorkFlowItemTypeConstraint;
import com.hitorro.basedms.workflow.constraints.OutstandingWorkflowItemInListConstraint;
import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.util.commandandcontrol.Command;
import com.hitorro.util.commandandcontrol.CommandSession;
import com.hitorro.util.commandandcontrol.Response;
import com.hitorro.util.commandandcontrol.RestOperations;
import com.hitorro.util.commandandcontrol.ano.CommandArgument;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.core.ListUtil;
import com.hitorro.util.core.opers.HTPredicate;
import com.hitorro.util.core.opers.LogicalAndOperator;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.json.keys.StringProperty;
import com.hitorro.util.statemachine.MooreStateMachine;
import com.hitorro.util.statemachine.State;
import com.hitorro.util.statemachine.StateMachineService;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTSerializable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@CommandDef(command = "workflow.setworkflowstate", description = "Force workflow item to specified state")
public class WorkflowItemSetState extends Command {
    @CommandArgument(required = true)
    private StringProperty WfiGuid = new StringProperty("guid", "WorkFlowItem guid", null);
    @CommandArgument(required = true)
    private StringProperty WfiState = new StringProperty("state", "WorkFlowItem new state", null);
    @CommandArgument(required = true)
    private StringProperty WfiStateMachine = new StringProperty("statemachine", "WorkFlowItem MooreStateMachine", null);

    public boolean execute(String rawValue, JsonNode args, Response response, CommandSession session, RestOperations operation) throws Exception {
        boolean hasStateSet = false;

        String argWfiGuid = WfiGuid.apply(args);
        String argWfiState = WfiState.apply(args);
        String argWfiStateMachine = WfiStateMachine.apply(args);

        if (StringUtil.nullOrEmptyString(argWfiGuid)) {
            writeSimpleError(response, "WorkFlowItem guid not provided");
        }

        if (StringUtil.nullOrEmptyString(argWfiState)) {
            writeSimpleError(response, "WorkFlowItem state not provide for specified WorkFlowItem: %s", argWfiGuid);
        }

        if (StringUtil.nullOrEmptyString(argWfiStateMachine)) {
            writeSimpleError(response, "WorkFlowItem statemachine not provide for specified WorkFlowItem: %s", argWfiGuid);
        }

        BaseSession dmsSession = DMSSessionFactory.getFactory().getSession();
        try {
            HTSerializable serializable = dmsSession.getObjectFromGuid(argWfiGuid);

            if (serializable instanceof WorkFlowItem) {
                WorkFlowItem wfi = (WorkFlowItem) serializable;
                String wfiStateMachine = wfi.getStateMachine();

                if (wfiStateMachine.equals(argWfiStateMachine)) {
                    MooreStateMachine stateMachine = StateMachineService.getService().getStateMachine(argWfiStateMachine);
                    State state = null;

                    if (stateMachine != null) {
                        state = stateMachine.getState(argWfiState);

                        if (state != null) {
                            Set<OutstandingWorkflowItem> outstandingWfis = wfi.getOutstandingWorkflowItems();

                            List staticOwfis = new ArrayList();
                            staticOwfis = ListUtil.iteratorToList(outstandingWfis.iterator(), staticOwfis);
                            wfi.setCurrentState(argWfiState);
                            wfi.notify(null, dmsSession);

                            HTPredicate<OutstandingWorkflowItem> validWfiConstraint = new OutstandingWorkFlowItemTypeConstraint(WorkFlowItem.GeneralNotificationType, true);
                            HTPredicate<OutstandingWorkflowItem> wfiInListConstraint = new OutstandingWorkflowItemInListConstraint(staticOwfis);
                            LogicalAndOperator<OutstandingWorkflowItem> wfiAndConstraint = new LogicalAndOperator(validWfiConstraint, wfiInListConstraint);
                            wfi.removeOutstandingWorkflowItemByConstraint(wfiAndConstraint, dmsSession);

                            hasStateSet = true;
                            writeSuccess(response, "set WorkFlowItem: %s to state: %s", wfi.getGuid(), state.getName());
                        } else {
                            writeSimpleError(response, "failure to find State: %s for WorkFlowItem: %s with MooreStateMachine: %s", argWfiState, wfi.getGuid(), stateMachine.toString());
                        }
                    } else {
                        writeSimpleError(response, "WorkFlowItem: %s has the MooreStateMachine: %s, not the command-line specified MooreStateMachine: %s", wfi.getGuid(), wfi.getStateMachine(), argWfiStateMachine);
                    }
                } else {
                    writeSimpleError(response, "WorkFlowItem: %s has associated MooreStateMachine: %s, not specified MooreStateMachine: %s", wfi.getGuid(), wfi.getStateMachine(), argWfiStateMachine);
                }
            } else {
                writeSimpleError(response, "failure to find WorkFlowItem with guid: %s", argWfiGuid);
            }
        } finally {
            if (hasStateSet) {
                DMSSessionFactory.getFactory().commitAndClose(dmsSession);

            } else {
                DMSSessionFactory.getFactory().rollbackClose(dmsSession);
            }
        }

        return hasStateSet;
    }
}
