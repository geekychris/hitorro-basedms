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

import com.hitorro.util.statemachine.State;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTSerializable;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved.
 * <p/>
 * User: chris
 */
public class WorkflowStateContext {
    private WorkFlowItem workflowItem;
    private HTSerializable notificationItem;
    private BaseSession session;
    private State currentState;

    public String getEffectiveUser() {
        return workflowItem.getEffectiveUser();

    }

    public WorkFlowItem getWorkflowItem() {
        return workflowItem;
    }

    public void setWorkflowItem(WorkFlowItem workflowItem) {
        this.workflowItem = workflowItem;
    }

    public HTSerializable getNotificationItem() {
        return notificationItem;
    }

    public void setNotificationItem(HTSerializable notificationItem) {
        this.notificationItem = notificationItem;
    }

    public BaseSession getSession() {
        return session;
    }

    public void setSession(BaseSession session) {
        this.session = session;
    }

    public State getCurrentState() {
        return currentState;
    }

    public void setCurrentState(State currentState) {
        this.currentState = currentState;
    }
}
