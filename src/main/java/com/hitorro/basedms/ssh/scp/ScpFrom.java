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
package com.hitorro.basedms.ssh.scp;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;
import com.hitorro.basedms.ssh.SshConstants;
import com.hitorro.basedms.ssh.SshExec;
import com.hitorro.basedms.ssh.SshSession;
import com.hitorro.util.core.ArrayUtil;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.FileUtil;

import java.io.*;
import java.util.HashMap;


public class ScpFrom extends Scp {


    private static HashMap<String, ScpFileStatus> _sourceFiles = new HashMap<String, ScpFileStatus>();
    private static String _targetPath;


    /**
     * Constructor for ScpFrom.
     *
     * @param sshSession the Scp sshSession to use
     */
    public ScpFrom(SshSession sshSession) {
        super(sshSession.getJschSession());

    }


    public static ScpFrom getScpFrom(SshSession sshSession, String[] sourceFiles, String targetPath) {
        ScpFrom scpFrom = new ScpFrom(sshSession);

        setTargetPath(targetPath);

        scpFrom.setSourceFiles(scpFrom.getFileList(sshSession, sourceFiles));

        return scpFrom;
    }

    public static void setTargetPath(String targetPath) {
        _targetPath = targetPath;
    }

    /**
     * Carry out the transfer.
     */
    public HashMap<String, ScpFileStatus> execute() {
        Channel channel = null;

        /*   iterate through the listFiles files   */
        for (String sourceFile : _sourceFiles.keySet()) {
            /**
             *   filter out fully qualified files in our listFiles of files that do not exist on the remote host,
             *   but are itemized in our listFiles and set a status of SshConstants.SSH_FILEXFER_FILENOTFOUND.
             */
            ScpFileStatus sourceFileStatus = _sourceFiles.get(sourceFile);

            if (sourceFileStatus.getFileTransferStatus() == SshConstants.SSH_NOTATTEMPED) {
                String command = "scp -f ";

                command = StringUtil.strcat(command, getQuotedFilename(sourceFile));

                try {
                    /*   get I/O streams for remote scp   */
                    channel = openExecChannel(command);
                    OutputStream out = channel.getOutputStream();
                    InputStream in = channel.getInputStream();
                    channel.connect();
                    sendAck(out);

                    /*   we have a file to transfer, so verify local path exists.  if not, attempt to create it.   soft algorithm.   */
                    File targetFile = new File(StringUtil.strcat(_targetPath, File.separator, new File(sourceFile).getName()));
                    File targetFilePath = targetFile.getParentFile();
                    if (!targetFilePath.exists()) {
                        targetFilePath.mkdirs();
                    }

                    startRemoteCpProtocol(in, out, targetFile);

                    if (channel != null) {
                        channel.disconnect();
                    }

                } catch (Exception e) {

                    /*   handle results of the directory transfer.  assumption in this handler is file transfer failure.   */
                    sourceFileStatus.setFileTransferStatus(SshConstants.SSH_FILEXFER_ERROR);
                    _sourceFiles.put(sourceFile, sourceFileStatus);

                    if (e instanceof JSchException) {
                        Log.ssh.error("error in SshSession's underlying JschSession %s %e", e, e);

                        if (channel != null) {
                            channel.disconnect();
                        }
                    } else if (e instanceof IOException) {
                        Log.ssh.error("stream error in SshSession's underlying JschSession channel %s %e", e, e);
                    } else {
                        Log.ssh.error("ScpTo file transfer error %s %e", e, e);

                    }
                } finally {
                    if (channel != null) {
                        channel.disconnect();
                    }
                }

                sourceFileStatus.setFileTransferStatus(SshConstants.SSH_FILEXFER_OK);
                _sourceFiles.put(sourceFile, sourceFileStatus);
            }
        }
        Log.ssh.debug("SCP completed");

        return _sourceFiles;
    }

    private void startRemoteCpProtocol(InputStream in, OutputStream out, File targetFile) throws IOException, JSchException {
        File startFile = targetFile;
        while (true) {
            // C0644 filesize filename - header for a regular file
            // T time 0 time 0\n - present if perserve time.
            // D directory - this is the header for a directory.
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            while (true) {
                int read = in.read();
                if (read < 0) {
                    return;
                }
                if ((byte) read == SshConstants.LINE_FEED) {
                    break;
                }
                stream.write(read);
            }
            String serverResponse = stream.toString("UTF-8");
            if (serverResponse.charAt(0) == 'C') {
                parseAndFetchFile(serverResponse, startFile, out, in);
            } else if (serverResponse.charAt(0) == 'D') {
                startFile = parseAndCreateDirectory(serverResponse,
                        startFile);
                sendAck(out);
            } else if (serverResponse.charAt(0) == 'E') {
                startFile = startFile.getParentFile();
                sendAck(out);
            } else if (serverResponse.charAt(0) == '\01' || serverResponse.charAt(0) == '\02') {
                throw new IOException(serverResponse.substring(1));
            }
        }
    }

