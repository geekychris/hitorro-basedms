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
package com.hitorro.util.job;

import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.log.Logger;
import org.apache.log4j.Level;

/**
 * <p/>
 * Created by a job if it has something to say.  Such as:
 * <p/>
 * I failed, please retry me later. I failed and I am giving up Here is my update parameters for the retry.
 */
public class JobExecutionResult {
    public static JobExecutionResult Executed = new JobExecutionResult();

    private boolean m_shouldRetry = false;
    private int m_timeMinutes = 0;
    private JobParameters m_jobParameters = null;
    private boolean m_hasMessage = false;
    private Level m_messageLevel = Level.INFO;
    private String m_message = null;
    private Object m_messageArgs[] = null;

    private boolean m_completedRewrite = false;
    private String m_newEventName;
    private int m_collectionId;

    protected JobExecutionResult() {
        //

    }

    public JobExecutionResult(int retryTimeInMinutes, JobParameters params) {
        m_shouldRetry = true;
        m_timeMinutes = retryTimeInMinutes;
        m_jobParameters = params;
    }

    public JobExecutionResult(Level level, String message, Object... msgArgs) {
        setMessage(level, message, msgArgs);
    }

    /**
     * Set the name and collection Id if we are complete and want to simply rename the PSO (if held in a pso) to a new
     * non queue entry.
     *
     * @param name
     * @param collectionId
     */
    public void setNewEventName(String name, int collectionId) {
        m_completedRewrite = true;
        m_newEventName = name;
        m_collectionId = collectionId;
    }

    public boolean getCompleteWithRewrite() {
        return m_completedRewrite;
    }

    public String getNewEventName() {
        return m_newEventName;
    }

    public int getCollectionID() {
        return m_collectionId;
    }

    public void setMessage(Level level, String msg, Object... args) {
        m_messageLevel = level;
        m_message = msg;
        m_messageArgs = args;
    }

    /**
     * Output the error message to the logger provided.
     *
     * @param logger
     */
    public void applyMessageToLogger(Logger logger) {
        if (m_hasMessage) {
            String msg = getMessage();
            if (m_messageLevel == Level.FATAL) {
                logger.fatal(msg);
            } else if (m_messageLevel == Level.ERROR) {
                logger.error(msg);
            } else if (m_messageLevel == Level.WARN) {
                logger.warn(msg);
            } else {
                logger.info(msg);
            }
        }
    }

    public String getMessage() {
        if (m_hasMessage) {
            if (m_messageArgs == null || m_messageArgs.length > 0) {
                return Fmt.S(m_message, m_messageArgs);
            }
            return m_message;
        }
        return null;
    }

    public Level getErrorLevel() {
        return m_messageLevel;
    }

    public boolean isGood() {
        // good is when an error message was not provided
        return m_messageLevel == Level.INFO;
    }

    public boolean shouldRetry() {
        return m_shouldRetry;
    }

    public int getRetryMinutes() {
        return m_timeMinutes;
    }

    public JobParameters getJobParameters() {
        return m_jobParameters;
    }

    public void setJobParameters(JobParameters jp) {
        m_jobParameters = jp;
    }
}
