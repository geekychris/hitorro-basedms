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
package com.hitorro.basedms.ssh;

import com.hitorro.basedms.ssh.scp.ScpFileStatus;
import com.hitorro.basedms.ssh.scp.ScpFrom;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.jsontypesystem.JVSUtils;
import com.hitorro.util.cmdline.BaseCommandLine;
import com.hitorro.util.core.Env;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.params.HTProperties;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.core.thread.RestartableService;
import com.hitorro.util.core.thread.RestartableServiceDaemon;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.json.keys.IntegerProperty;
import com.hitorro.util.json.keys.StringListFromDelimitedKey;
import com.hitorro.util.json.keys.StringProperty;
import com.hitorro.util.startupframework.phases.ServiceDefinition;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@ServiceDefinition(dependentService = {},
        shortName = "pollingftp",
        description = "polling file transport protocol service",
        debugCommands = {},
        typeManagedClasses = {},
        uiDirectories = {})
public class PollingFileTransferService {
    /*   aggregated file transfer results across hosts.   */
    private HashMap<String, ScpFileStatus> _aggregatedScpFiles = new HashMap<String, ScpFileStatus>();

    /*   properties   */
    private StringProperty _fileTransferPropertiesFileKey;
    private IntegerProperty _fileTransferIntervalKey;


    /*   property representing listFiles of hosts to transfer from    */
    private List<String> _fileTransferHosts;


    /*   define host-specific transfer property values   */
    private String _sourceHost;
    private int _sourcePort;
    private String _sourceUsername;
    private String _sourcePassword;
    private String[] _sourceFilePathname;
    private String _destinationPath;

    public PollingFileTransferService() {
        /*    properties.   bootstrap.  need the properties file and file transfer interval defined asap.   */
        _fileTransferPropertiesFileKey = new StringProperty("file.transfer.propertiesfile",
                "property files defining hosts, users, filepathnames for copying files",
                StringUtil.strcat(Env.getBin(), "/../tools/log_digest/config/origin/FileTransfer.properties"));   // todo fix-up file name
        _fileTransferIntervalKey = new IntegerProperty("file.transfer.interval", "interval between file transfer attempts (secs)", 43200);


    }

    public String init(boolean dbInit, final boolean upgrading, final long currentVersion, final long targetVersion) {
        HTProperties.getProperties().readFile(new File(_fileTransferPropertiesFileKey.apply()), false);

        StringListFromDelimitedKey fileTransferHostsKey =
                new StringListFromDelimitedKey("file.transfer.hosts",
                        "listFiles of hosts with files to transfer",
                        ",",
                        new ArrayList());
        _fileTransferHosts = fileTransferHostsKey.apply();
        return null;
    }


    public String start(boolean dbInit) {
        RestartableService rs = new RestartableService("PollingFileTransferService",
                "PollingFileTransferService",
                100,
                new KeepAliveService(this), true);
        RestartableServiceDaemon.addService(rs);
        return null;
    }

    public String deInit() {
        BaseCommandLine.getCommandLine().reloadJVSProps(true);

        return null;
    }

    public void transferRemoteHost() {
        _aggregatedScpFiles.clear();

        for (String host : _fileTransferHosts) {
            HashMap<String, ScpFileStatus> scpFiles = null;

            if (!StringUtil.nullOrEmptyOrBlankString(host)) {
                /*   set up relevant host names    */
                getHostPropertyKeysValues(host);

                /*   get host-specific session    */
                SshSession sshSession = SshSession.getSshSession(_sourceUsername, _sourcePassword, _sourceHost, _sourcePort, true);


                if (sshSession.getJschSession() != null) {
                    /*   transfer from remote (source) host to local (target) m_scpFiles   */
                    scpFiles = transferRemoteHostFiles(sshSession, scpFiles);

                    /*   delete remote (source) host m_scpFiles   */
                    scpFiles = deleteRemoteHostFiles(sshSession, scpFiles);

                    /*   handle sessions.   */
                    sshSession.disconnect();

                    /*   gzip local (target) host m_scpFiles    */
                    scpFiles = gzipLocalHostFiles(scpFiles);

                    /*    append file results into one master result-set filenames, making keys unique by prepending host.  */
                    for (ScpFileStatus scpFile : scpFiles.values()) {
                        String fullSourceFilePathname = StringUtil.strcat(File.separator,
                                File.separator,
                                scpFile.getSourceHost(),
                                scpFile.getSourceFilePathname());

                        _aggregatedScpFiles.put(fullSourceFilePathname, scpFile);
                    }
                } else {
                    Log.ssh.warn("failure to get a session to host: %s; username: %s; password: %s", _sourceHost, _sourceUsername, _sourcePassword);
                }

            }
        }

        /*   log to file aggregated file transfer results.   */
        logFileTransferResults();

    }


