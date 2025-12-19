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
import com.hitorro.basedms.cache.ContentTypeCache;
import com.hitorro.basedms.ssh.SshConstants;
import com.hitorro.basedms.ssh.SshSession;
import com.hitorro.util.core.ArrayUtil;
import com.hitorro.util.core.Constants;
import com.hitorro.util.core.ListUtil;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.io.filefilters.FileNameWildcardFilter;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class ScpTo extends Scp {

    private static String _targetPath;
    private static HashMap<String, ScpFileStatus> _sourceFileStatus = new HashMap<String, ScpFileStatus>();
    private static String _sourceHost = "";
    private String _sourceFile;
    private ArrayList<ScpDirectory> _sourcePaths;


    public ScpTo(SshSession session) {
        super(session.getJschSession());
    }


    public static ScpTo getScpTo(SshSession sshSession, String[] sourceFiles, String targetPath) {
        ScpTo scpTo = new ScpTo(sshSession);
        scpTo.setTargetPath(targetPath);
        setSourceHost(getLocalHost());

        //   build up nested directory/files structure suitable for SCpToMessage file transfer. // todo: bad assumption here.  clean-up.
        if (sourceFiles != null && sourceFiles.length == 1) {
            scpTo.setSourceFile(sourceFiles[0]);
        } else {
            scpTo.setSourcePaths(getFileList(sourceFiles));
        }
        return scpTo;
    }


    private static ArrayList<ScpDirectory> getFileList(String[] sourceFiles) {
        //   get the listFiles of files
        ScpDirectory directory = null;

        if (sourceFiles != null) {
            for (String sourceFile : sourceFiles) {
                /*   extract path root and filename from filepathname.   */
                File file = new File(sourceFile);
                File sourceFilePath = file.getParentFile();
                String sourceFileName = file.getName();

                /*  hack.  need *one* directory with some relevant sourceFilePath to in turn parent all file objects    */
                if (directory == null) {
                    directory = new ScpDirectory(sourceFilePath);
                }

                /*   wildcard search or not?   */
                if (sourceFileName.indexOf("*") >= 0) {
                    /*   wildcard search.   get directory's worth of wildcarded filtered Source Files.   */
                    FileNameWildcardFilter filter = new FileNameWildcardFilter(sourceFileName, "*", true);
                    File filteredSourceFiles[] = sourceFilePath.listFiles(filter);

                    /*   one or more 'filtered' Source Files exist.   track them normally.   */
                    if (!ArrayUtil.nullOrEmpty(filteredSourceFiles)) {

                        for (File filteredSourceFile : filteredSourceFiles) {

                            directory.addFile(filteredSourceFile);

                            String filteredSrcFile = filteredSourceFile.getPath();
                            String targetFile = StringUtil.strcat(_targetPath, File.separator, FileUtil.getFileName(filteredSrcFile));
                            ScpFileStatus fileStatus = new ScpFileStatus(filteredSrcFile,
                                    targetFile,
                                    SshConstants.SSH_NOTATTEMPED,
                                    _sourceHost);
                            _sourceFileStatus.put(filteredSrcFile, fileStatus);
                        }
                    } else {
                        /*   no filteredSourceFiles exist.  track the wildcarded file.  do not process the file.   */
                        ScpFileStatus fileStatus = new ScpFileStatus(sourceFileName,
                                null,
                                SshConstants.SSH_FILEXFER_FILENOTFOUND,
                                _sourceHost);
                        _sourceFileStatus.put(sourceFileName, fileStatus);
                    }
                } else {
                    /*  false.  transfer one file.   */
                    if (file.exists()) {

                        /*   specific file exists.   track it normally.   */
                        directory.addFile(file);

                        String srcFile = file.getPath();

                        /*   handle target file.  if trailing filename, do not concatenate source filename onto it.   have a fully qualified name  */
                        String targetFileExtension = FileUtil.getFileExtension(_targetPath);
                        String targetFile = _targetPath;

                        if (StringUtil.nullOrEmptyOrBlankString(targetFileExtension)) {
                            targetFileExtension = Constants.EmptyString;

                            if (ListUtil.nullOrEmpty(ContentTypeCache.getCache().getContentByExtension(targetFileExtension))) {
                                targetFile = StringUtil.strcat(_targetPath, File.separator, FileUtil.getFileName(srcFile));
                            }
                        }


                        ScpFileStatus fileStatus = new ScpFileStatus(srcFile,
                                targetFile,
                                SshConstants.SSH_NOTATTEMPED,
                                _sourceHost);
                        _sourceFileStatus.put(srcFile, fileStatus);
                    } else {
                        /*   specific file not exists, track the file.  do not process the file.   */
                        ScpFileStatus fileStatus = new ScpFileStatus(file.getPath(),
                                null,
                                SshConstants.SSH_FILEXFER_FILENOTFOUND,
                                _sourceHost);

                        _sourceFileStatus.put(file.getPath(), fileStatus);
                    }
                }
            }
        }

        /*   hack. now we have *one* directory parenting all files; convertToPdf that directory to a listFiles of directories.  doh!   */
        ArrayList<ScpDirectory> directories = new ArrayList<ScpDirectory>();
        directories.add(directory);
        return directories;
    }

    private static String getLocalHost() {
        String address = "";

        try {
            address = InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            /*   eat exception.   do not care about unknown hosts.   */
        }

        return address;
    }

    public static void setSourceHost(String sourceHost) {
        _sourceHost = sourceHost;
    }

    /**
     * Carry out the transfer.
     */
    public HashMap<String, ScpFileStatus> execute() {

        try {
            if (_sourcePaths != null) {
                doMultipleTransfer();
            }
            if (_sourceFile != null) {
                doSingleTransfer();
            }
        } catch (Exception e) {
            /*   handle results of the file transfer.  assumption is file transfer failure.   */
            if (_sourceFile != null) {
                ScpFileStatus fileStatus = _sourceFileStatus.get(_sourceFile);
                fileStatus.setFileTransferStatus(SshConstants.SSH_FILEXFER_ERROR);
                _sourceFileStatus.put(_sourceFile, fileStatus);
            }
            Log.ssh.error("ScpTo file transfer error %s %e", e, e);
        }

        return _sourceFileStatus;
    }

    private void doSingleTransfer() {
        String cmd = StringUtil.strcat("scp -t ", _targetPath);
        ScpFileStatus fileStatus = new ScpFileStatus(_sourceFile, _targetPath, SshConstants.SSH_FILEXFER_OK, _sourceHost);

        Channel channel = null;
        try {
            channel = openExecChannel(cmd);


            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();

            waitForAck(in);
            sendFileToRemote(new File(_sourceFile), in, out);
        } catch (JSchException e) {
            fileStatus.setFileTransferStatus(SshConstants.SSH_FILEXFER_ERROR);
            Log.ssh.error("error in SshSession's underlying JschSession %s %e", e, e);
        } catch (IOException e) {
            fileStatus.setFileTransferStatus(SshConstants.SSH_FILEXFER_ERROR);
            Log.ssh.error("stream error in SshSession's underlying JschSession channel %s %e", e, e);
        } finally {
            _sourceFileStatus.put(_sourceFile, fileStatus);
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private void doMultipleTransfer() {
        Channel channel = null;
        ScpFileStatus fileStatus = null;
        try {
            String targetPath = _targetPath;
            if (!StringUtil.nullOrEmptyOrBlankString(FileUtil.getFileExtension(targetPath))) {
                targetPath = FileUtil.getFilePath(targetPath);
            }

            channel = openExecChannel(StringUtil.strcat("scp -r -d -t ", targetPath));
            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();

            waitForAck(in);
            for (ScpDirectory directory : _sourcePaths) {
                fileStatus = new ScpFileStatus();
                fileStatus.setTargetFilePathName(targetPath);
                fileStatus.setSourceFilePathname(directory.getDirectory().getAbsolutePath());
                fileStatus.setFileTransferStatus(SshConstants.SSH_FILEXFER_OK);
                sendDirectory(directory, in, out);
                _sourceFileStatus.put(directory.toString(), fileStatus);
            }
        } catch (JSchException e) {
            fileStatus.setFileTransferStatus(SshConstants.SSH_FILEXFER_ERROR);
            Log.ssh.error("error in SshSession's underlying JschSession %s %e", e, e);

        } catch (IOException e) {
            fileStatus.setFileTransferStatus(SshConstants.SSH_FILEXFER_ERROR);
            Log.ssh.error("stream error in SshSession's underlying JschSession channel %s %e", e, e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private void sendDirectory(ScpDirectory current, InputStream in, OutputStream out) throws IOException, JSchException {
        for (Iterator fileIt = current.filesIterator(); fileIt.hasNext(); ) {
            sendFileToRemote((File) fileIt.next(), in, out);
        }
        for (Iterator dirIt = current.directoryIterator(); dirIt.hasNext(); ) {
            ScpDirectory dir = (ScpDirectory) dirIt.next();
            sendDirectoryToRemote(dir, in, out);
        }
    }

    private void sendDirectoryToRemote(ScpDirectory directory, InputStream in, OutputStream out) throws IOException, JSchException {
        String command = "D0755 0 ";
        command = StringUtil.strcat(command, directory.getDirectory().getName());
        command = StringUtil.strcat(command, "\n");

        out.write(command.getBytes());
        out.flush();

        waitForAck(in);
        sendDirectory(directory, in, out);
        out.write("E\n".getBytes());
        waitForAck(in);
    }

    private void sendFileToRemote(File localFile, InputStream in, OutputStream out) throws IOException, JSchException {
        // send "C0644 filesize filename", where filename should not include '/'
        int filesize = (int) localFile.length();
        String localFilename = localFile.getName();
        String localFilePath = localFile.getPath();
        String command = StringUtil.strcat("C0644 ", filesize, " ");
        command = StringUtil.strcat(command, localFilename);
        command = StringUtil.strcat(command, "\n");

        out.write(command.getBytes());
        out.flush();

        waitForAck(in);

        // send a content of lfile
        FileInputStream fis = new FileInputStream(localFile);
        byte[] buf = new byte[SshConstants.BUFFER_SIZE];
        long startTime = System.currentTimeMillis();
        int totalLength = 0;

        // only track progress for files larger than 100kb in verbose mode
        boolean trackProgress = filesize > 102400;
        // since filesize keeps on decreasing we have to store the initial filesize
        int initFilesize = filesize;
        int percentTransmitted = 0;

        try {
            Log.ssh.debug("Sending file: %s : %s.", localFilename, filesize);
            while (true) {
                int len = fis.read(buf, 0, buf.length);
                if (len <= 0) {
                    break;
                }
                out.write(buf, 0, len);
                totalLength += len;

                if (trackProgress) {
                    percentTransmitted = trackProgress(initFilesize,
                            totalLength,
                            percentTransmitted);
                }
            }
            out.flush();
            sendAck(out);
            waitForAck(in);
        } finally {
            long endTime = System.currentTimeMillis();
            logStats(startTime, endTime, totalLength);
            fis.close();
        }

        //   track results of the file transfer
        ScpFileStatus fileStatus = new ScpFileStatus();
        fileStatus.setFileTransferStatus(SshConstants.SSH_FILEXFER_OK);
        _sourceFileStatus.put(localFilePath, fileStatus);
    }

    /**
     * Get the local file
     *
     * @return the local file
     */
    public String getLocalFile() {
        return _sourceFile;
    }

    /**
     * Get the remote path
     *
     * @return the remote path
     */
    public String getRemotePath() {
        return _targetPath;
    }

    public void setTargetPath(String targetPath) {
        _targetPath = targetPath;
    }

    public void setSourcePaths(ArrayList<ScpDirectory> sourcePaths) {
        _sourcePaths = sourcePaths;
    }


    public void setSourceFile(String sourceFilePath) {
        _sourceFile = sourceFilePath;
    }

}