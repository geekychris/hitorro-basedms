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
package com.hitorro.basedms.db;

import com.hitorro.jsontypesystem.JVS;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import org.hibernate.stat.Statistics;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Dec 17, 2006 Time: 11:07:35 AM
 */
@CommandDef(command = "dms.dumphibernatestats", description = "Dump the hibernate statistics")
public class DumpHibernateStatistics extends com.hitorro.util.commandandcontrol.Command {
    public boolean execute(String rawValue, JVS args, com.hitorro.util.commandandcontrol.Response response, com.hitorro.util.commandandcontrol.CommandSession session, com.hitorro.util.commandandcontrol.RestOperations operation) throws Exception {
        Statistics stats = HibernateService.s_service.getStatistics();
        if (stats == null) {
            response.addInfo(com.hitorro.util.commandandcontrol.InfoLevel.Error, "hibernate statistics not enabled.");
            return false;
        }
        response.setResponseShape(getKVShape());

        response.addRow("start time", stats.getStartTime());
        response.addRow("sessions opened", stats.getSessionOpenCount());
        response.addRow("sessions closed", stats.getSessionCloseCount());
        response.addRow("transactions", stats.getTransactionCount());
        response.addRow("optimistic lock failures", stats.getOptimisticFailureCount());
        response.addRow("flushes", stats.getFlushCount());
        response.addRow("connections obtained", stats.getConnectCount());
        response.addRow("statements prepared", stats.getPrepareStatementCount());
        response.addRow("statements closed", stats.getCloseStatementCount());
        response.addRow("second level cache puts", stats.getSecondLevelCachePutCount());
        response.addRow("second level cache hits", stats.getSecondLevelCacheHitCount());
        response.addRow("second level cache misses", stats.getSecondLevelCacheMissCount());
        response.addRow("entities loaded", stats.getEntityLoadCount());
        response.addRow("entities updated", stats.getEntityUpdateCount());
        response.addRow("entities inserted", stats.getEntityInsertCount());
        response.addRow("entities deleted", stats.getEntityDeleteCount());
        response.addRow("entities fetched", stats.getEntityFetchCount());
        response.addRow("collections loaded", stats.getCollectionLoadCount());
        response.addRow("collections updated", stats.getCollectionUpdateCount());
        response.addRow("collections removed", stats.getCollectionRemoveCount());
        response.addRow("collections recreated", stats.getCollectionRecreateCount());
        response.addRow("collections fetched", stats.getCollectionFetchCount());
        response.addRow("queries executed to database", stats.getQueryExecutionCount());
        response.addRow("query cache puts", stats.getQueryCachePutCount());
        response.addRow("query cache hits", stats.getQueryCacheHitCount());
        response.addRow("query cache misses", stats.getQueryCacheMissCount());
        response.addRow("max query time", stats.getQueryExecutionMaxTime());

        response.addRow("", stats.getCloseStatementCount());

        return true;
    }
}