    private File parseAndCreateDirectory(String serverResponse, File targetFile) {
        int start = serverResponse.indexOf(" ");
        // appears that the next token is not used and it's zero.
        start = serverResponse.indexOf(" ", start + 1);
        String directoryName = serverResponse.substring(start + 1);
        if (targetFile.isDirectory()) {
            File dir = new File(targetFile, directoryName);
            dir.mkdir();
            Log.ssh.debug("Creating directory %s", dir);
            return dir;
        }
        return null;
    }

    private void parseAndFetchFile(String serverResponse, File targetFile, OutputStream out, InputStream in)
            throws IOException, JSchException {
        int start = 0;
        int end = serverResponse.indexOf(" ", start + 1);
        start = end + 1;
        end = serverResponse.indexOf(" ", start + 1);
        long filesize = Long.parseLong(serverResponse.substring(start, end));
        String filename = serverResponse.substring(end + 1);
        Log.ssh.debug("Receiving %s of size %s", filename, filesize);
        File transferFile = (targetFile.isDirectory())
                ? new File(targetFile, filename)
                : targetFile;
        fetchFile(transferFile, filesize, out, in);
        waitForAck(in);
        sendAck(out);
    }

    private void fetchFile(File targetFile, long filesize, OutputStream out, InputStream in) throws IOException {
        byte[] buf = new byte[SshConstants.BUFFER_SIZE];
        sendAck(out);

        // read a content of lfile
        FileOutputStream fos = new FileOutputStream(targetFile);
        int length;
        int totalLength = 0;
        long startTime = System.currentTimeMillis();

        // only track progress for files larger than 100kb in verbose mode
        boolean trackProgress = filesize > 102400;
        // since filesize keeps on decreasing we have to store the initial filesize
        long initFilesize = filesize;
        int percentTransmitted = 0;

        try {
            while (true) {
                length = in.read(buf, 0, (buf.length < filesize) ? buf.length : (int) filesize);   //  downcast long to int
                if (length < 0) {
                    throw new EOFException("Unexpected end of stream.");
                }
                fos.write(buf, 0, length);
                filesize -= length;
                totalLength += length;
                if (filesize == 0) {
                    break;
                }

                if (trackProgress) {
                    percentTransmitted = trackProgress(initFilesize,
                            totalLength,
                            percentTransmitted);
                }
            }
        } finally {
            long endTime = System.currentTimeMillis();
            logStats(startTime, endTime, totalLength);
            fos.flush();
            fos.close();
        }
    }

    public HashMap<String, ScpFileStatus> getFileList(SshSession session, String[] sourceFiles) {

        final String command = "ls -AB1d ";
        HashMap<String, ScpFileStatus> remoteFileStatus = new HashMap<String, ScpFileStatus>();

        /*  session already has connection info, userinfo set   */
        SshExec sshExec = SshExec.getSshExec(session);

        /*   iterate through the possibly wildcarded filepathnames; get back all matching files for each filepathname.   */
        for (String sourceFile : sourceFiles) {
            if (!StringUtil.nullOrEmptyOrBlankString(sourceFile)) {
                String remotePath = new File(sourceFile).getParent();

                String fileCommand = StringUtil.strcat(command, getQuotedFilename(sourceFile));
                String[] sourceFilesStream = sshExec.executeArray(fileCommand);

                /**
                 *  unix hard-coded.    particular 'ls' returns listFiles of files separated by '\n'
                 *  the first set of files returned are from root directory and are (nicely) fully qualified.
                 */
                if (!ArrayUtil.nullOrEmpty(sourceFilesStream)) {
                    for (String srcFile : sourceFilesStream) {
                        if (!StringUtil.nullOrEmptyOrBlankString(srcFile)) {
                            /**
                             *  sanity check: verify each file token really contains user-specified root path
                             *  put files with a default transfer status of not attempted...to be updated upon success.
                             */
                            if (srcFile.indexOf(remotePath) >= 0) {
                                String targetFile = StringUtil.strcat(_targetPath,
                                        File.separator,
                                        FileUtil.getFileName(srcFile));
                                ScpFileStatus fileStatus = new ScpFileStatus(srcFile,
                                        targetFile,
                                        SshConstants.SSH_NOTATTEMPED,
                                        session.getJschSession().getHost());

                                remoteFileStatus.put(srcFile, fileStatus);
                            }
                        }
                    }
                } else {
                    /*   track the fact that no files were found, with proper status.   */
                    String targetFile = StringUtil.strcat(_targetPath,
                            File.separator,
                            FileUtil.getFileName(sourceFile));
                    ScpFileStatus fileStatus = new ScpFileStatus(sourceFile,
                            targetFile,
                            SshConstants.SSH_FILEXFER_FILENOTFOUND,
                            session.getJschSession().getHost());
                    remoteFileStatus.put(sourceFile, fileStatus);
                }
            }
        }

        return remoteFileStatus;
    }

    private String getQuotedFilename(String SourceFile) {

        /*   quote spaces in filepathname only.  do not attempt to quote within commands containing filenames.   */
        if (SourceFile.indexOf("\\") < 0) {
            SourceFile = SourceFile.replaceAll("\\s", "\\\\ ");
        }
        return SourceFile;
    }

    public HashMap<String, ScpFileStatus> getSourceFiles() {
        return _sourceFiles;
    }

    public void setSourceFiles(HashMap<String, ScpFileStatus> sourceFiles) {
        _sourceFiles = sourceFiles;
    }

}