    public HashMap<String, ScpFileStatus> transferRemoteHostFiles(SshSession sshSession, HashMap<String, ScpFileStatus> scpFiles) {
        ScpFrom scpfrom = ScpFrom.getScpFrom(sshSession, _sourceFilePathname, _destinationPath);
        scpFiles = scpfrom.execute();

        return scpFiles;
    }


    public HashMap<String, ScpFileStatus> gzipLocalHostFiles(HashMap<String, ScpFileStatus> scpFiles) {
        for (ScpFileStatus scpFile : scpFiles.values()) {
            String targetFilePathname = scpFile.getTargetFilePathName();

            /*   compress only uncompressed m_scpFiles that have been copied over.   */
            if (!targetFilePathname.toLowerCase().endsWith(".gz")) {
                switch (scpFile.getFileTransferStatus()) {
                    case SshConstants.SSH_FILEXFER_OK:
                    case SshConstants.SSH_FILEDELETE_ERROR:
                    case SshConstants.SSH_FILEDELETE_OK:

                        File inputFile = new File(targetFilePathname);
                        String outputZipFilePathname = StringUtil.strcat(targetFilePathname, ".gz");
                        File outputZipFile = new File(outputZipFilePathname);

                        try {
                            FileUtil.gzipFile(inputFile, outputZipFile, true);

                            if (outputZipFile.exists()) {
                                inputFile.delete();
                            }

                            /*   update scpFile status with the scpFile target filename.   */
                            scpFile.setTargetFilePathName(outputZipFilePathname);
                            scpFiles.put(scpFile.getSourceFilePathname(), scpFile);
                        } catch (IOException e) {
                            Log.ssh.error("error compressing scpFile %s %e %s", targetFilePathname, e, e);
                        }

                        break;

                    default:
                        break;
                }
            }
        }
        return scpFiles;
    }


    public HashMap<String, ScpFileStatus> deleteRemoteHostFiles(SshSession sshSession, HashMap<String, ScpFileStatus> scpFiles) {

        /*   with session in hand, get ssh utilities.   */
        SshCommand sshCmd = SshCommand.getSshCommand(sshSession);

        for (ScpFileStatus scpFile : scpFiles.values()) {
            /*   delete only m_scpFiles that have transferred cleanly from source to target host.   */
            if (scpFile.getFileTransferStatus() == SshConstants.SSH_FILEXFER_OK) {
                int shellStatus = sshCmd.rm(scpFile.getSourceFilePathname());

                switch (shellStatus) {
                    case SshConstants.SSH_SHELLSTATUS_OK:
                        scpFile.setFileTransferStatus(SshConstants.SSH_FILEDELETE_OK);
                        break;

                    case SshConstants.SSH_SHELLSTATUS_ERROR:
                    case SshConstants.SSH_SHELL_STATUS_OUTOFRANGE:
                        scpFile.setFileTransferStatus(SshConstants.SSH_FILEDELETE_ERROR);
                        break;

                    default:
                        scpFile.setFileTransferStatus(SshConstants.SSH_FILEDELETE_ERROR);
                        break;
                }

                /*   stuff the results of the remote file deletion back into file status apply.   */
                scpFiles.put(scpFile.getSourceFilePathname(), scpFile);
            }
        }

        return scpFiles;
    }


