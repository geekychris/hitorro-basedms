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
package com.hitorro.basedms.transformer.squeeze;

import com.hitorro.basedms.BaseUtil;
import com.hitorro.basedms.transformer.TransformMethod;
import com.hitorro.basedms.workflow.DormontWorkflowItemTickler;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.basefile.fs.file.FileFile;
import com.hitorro.util.basefile.fs.file.FileFileSystem;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.io.filefilters.FileStartsEndsWith;
import com.hitorro.util.json.keys.IntegerProperty;
import com.hitorro.util.statemachine.Log;

import java.io.File;
import java.io.IOException;

/**

 * <p>
 * Though we are given a BaseFile, this implementation only works with a local file system
 */
public class SqueezeWatchDir implements TransformMethod {
    public static final String CompressedOutput = "CompressedOutput";
    public static final String CompletedSource = "CompletedSource";
    private static final IntegerProperty TranscodePollingInterval = new IntegerProperty("transcoder.squeeze.PollingDelay", "ProducerApp interval in polling Sorenson Squeeze activity (seconds)", 5);
    private static final IntegerProperty TranscodePollingRetriesMax = new IntegerProperty("transcoder.squeeze.PollingRetriesMax", "ProducerApp retries for detecting Sorenson Squeeze activity", 200);
    private static final IntegerProperty WorkflowItemTouchInterval = new IntegerProperty("transcoder.squeeze.WorkflowItemTouchInterval", "ProducerApp interval touching underlying WorkflowItem (seconds)",
            DormontWorkflowItemTickler.MinutesToWaitBetweenPoll.apply() * com.hitorro.util.core.Constants.SecondsInMinute / TranscodePollingInterval.apply() / 4);


    private File m_root;
    private File m_temp;
    private File m_completed;
    private File m_completedSource;

    private String m_method;


    public SqueezeWatchDir(File root, File temp) {
        m_root = root;
        m_temp = temp;
        m_method = root.getName();
        m_completed = new File(m_root, CompressedOutput);
        m_completedSource = new File(m_root, CompletedSource);
    }


    /**
     * synchronous method to convertToPdf a file.
     *
     * @param fIn
     * @param parameters
     * @return file if converted.  Caller responsible for removal of
     */
    public BaseFile convert(BaseFile fIn, String id, String parameters, String notifyGuid, int maxWaitTimeMinutes) throws IOException {
        if (!fIn.isLocal()) {
            return null;
        }
        File f = ((FileFile) fIn).getJavaFile();

        boolean link = false;
        File q = null;

        try {
            q = queue(f, id, link);

            int transcodePollingRetry = 0;
            int workflowItemTouchCounter = 0;
            boolean isTranscodeInProgress = false;
            SqueezeService service = SqueezeService.getService();
            com.hitorro.util.core.Env.sleepNSeconds(TranscodePollingInterval.apply());

            while (true) {
                Log.workflow.info("Sorenson Squeeze in process = %s", service.isTranscodeInProgress(id));
                com.hitorro.util.core.Console.println("Sorenson Squeeze in process = %s", service.isTranscodeInProgress(id));
                if (getCompletedById(id) != null) {
                    // it either converted it or not.
                    return FileFileSystem.Root.getFile(getConvertedById(id).getAbsolutePath());
                }

                com.hitorro.util.core.Env.sleepNSeconds(TranscodePollingInterval.apply());
                isTranscodeInProgress = service.isTranscodeInProgress(id);
                workflowItemTouchCounter++;

                if (!isTranscodeInProgress) {
                    transcodePollingRetry++;
                } else {
                    transcodePollingRetry = 0;
                }


                if (workflowItemTouchCounter % WorkflowItemTouchInterval.apply() == 0) {
                    BaseUtil.touchObject(notifyGuid);
                }


                if (!isTranscodeInProgress && transcodePollingRetry >= TranscodePollingRetriesMax.apply()) {
                    // XXX timed out (perhaps we should have an exception we throw.
                    Log.workflow.info("Sorenson Squeeze timeout processing %s; isTranscodeInProgress: %s; transcodePollingRetry: %s: TranscodePollingRetriesMax: %s", id, isTranscodeInProgress, transcodePollingRetry, TranscodePollingRetriesMax);
                    return null;
                }
            }
        } finally {
            if (!link) {
                if (FileUtil.notNullAndExists(q)) {
                    q.delete();
                }
            }

            File completed = getCompletedById(id);
            if (FileUtil.notNullAndExists(completed)) {
                completed.delete();
            }
        }
    }


    public boolean ensureServiceAvailable() {
        SqueezeService.getService().startIfNotStarted();
        return true;
    }


    public File queue(File source, String id, boolean link) throws IOException {
        if (link) {
            return queueWithLink(source, id);
        } else {
            return queueWithCopy(source, id);
        }
    }


    /**
     * given a file assume its on a different partition and copy it.  First copy it to a tmp location and then move it
     * into the right place.
     *
     * @param source
     * @param id
     * @return true if successfull.
     */
    public File queueWithCopy(File source, String id) throws IOException {
        String file = getFile(source, id);
        File temp = new File(m_temp, file);

        FileUtil.copy(source, temp);

        File watchFile = new File(m_root, file);
        temp.renameTo(watchFile);
        return watchFile;
    }


    /**
     * Determine if the id is to be found in the completed dir.  This does not mean it was successfull.   Sorenson will
     * move the file into completed source even if it failed.  This is
     *
     * @param id
     * @return file if found
     */
    public File getCompletedById(String id) {
        FileStartsEndsWith filter = new FileStartsEndsWith(id, true, false);
        File files[] = this.m_completedSource.listFiles(filter);
        if (com.hitorro.util.core.ArrayUtil.nullOrEmpty(files)) {
            return null;
        }
        return files[0];
    }


    /**
     * If the file exists then
     *
     * @param id
     * @return
     */
    public File getConvertedById(String id) {
        FileStartsEndsWith filter = new FileStartsEndsWith(id, true, false);
        File files[] = this.m_completed.listFiles(filter);
        if (com.hitorro.util.core.ArrayUtil.nullOrEmpty(files)) {
            return null;
        }
        return files[0];
    }


    public boolean removeId(String id) {
        File compressed = getConvertedById(id);
        File completed = getCompletedById(id);

        FileUtil.deleteIfNotNull(compressed);

        FileUtil.deleteIfNotNull(completed);
        return true;
    }


    public File queueWithLink(File source, String id) throws IOException {
        String file = getFile(source, id);
        File watchFile = new File(m_root, file);

        if (com.hitorro.util.core.Platform.getPlatform().softLink(source, watchFile)) {
            return watchFile;
        }
        return null;
    }


    private String getFile(File source, String id) {
        String extension = FileUtil.getFileExtension(source);
        String file = null;
        if (StringUtil.nullOrEmptyString(extension)) {
            // XXX probably need todo something to figure out an extension
            file = id;
        } else {
            file = Fmt.S("%s.%s", id, extension);
        }
        return file;
    }


    public String getMethodName() {
        return m_method;
    }
}
