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
package com.hitorro.basedms.scheduler;

import com.hitorro.jsontypesystem.JVS;
import com.hitorro.util.commandandcontrol.ano.CommandDef;
import com.hitorro.util.commandandcontrol.ano.RespColumn;
import com.hitorro.util.commandandcontrol.ano.ResponseDefinition;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.startupframework.ServiceContext;
import org.quartz.*;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Nov 22, 2006 Time: 11:39:02 AM
 */
@CommandDef(command = "job.listscheduled", description = "Displays a listFiles of jobs currently scheduled for execution")
public class DumpScheduledJobs extends com.hitorro.util.commandandcontrol.Command {
    private static final String DATE_FORMAT = "MM/dd/yyyy HH:mm:ss z";

    @ResponseDefinition(command = "scheduled",
            rowname = "job",
            columns = {@RespColumn(name = "Name", lName = "name"),
                    @RespColumn(name = "Class", lName = "class"),
                    @RespColumn(name = "Next Execution", lName = "next"),
                    @RespColumn(name = "Cron", lName = "cron")})
    private com.hitorro.util.commandandcontrol.ResponseShape shape = new com.hitorro.util.commandandcontrol.ResponseShape();

    public boolean execute(String rawValue, JVS args, com.hitorro.util.commandandcontrol.Response response, com.hitorro.util.commandandcontrol.CommandSession session, com.hitorro.util.commandandcontrol.RestOperations operation) throws Exception {
        SchedulerService module = (SchedulerService) ServiceContext.getSC().getInitializedModule(SchedulerService.class);

        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        Map<JobDetail, Trigger> jobs = null;
        jobs = module.getScheduledJobs();


        response.setResponseShape(shape);
        for (JobDetail job : jobs.keySet()) {
            Trigger trigger = jobs.get(job);
            String cron = "";
            if (trigger instanceof CronTrigger) {
                cron = ((CronTrigger) trigger).getCronExpression();
            }

            response.addRow(job.getKey(), job.getJobClass().getCanonicalName(),
                    formatter.format(trigger.getNextFireTime()), cron);
        }

        Scheduler sc = module.getScheduler();
        List l = null;
        try {
            l = sc.getCurrentlyExecutingJobs();
        } catch (SchedulerException e) {
            Log.util.error("Exception %s %e", e, e);
        }
        response.addInfo(com.hitorro.util.commandandcontrol.InfoLevel.Info, "EXECUTING");
        for (Object o : l) {
            response.addInfo(com.hitorro.util.commandandcontrol.InfoLevel.Info, Fmt.S("executing: %s", o.toString()));
        }

        response.end();
        return true;
    }
}
