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
package com.hitorro.basedms.queue;

import com.hitorro.base.objects.PersistedSerializedObject;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.job.JobExecutionResult;
import com.hitorro.util.typesystem.HTSerializable;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Element that enters a workQueue.
 */
public class JobQueueElement<T> {
    private String m_guid;
    private T m_payload;
    private JobExecutionResult m_result;
    private Object groupId = 0;

    private long m_notBeforeTime = 0;

    public JobQueueElement(T pso, String guid) {
        m_payload = pso;
        m_guid = guid;
        if (m_payload instanceof PersistedSerializedObject) {
            try {
                HTSerializable param = ((PersistedSerializedObject) pso).getObject();
                if (param instanceof GroupId) {
                    groupId = ((GroupId) param).getGroupId();
                }
            } catch (SQLException e) {

            } catch (IOException e) {

            } catch (ClassNotFoundException e) {

            } catch (StoreException e) {

            }
        }

    }

    public long getNotBeforeTime() {
        return m_notBeforeTime;
    }

    public Object getGroupId() {
        return groupId;
    }

    public T getPayload() {
        return m_payload;
    }

    public String getGuid() {
        return m_guid;
    }

    public JobExecutionResult getResult() {
        return m_result;
    }

    public void setResult(JobExecutionResult jer) {
        m_result = jer;
    }

    public Object getGroup() {
        return groupId;
    }
}
