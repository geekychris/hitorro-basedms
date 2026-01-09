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

import com.megginson.sax.XMLWriter;
import com.hitorro.basedms.transformer.Log;
import com.hitorro.basedms.transformer.TransformerService;
import com.hitorro.network.rpc.cluster.ClusterService;
import com.hitorro.util.core.Console;
import com.hitorro.util.core.Constants;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.io.RotatingBufferedOutputStream;
import com.hitorro.util.json.keys.FileProperty;
import com.hitorro.util.json.keys.IntegerProperty;
import com.hitorro.util.json.keys.StringProperty;
import com.hitorro.util.osprocessexec.ExecContext;
import com.hitorro.util.osprocessexec.ProcessUtil;
import com.hitorro.util.osprocessexec.TerminationKey;
import com.hitorro.util.startupframework.phases.ServiceDefinition;
import com.hitorro.util.xml.SAXUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**

 * Sorenson Squeeze service.   This service is only applicable for machines (macs) that have Sorenson 4.5 Squeeze
 * installed
 */
@ServiceDefinition(dependentService = {TransformerService.class},
        shortName = "squeeze",
        description = "Squeeze service",
        debugCommands = {},
        typeManagedClasses = {},
        uiDirectories = {})
public class SqueezeService implements TerminationKey {
    public static final String SqueezeKey = "squeeze";

    public static final IntegerProperty RotatingBufferSize = new IntegerProperty("transcoder.sorenson.rotatingbuffersize",
            "Size of the out and err buffers used by running clients",
            1024 * 10);
    private static final FileProperty SorensonExec = new FileProperty("transcoder.sorenson.exe",
            "Sorenson squeeze executable",
            "/Applications/Squeeze/Squeeze.app/Contents/MacOS/Squeeze");
    private static final FileProperty SorensonProject = new FileProperty("transcoder.sorenson.config",
            "Sorenson squeeze configuration",
            "${HT_BIN}/data/transcoder/sorenson/pt_3.sqz");
    private static final FileProperty SorensonProjectOut = new FileProperty("transcoder.sorenson.configout",
            "Sorenson squeeze configuration",
            "${HT_HOME}/data/transcoder/sorenson/pt_generated.sqz");
    private static final FileProperty SorensonProjectWatches = new FileProperty("transcoder.sorenson.watchdir",
            "Sorenson squeeze configuration",
            "${HT_HOME}/data/transcoder/sorenson/watch");
    private static final StringProperty SorensonHost = new StringProperty("transcoder.squeeze.host", "sorenson squeeze default host", "127.0.0.1");
    private static final IntegerProperty SorensonPort = new IntegerProperty("transcoder.squeeze.port", "sorenson squeeze default port", 9876);
    private static final IntegerProperty SorensonTimeOut = new IntegerProperty("transcoder.squeeze.timeout", "sorenson squeeze timeout between status updates",
            (int) Constants.MillisInSecond * Constants.SecondsInMinute * 15);
    private static SqueezeService s_service;
    protected int m_exitCode;
    private File m_temp;
    private ExecContext m_ec = null;
    private boolean m_isRunning = false;
    private boolean shouldKeepRunning = true;
    private RotatingBufferedOutputStream m_out;
    private RotatingBufferedOutputStream m_err;
    private Map<String, File> m_watches = new HashMap<String, File>();
    private String processingFile;
    private String processingPercentComplete;
    private long processingLastUpdate;
    private SqueezeListener squeezeListener = new SqueezeListener(SorensonHost.apply(), SorensonPort.apply(), this);
    private Thread squeezeListenerThread;

    public static SqueezeService getService() {
        return s_service;
    }

    public void addWatch(File watch) {
        String name = watch.getName();
        m_watches.put(name, watch);
        SqueezeWatchDir watcher = new SqueezeWatchDir(watch, m_temp);
        TransformerService.getService().setMethod(watcher);
    }

