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

import com.hitorro.base.objects.Document;
import com.hitorro.base.objects.PersistedSerializedObject;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.basedms.PersistableList;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.util.core.Console;
import com.hitorro.util.core.Constants;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.opers.HTPredicate;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.job.JobParameters;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved.
 * <p/>
 * User: chris
 * <p/>
 * represents an item
 */
@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.WorkFlowItem,
        isView = false,
        isPersisted = true,
        schemaVersion = WorkFlowItem.SerializationVersion)
public class WorkFlowItem extends VersionableObject implements com.hitorro.util.typesystem.HTSerializableNotify<com.hitorro.util.typesystem.BaseSession> {
    public static final String GeneralNotificationType = "NotificationType";
    public static final int SerializationVersion = 2;
    public static final String UploadDocKey = "HTPost";
    private static String s_query = null;
    private Set<WorkFlowItemEntry> workflowItemEntries = new HashSet<WorkFlowItemEntry>();
    private Set<OutstandingWorkflowItem> outstandingWorkflowItems = new HashSet<OutstandingWorkflowItem>();
    private String stateMachine;
    private String currentState;
    private boolean finished = false;

    /**
     * Look for workflow items that have not moved on in n minutes.
     *
     * @param minutes
     * @param session
     * @return iterator of workflow items that are old
     */
    public static Iterator<String> getAllWFGuidsNonFinishedItemsNotTouched(int minutes, com.hitorro.util.typesystem.BaseSession session) {
        String query = getQuery();
        Date date = new Date(System.currentTimeMillis() - (Constants.MillisInSecond * 60 * minutes));
        return session.getIteratorFromQueryArgs(query, date);
    }

    private static String getQuery() {
        if (StringUtil.nullOrEmptyString(s_query)) {
            StringBuilder b = new StringBuilder();
            //'2007-01-25 22:14:50'
            Console.bprint(b, "select guid from %s  as entry where finished=false", WorkFlowItem.class.getCanonicalName());

            Console.bprint(b, " and modifiedDate < :a");

            s_query = b.toString();
        }
        return s_query;
    }

    /**
     * Add a pso to the entry listFiles in the null collection using the specified key for the entry name
     *
     * @param pts     - serializable object to be stored in the pso
     * @param session
     * @param key
     * @throws IOException
     * @throws com.hitorro.util.io.StoreException
     */
    public void addPSOElement(com.hitorro.util.typesystem.HTSerializable pts, DMSSession session, String key)
            throws IOException, StoreException {
        PersistedSerializedObject pso = new PersistedSerializedObject();
        pso.setCollectionId(PersistedSerializedObject.CollectionID_Null);
        pso.setSerializableObject(pts);
        pso.setName(key);
        session.persist(pso);
        addEntry(key, pso.getGuid());
    }

    public PersistableList getPersistedList(DMSSession session, String key) {
        return getPersistedList(session, key, true);
    }

    /**
     * Util method to get a persisted liste from an entry.  It it was not achievable null is returned.
     *
     * @param session
     * @param key
     * @return
     */
    public PersistableList getPersistedList(DMSSession session, String key, boolean showException) {
        PersistedSerializedObject pso = getPSOFromEntryByKey(key, session, showException);
        PersistableList plist = null;
        if (pso == null) {
            return null;
        }
        try {
            Object o = pso.getSerializableObject();
            plist = (PersistableList) o;
            return plist;
        } catch (SQLException e) {
            Log.workflow.error("Unable to retrieve Persisted List %s %e", e, e);
        } catch (IOException e) {
            Log.workflow.error("Unable to retrieve Persisted List %s %e", e, e);
        } catch (StoreException e) {
            Log.workflow.error("Unable to retrieve Persisted List %s %e", e, e);
        } catch (ClassNotFoundException e) {
            Log.workflow.error("Unable to retrieve Persisted List %s %e", e, e);
        }
        return null;
    }