    private void getHostPropertyKeysValues(String hostname) {
        Map<String, String> _fileTransferProperties = HTProperties.getProperties().getSubMap(hostname);
        JVS jvs = JVSUtils.convertMapToJVS(_fileTransferProperties, new JVS());
        /*  define, settransfer property keys   */
        StringProperty fileTransferSourceHostKey = new StringProperty("file.transfer.source.host", "production source host", null);
        IntegerProperty fileTransferSourcePortKey = new IntegerProperty("file.transfer.source.port", "production source host port", 22);
        StringProperty fileTransferSourceUsernameKey = new StringProperty("file.transfer.source.username", "production source host user", null);
        StringProperty fileTransferSourcePasswordKey = new StringProperty("file.transfer.source.password", "production source host password", null);
        StringProperty fileTransferDestinationPathKey = new StringProperty("file.transfer.target.path", "production destination host path", null);
        StringListFromDelimitedKey fileTransferSourceFilePathnameKey = new StringListFromDelimitedKey("file.transfer.source.filepathname",
                "production source host path and filename",
                ";",
                new ArrayList<String>());

        /*   get transfer property key values   */
        _sourceHost = fileTransferSourceHostKey.apply(jvs);
        _sourcePort = fileTransferSourcePortKey.apply(jvs);
        _sourceUsername = fileTransferSourceUsernameKey.apply(jvs);
        _sourcePassword = fileTransferSourcePasswordKey.apply(jvs);
        _destinationPath = fileTransferDestinationPathKey.apply(jvs);
        List filePathNameList = fileTransferSourceFilePathnameKey.apply(jvs);
        _sourceFilePathname = StringUtil.listToArray(filePathNameList);
    }


    private void logFileTransferResults() {
        Log.ssh.info("Begin file transfer: source host '%s'", _sourceHost);

        for (ScpFileStatus fileStatus : _aggregatedScpFiles.values()) {
            String sourceFile = fileStatus.getSourceFilePathname();
            String targetFile = fileStatus.getTargetFilePathName();
            String sourceHost = fileStatus.getSourceHost();
            String status = "file transfer not attempted";


            switch (fileStatus.getFileTransferStatus()) {
                case SshConstants.SSH_NOTATTEMPED:
                    status = "file transfer not attempted.";
                    break;

                case SshConstants.SSH_FILEXFER_OK:
                    status = "file transferred cleanly.";
                    break;

                case SshConstants.SSH_FILEXFER_ERROR:
                case SshConstants.SSH_FILEXFER_FATAL:
                    status = "file transfer failure.";
                    break;

                case SshConstants.SSH_FILEXFER_FILENOTFOUND:
                    status = "file not found on source (remote) host.";
                    break;

                case SshConstants.SSH_FILEDELETE_OK:
                    status = "file transferred cleanly and deleted on source (remote) host.";
                    break;

                case SshConstants.SSH_FILEDELETE_ERROR:
                    status = "file transferred cleanly with deletion error on source (remote) host.";
                    break;

                default:
                    break;
            }

            if (targetFile.endsWith(".gz") && !sourceFile.toLowerCase().endsWith(".gz")) {
                status = StringUtil.strcat(status, ". file zipped on target (local) host");
            }

            Log.ssh.info("File transfer: source host '%s'; source file '%s'; target file '%s'; with status: %s",
                    sourceHost,
                    sourceFile,
                    targetFile,
                    status);
        }

        Log.ssh.info("End file transfer: source host '%s'", _sourceHost);
    }


    class KeepAliveService implements Runnable {
        private PollingFileTransferService pollingFileTransferService;

        public KeepAliveService(PollingFileTransferService pollingFileTransferService) {
            this.pollingFileTransferService = pollingFileTransferService;
        }

        public void run() {
            while (true) {
                transferRemoteHost();
                Env.sleepNSeconds(_fileTransferIntervalKey.apply());

            }
        }
    }
}