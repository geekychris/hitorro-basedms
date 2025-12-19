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
package com.hitorro.basedms.transformer.debugcommand;

import com.hitorro.base.objects.PersistedSerializedObject;
import com.hitorro.base.objects.User;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.basedms.transformer.squeeze.SqueezeService;
import com.hitorro.basedms.workflow.WorkFlowItem;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.commandandcontrol.ano.RespColumn;
import com.hitorro.util.commandandcontrol.ano.ResponseDefinition;
import com.hitorro.util.core.Constants;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.job.DecisionJobParameters;
import com.hitorro.util.typesystem.BaseSession;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@CommandDef(command = "transform.dumpworkflowtranscodequeue", description = "Dump all outstanding workflows in transcode queue")
public class DumpWorkflowTranscodeQueue extends com.hitorro.util.commandandcontrol.Command {
    @ResponseDefinition(command = "workflow",
            rowname = "item",
            columns = {@RespColumn(name = "PSO Guid", lName = "psoguid"),
                    @RespColumn(name = "PSO Name", lName = "psoname"),
                    @RespColumn(name = "Effective Date", lName = "effectivedate"),
                    @RespColumn(name = "WFI Creator", lName = "wficreator"),
                    @RespColumn(name = "From", lName = "from"),
                    @RespColumn(name = "WFI Guid", lName = "wfiguid"),
                    @RespColumn(name = "Job Name", lName = "jobinfo"),
                    @RespColumn(name = "Description", lName = "description"),
                    @RespColumn(name = "WFI State", lName = "wfistate"),
                    @RespColumn(name = "Content Guid", lName = "contguid"),
                    @RespColumn(name = "Content Title", lName = "conttitle")})
    private com.hitorro.util.commandandcontrol.ResponseShape shape = new com.hitorro.util.commandandcontrol.ResponseShape();

    public boolean execute(String rawValue, JVS args, com.hitorro.util.commandandcontrol.Response response, com.hitorro.util.commandandcontrol.CommandSession commandSession, com.hitorro.util.commandandcontrol.RestOperations operation) throws Exception {
        SqueezeService service = SqueezeService.getService();

        if (service == null) {
            return false;
        }

        BaseSession session = DMSSessionFactory.getFactory().getSession();

        try {
            List<String> results = getWorkflowQueue(session);
            response.setResponseShape(shape);

            Iterator<String> iterator = results.iterator();
            while (iterator.hasNext()) {
                Object[] responseRow = getResponseRow(session, iterator.next());
                response.addRow(responseRow);
            }
            response.end();
        } finally {
            DMSSessionFactory.getFactory().rollbackClose(session);
        }

        return true;
    }


    private List getWorkflowQueue(BaseSession session) {
        List<String> results = new ArrayList<String>();


        if (session != null) {
            String sql = "SELECT guid FROM PersistedSerializedObject WHERE collectionId = :collectionId ORDER BY effectiveFrom DESC";
            NativeQuery query = ((DMSSession) session).createSQLQuery(sql);
            query.setParameter("collectionId", PersistedSerializedObject.CollectionID_UIJobQueue);
            query.addScalar("guid", StandardBasicTypes.STRING);
            results = query.list();
        }
        return results;
    }

    private Object[] getResponseRow(BaseSession session, String psoGuid) {
        PersistedSerializedObject pso = (PersistedSerializedObject) session.getObjectFromGuid(psoGuid);
        DecisionJobParameters djp = null;
        String[] rowElements = new String[11];

        rowElements[0] = psoGuid;

        if (pso != null) {
            rowElements[1] = pso.getName();
            rowElements[2] = pso.getEffectiveFrom().toString();

            String creatorGuid = pso.getExecutor();

            if (!StringUtil.nullOrEmptyString(creatorGuid)) {
                User creator = (User) session.getObjectFromGuid(creatorGuid);
                rowElements[3] = creator.getName();
            }

            try {
                djp = (DecisionJobParameters) pso.getSerializableObject();
            } catch (SQLException e) {
                Log.workflow.error("Unable to retrieve workflow DecisionJobParameters %s %e", e, e);
            } catch (IOException e) {
                Log.workflow.error("Unable to retrieve workflow DecisionJobParameters %s %e", e, e);
            } catch (StoreException e) {
                Log.workflow.error("Unable to retrieve workflow DecisionJobParameters %s %e", e, e);
            } catch (ClassNotFoundException e) {
                Log.workflow.error("Unable to retrieve workflow DecisionJobParameters %s %e", e, e);
            }

            if (djp != null) {
                rowElements[4] = djp.getFromUser();
                rowElements[5] = djp.getPayloadGuid();
                rowElements[6] = djp.getJobName();
                rowElements[7] = djp.getDescription();
            }

            WorkFlowItem wfi = (WorkFlowItem) session.getObjectFromGuid(djp.getPayloadGuid());

            if (wfi != null) {

                rowElements[8] = wfi.getCurrentState();

                VersionableObject sysobject = wfi.getVersionableObjectFromEntryByKey(WorkFlowItem.UploadDocKey, session, true);

                if (sysobject != null) {
                    rowElements[9] = sysobject.getGuid();
                    rowElements[10] = wfi.getContentTitle();
                }
            }
        }

        for (String element : rowElements) {
            if (StringUtil.nullOrEmptyOrBlankString(element)) {
                element = Constants.EmptyString;
            }
        }

        return rowElements;
    }


}