    public com.hitorro.util.typesystem.HTSerializable getPSOcontainedHTSerialalizable(DMSSession session, String key) {
        PersistedSerializedObject pso = getPSOFromEntryByKey(key, session, true);
        PersistableList plist = null;
        if (pso == null) {
            return null;
        }
        try {
            return pso.getSerializableObject();

        } catch (SQLException e) {
            Log.workflow.error("Unable to retrieve Persisted List %s %e", e, e);
        } catch (IOException e) {
            Log.workflow.error("Unable to retrieve Persisted List %s %e", e, e);
        } catch (StoreException e) {
            Log.workflow.error("Unable to retrieve Persisted List %s %e", e, e);
        } catch (ClassNotFoundException e) {
            Log.workflow.error("Unable to retrieve Persisted List %s %e", e, e);
        }
        return null;
    }

    public void addEntry(String name, String guid) {
        WorkFlowItemEntry entry = new WorkFlowItemEntry();
        entry.setItemName(name);
        entry.setItemGuid(guid);
        workflowItemEntries.add(entry);
        entry.setWorkFlowItem(this);
    }

    public void copy(VersionableObject orig) {
        super.copy(orig);
        WorkFlowItem item = (WorkFlowItem) orig;
        stateMachine = item.stateMachine;
        currentState = item.currentState;
        HashSet<WorkFlowItemEntry> temp = new HashSet<WorkFlowItemEntry>();
        // have to copy this way because of hibernate
        for (WorkFlowItemEntry c : item.workflowItemEntries) {
            temp.add(c);
        }
        workflowItemEntries = temp;
        HashSet<OutstandingWorkflowItem> t2 = new HashSet<OutstandingWorkflowItem>();
        // have to copy this way because of hibernate
        for (OutstandingWorkflowItem c : item.outstandingWorkflowItems) {
            t2.add(c);
        }
        outstandingWorkflowItems = t2;
    }

    public String getGuidFromEntry(String entryName) {
        WorkFlowItemEntry e = getEntry(entryName);
        if (e == null) {
            return null;
        }
        return e.getItemGuid();
    }

    public WorkFlowItemEntry getEntry(String entryName) {
        for (WorkFlowItemEntry entry : workflowItemEntries) {
            if (entry.getItemName().equalsIgnoreCase(entryName)) {
                return entry;
            }
        }
        return null;
    }

    public void serialize(com.hitorro.util.typesystem.HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeSetOfBaseType(workflowItemEntries);
        os.writeString(stateMachine);
        os.writeString(currentState);
        os.writeSetOfBaseType(outstandingWorkflowItems);
    }