    public String init(boolean dbInit, final boolean upgrading, final long currentVersion, final long targetVersion) {
        try {
            File file = SorensonExec.apply();
            if (FileUtil.notNullAndExists(file)) {
                // first make sure squeeze isnt running
                int count = ProcessUtil.killProcesses("squeeze", "java");
                if (count > 0) {
                    Log.transformer.info("Killed %s sorenson squeeze processes", file);
                }

                m_temp = new File(SorensonProjectWatches.apply(), "temp");
                boolean success = getWatchDirectories(SorensonProject.apply(),
                        SorensonProjectOut.apply(),
                        SorensonProjectWatches.apply());
                s_service = this;
                ClusterService.getThisInstanceDefinition().addInstanceCapability(SqueezeKey, "", "", "", true);
            } else {
                Log.transformer.warn("SquuezeService could not find a Sorenson Squeeze binary: %s", SorensonExec.apply());
            }
        } catch (IOException e) {
            Log.transformer.error("%s %e", e, e);
            return e.getMessage();
        } catch (ParserConfigurationException e) {
            Log.transformer.error("%s %e", e, e);
        } catch (SAXException e) {
            Log.transformer.error("%s %e", e, e);
        } catch (InterruptedException e) {
            Log.transformer.error("%s %e", e, e);
        }
        return null;
    }

    public String start(boolean dbInit) {

        return null;
    }

    public void startIfNotStarted() {
        if (m_isRunning == false) {
            m_isRunning = start();
        }
    }

    public String deInit() {
        shouldKeepRunning = false;
        if (m_isRunning) {
            if (m_ec != null) {
                m_ec.destroy();
            }
        }
        return null;
    }

    public void complete(int exitCode) {
        Log.util.info("Sorenson exited with exit code %s", exitCode);
        m_isRunning = false;
        m_exitCode = exitCode;
        m_isRunning = false;
        if (shouldKeepRunning) {
            m_isRunning = start();
        }
    }

    private boolean start() {
        m_ec = new ExecContext(this);
        m_out = new RotatingBufferedOutputStream(RotatingBufferSize.apply());
        m_err = new RotatingBufferedOutputStream(RotatingBufferSize.apply());
        m_ec.setOutput(m_out);
        m_ec.setError(m_err);
        String[] args = new String[2];
        String execString = Fmt.S("%s %s -s",
                SorensonExec.apply().toString(),
                SorensonProjectOut.apply().toString());
        m_ec.setProgram(execString);
        //m_ec.setArgs(args);
        try {
            m_ec.exec();
            m_isRunning = true;
            Log.util.info("Started %s", execString);
            squeezeListenerThread = new Thread(squeezeListener);
            squeezeListenerThread.start();
            return true;
        } catch (IOException e) {
            Log.util.error("%s %e", e, e);
        }
        return false;
    }


    private boolean getWatchDirectories(File sourceConfig, File targetConfig, File watchDir)
            throws IOException, ParserConfigurationException, SAXException {
        WatchDirHandler handler = new WatchDirHandler(targetConfig, watchDir, this);
        Log.transformer.debug("Translating SQZ file: %s target: %s, watchDir: %s", sourceConfig, targetConfig, watchDir);
        SAXUtil.readSax(sourceConfig, handler);
        handler.close();
        return true;
    }


    public synchronized void setProcessingFile(String file, String percent) {
        setProcessingFile(file);
        setProcessingPercentComplete(percent);
        setProcessingLastUpdate(System.currentTimeMillis());
        Console.println("Sorenson Squeeze file %s; percentage %s; time %s", processingFile, processingPercentComplete, processingLastUpdate);
        com.hitorro.util.statemachine.Log.workflow.info("Sorenson Squeeze file %s; percentage %s; time %s", processingFile, processingPercentComplete, processingLastUpdate);
    }


    public boolean isTranscodeInProgress(String jobId) {
        boolean inProgress = false;

        if (jobId.equals(getProcessingJobId())) {
            String percentComplete = getProcessingPercentComplete();
            Long timeLastUpdate = getProcessingLastUpdate();
            Long timeCurrent = System.currentTimeMillis();

            if (!StringUtil.nullOrEmptyOrBlankString(percentComplete)) {
                int percent = Integer.valueOf(percentComplete);

                if (percent >= 0 && percent <= 100) {
                    if ((timeCurrent - timeLastUpdate) < SorensonTimeOut.apply()) {
                        inProgress = true;
                    }
                }
            }
        } else {
            Console.println("Sorenson Squeeze is actually processing job: %s, SqueezeService is watching status for job: %s", getProcessingJobId(), jobId);
            com.hitorro.util.statemachine.Log.workflow.info("Sorenson Squeeze is processing job: %s, SqueezeService is watching status for job: %s", getProcessingJobId(), jobId);
        }

        return inProgress;
    }


    public synchronized String getProcessingFile() {
        return processingFile;
    }


    private void setProcessingFile(String processingFile) {
        this.processingFile = processingFile;
    }


    public synchronized String getProcessingPercentComplete() {
        return processingPercentComplete;
    }