    /**
     * @param os
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void deserialize(com.hitorro.util.typesystem.HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);

        switch (version) {

            case 1:
                os.readSetOfHTSerializable(workflowItemEntries);
                stateMachine = os.readString();
                currentState = os.readString();
                os.readSetOfHTSerializable(outstandingWorkflowItems);
        }
    }

    public void addOutstandingWorkflowItem(String guid, String name, String type) {
        OutstandingWorkflowItem item = new OutstandingWorkflowItem();
        item.setItemGuid(guid);
        item.setItemName(name);
        item.setItemType(type);
        item.setWorkFlowItem(this);
        outstandingWorkflowItems.add(item);
    }

    public void removeOutstandingWorkflowItemByConstraint(HTPredicate<OutstandingWorkflowItem> constraint, com.hitorro.util.typesystem.BaseSession session) {
        Iterator<OutstandingWorkflowItem> iter = outstandingWorkflowItems.iterator();
        while (iter.hasNext()) {
            OutstandingWorkflowItem item = iter.next();
            if (constraint.test(item)) {
                session.deleteObjectIfExists(item.getItemGuid());
                iter.remove();
            }
        }

    }

    public Set<OutstandingWorkflowItem> getOutstandingWorkflowItems() {
        return outstandingWorkflowItems;
    }

    public void setoutstandingWorkflowItems(Set<OutstandingWorkflowItem> outstandingWorkflowItems) {
        this.outstandingWorkflowItems = outstandingWorkflowItems;
    }

    public Set<WorkFlowItemEntry> getWorkflowItemEntries() {
        return workflowItemEntries;
    }

    public void setWorkflowItemEntries(Set<WorkFlowItemEntry> workflowItemEntries) {
        this.workflowItemEntries = workflowItemEntries;
    }

    public String getStateMachine() {
        return stateMachine;
    }

    public void setStateMachine(String stateMachine) {
        this.stateMachine = stateMachine;
    }

    public String getFromUser() {
        return Constants.EmptyString;
    }

    public String getContentTitle() {
        String title = Constants.EmptyString;

        //   todo: fix this!!!
        com.hitorro.util.typesystem.BaseSession session = com.hitorro.util.typesystem.BaseSessionFactory.getFactory().getSession();

        try {
            if (session != null) {
                VersionableObject so = getVersionableObjectFromEntryByKey(UploadDocKey, session, false);

                if (so instanceof Document) {
                    Document document = (Document) so;

                    title = document.getTitle();
                }
            }
        } finally {
            session.rollback();
        }
        return title;
    }

    public Date getEffectiveDate() {
        return getCreationDate();
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public WorkFlowItem getWorkFlowItem() {
        return this;
    }

    public PersistedSerializedObject getPSOFromEntryByKey(String key, DMSSession session, boolean throwError) {
        com.hitorro.util.typesystem.HTSerializable o = getObjectFromEntryByKey(key, session, throwError);
        if (o == null) {
            return null;
        }

        if (o instanceof PersistedSerializedObject) {
            return (PersistedSerializedObject) o;
        }
        if (throwError) {
            Log.workflow.error("Item retrieved is not a PSO key:%s for wfi.guid:%s, class: %s", key, getGuid(), o.getClass().getCanonicalName());
        }
        return null;
    }

    public VersionableObject getVersionableObjectFromEntryByKey(String key, com.hitorro.util.typesystem.BaseSession session, boolean throwError) {
        com.hitorro.util.typesystem.HTSerializable o = getObjectFromEntryByKey(key, session, throwError);

        if (o == null) {
            return null;
        }


        if (o instanceof VersionableObject) {
            return (VersionableObject) o;
        }
        if (throwError) {
            Log.workflow.error("Item retrieved is not a VersionableObject key:%s for wfi.guid:%s, class: %s", key, getGuid(), o.getClass().getCanonicalName());
        }
        return null;
    }

    public com.hitorro.util.typesystem.HTSerializable getObjectFromEntryByKey(String key, com.hitorro.util.typesystem.BaseSession session, boolean throwError) {
        String guid = getGuidFromEntry(key);
        if (StringUtil.nullOrEmptyString(guid)) {
            if (throwError) {
                Log.workflow.error("Unable to find PSO with key:%s guid: %s for wfi.guid:%s", key, guid, getGuid());
            }
            return null;
        }
        com.hitorro.util.typesystem.HTSerializable o = session.getObjectFromGuid(guid);
        if (o == null) {
            if (throwError) {
                Log.workflow.error("Unable to find Object with key:%s guid: %s for wfi.guid:%s", key, guid, getGuid());
            }
            return null;
        }
        return o;
    }

    public String getWorkflowStateDescription() {
        com.hitorro.util.statemachine.MooreStateMachine sm = com.hitorro.util.statemachine.StateMachineService.getService().getStateMachine(this.getStateMachine());

        if (sm == null) {
            Log.workflow.error("Unable to find state machine %s", getStateMachine());
            return "Unknown";
        }

        com.hitorro.util.statemachine.State currState = sm.getState(this.getCurrentState());
        return currState.getDescription();
    }

    public String getWorkflowStateName() {
        com.hitorro.util.statemachine.MooreStateMachine sm = com.hitorro.util.statemachine.StateMachineService.getService().getStateMachine(this.getStateMachine());

        if (sm == null) {
            Log.workflow.error("Unable to find state machine %s", getStateMachine());
            return "Unknown";
        }

        com.hitorro.util.statemachine.State currState = sm.getState(this.getCurrentState());
        return currState.getName();
    }

    public boolean notify(com.hitorro.util.typesystem.HTSerializable pts, com.hitorro.util.typesystem.BaseSession session) {
        session.enableCache(false);
        Log.workflow.debug("notify called on workflow item %s, stateMachine: %s, state: %s", this.getGuid(),
                this.getStateMachine(),
                this.getCurrentState());
        if (pts != null && pts instanceof JobParameters) {

            String ptsState = ((JobParameters) pts).getNotifyGuidState();
            if (!getCurrentState().equalsIgnoreCase(ptsState)) {
                // params was from another state, throw away.
                Log.workflow.info("JobParams passed to workflow item notify were for state %s but we are in %s, wfi.guid: %s ",
                        getCurrentState(), ptsState, getGuid());
                // still return true as we want to just ignore it and move on.
                return true;
            }
            Log.workflow.debug("JobParameters object of type %s passed in", pts.getClass().getCanonicalName());
        }
        com.hitorro.util.statemachine.MooreStateMachine sm = com.hitorro.util.statemachine.StateMachineService.getService().getStateMachine(this.getStateMachine());

        if (sm == null) {
            Log.workflow.error("Unable to find state machine %s", getStateMachine());
            return false;
        }
        com.hitorro.util.statemachine.State currState = sm.getState(this.getCurrentState());
        if (currState == null) {
            Log.workflow.error("Unable to find state %s for state machine %s for item: %s",
                    getCurrentState(),
                    getStateMachine(),
                    this.getGuid());
            return false;
        }
        WorkflowStateContext context = new WorkflowStateContext();
        context.setSession(session);
        context.setNotificationItem(pts);
        context.setWorkflowItem(this);

        while (true) {
            com.hitorro.util.statemachine.DirectedEdge edges[] = currState.getEdges();
            boolean hit = false;

            for (com.hitorro.util.statemachine.DirectedEdge edge : edges) {
                com.hitorro.util.statemachine.Validator<WorkflowStateContext> validator = edge.getValidator();
                if (validator != null) {
                    if (validator.validate(context, edge, edge.getNextState())) {
                        Log.workflow.debug("Validator: %s returned true for workflow item:%s in state: %s transitioning to state: %s",
                                validator.getClass().getCanonicalName(),
                                this.getGuid(),
                                currState.getName(),
                                edge.getNextState().getName());
                        currState = edge.getNextState();
                        this.setCurrentState(currState.getName());
                        context.setCurrentState(currState);

                        com.hitorro.util.statemachine.Action<WorkflowStateContext, Object> action = currState.getModifier();
                        hit = true;
                        if (action != null) {
                            try {
                                if (!action.
                                        modifyState(context, null)) {
                                    Log.workflow.error("Unable to execute action %s for %s",
                                            action.getClass().getCanonicalName(),
                                            this.getGuid());
                                    return false;
                                }
                                Log.workflow.debug("Action executed: %s for workflow item:%s in state: %s",
                                        action.getClass().getCanonicalName(),
                                        this.getGuid(),
                                        currState.getName());
                                // ensure we dont carry forward the notification to the next state.
                                context.setNotificationItem(null);

                            } catch (Exception e) {
                                Log.statemachine.error("%s %e", e, e);
                                return false;
                            }
                        }

                        break;

                    } else {
                        Log.workflow.debug("Validator: %s returned false for workflow item:%s in state: %s",
                                validator.getClass().getCanonicalName(),
                                this.getGuid(),
                                currState.getName());
                    }
                }
            }
            if (hit) {
                session.commit();
                continue;
            }
            return true;
        }
    }

    public boolean getFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
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

}