    private void setProcessingPercentComplete(String processingPercentComplete) {
        this.processingPercentComplete = "-1";

        if (!StringUtil.nullOrEmptyOrBlankString(processingPercentComplete)) {
            this.processingPercentComplete = processingPercentComplete.substring(0, processingPercentComplete.lastIndexOf("%"));
        }
    }


    public synchronized long getProcessingLastUpdate() {
        return processingLastUpdate;
    }


    private void setProcessingLastUpdate(long processingLastUpdate) {
        this.processingLastUpdate = processingLastUpdate;
    }


    public String getProcessingJobId() {
        String processingFile = getProcessingFile();
        String jobId = null;

        if (!StringUtil.nullOrEmptyOrBlankString(processingFile)) {
            jobId = FileUtil.getFileNameSansExtension(processingFile);
        }

        return jobId;

    }
}

class WatchDirHandler extends DefaultHandler {
    public static final String Source = "Source";
    public static final String FileName = "FileName";

    private File m_watchDir;
    private File m_targetConfig;
    private XMLWriter m_writer;
    private SqueezeService m_service;
    private PrintWriter printWriter;

    private File currWatchDir = null;

    private String oldPath;
    private String newPath;


    public WatchDirHandler(File targetConfig, File watchDir, SqueezeService service) {
        m_targetConfig = targetConfig;
        m_watchDir = watchDir;
        m_service = service;
        FileUtil.ensureParentDirectories(m_targetConfig, true);
        m_targetConfig.delete();
        FileUtil.ensureDirectoryExists(m_watchDir);
        m_writer = new XMLWriter();
        printWriter = FileUtil.getBufferedPrintWriterFromFile(m_targetConfig);
        m_writer.setOutput(printWriter);
    }


    public void close() throws IOException {
        m_writer.flush();
        printWriter.close();
    }


    public void startDocument() throws SAXException {
        m_writer.startDocument();
    }


    public void endDocument() throws SAXException {
        m_writer.endDocument();
    }


    /**
     * When you see a start tag, print it out and then increase indentation by two spaces. If the element has
     * attributes, place them in parens after the element name.
     */

    public void startElement(String namespaceUri,
                             String localName,
                             String qualifiedName,
                             Attributes attributes)
            throws SAXException {

        // attributes processing here.
        int numAttributes = attributes.getLength();
        AttributesImpl attribs = new AttributesImpl();
        if (numAttributes > 0) {
            for (int i = 0; i < numAttributes; i++) {
                String name = attributes.getQName(i);
                String value = attributes.getValue(i);
                if (qualifiedName.equals(Source) && name.equals(FileName)) {
                    Log.transformer.debug("SQZ file me source filename: %s, value: %s", name, value);
                    this.oldPath = value;
                    String fName = FileUtil.getFileName(value);
                    File newFName = new File(m_watchDir, fName);
                    value = newFName.getAbsolutePath();

                    this.newPath = value;
                    currWatchDir = newFName;
                    m_service.addWatch(newFName);
                } else if (qualifiedName.equals(FileName) && name.equals("Value")) {

                    /*String right = value.substring(this.oldPath.length());
                    File compressed = new File(this.newPath, right);
                    FileUtil.ensureParentDirectories(compressed, true);
                    File source = new File(compressed.getParentFile().getParentFile(), "CompletedSource");
                    FileUtil.ensureDirectoryExists(source);
                    String ext = FileUtil.getFileExtension(compressed);
                    File compressed2 = new File(compressed.getParentFile(), StringUtil.strcat(".", ext));
                    value = compressed2.toString();     */
                    File compDir = new File(currWatchDir, SqueezeWatchDir.CompressedOutput);
                    FileUtil.ensureDirectoryExists(compDir);
                    File file = new File(value);
                    String ext = FileUtil.getFileExtension(file);

                    value = Fmt.S("%s/.%s", compDir.getAbsolutePath(), ext);
                }
                attribs.addAttribute("", name, "", "", value);

            }
        }
        attributes = attribs;

        m_writer.startElement(namespaceUri, qualifiedName, localName, attributes);
    }


    /**
     * When you see the end tag, print it out and decrease indentation level by 2.
     */

    public void endElement(String namespaceUri,
                           String localName,
                           String qualifiedName)
            throws SAXException {
        {
            m_writer.endElement(namespaceUri, qualifiedName, localName);
        }
    }


    /**
     * Print out the first word of each tag body.
     */

    public void characters(char[] chars,
                           int startIndex,
                           int endIndex) throws SAXException {
        m_writer.characters(chars, startIndex, endIndex);
    }
}
