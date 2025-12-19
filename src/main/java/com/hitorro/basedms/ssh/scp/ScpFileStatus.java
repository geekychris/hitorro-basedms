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


import com.hitorro.basedms.ssh.SshConstants;

/**
 * helper class to maintain semi-detailed file transfer status.   File status constants maintained in SshConstants.java
 */
public class ScpFileStatus {
    private String _sourceFilePathname = "";
    private String _targetFilePathname = "";
    private int _fileTransferStatus = SshConstants.SSH_NOTATTEMPED;
    private String _sourceHost = "";


    public ScpFileStatus() {
    }


    public ScpFileStatus(String sourceFilePathname, String targetFilePathname, int fileTransferStatus, String sourceHost) {
        _sourceFilePathname = sourceFilePathname;
        _targetFilePathname = targetFilePathname;
        _fileTransferStatus = fileTransferStatus;
        _sourceHost = sourceHost;
    }

    public String getSourceHost() {
        return _sourceHost;
    }

    public void setRemoteHost(String remoteHost) {
        _sourceHost = remoteHost;
    }

    public String getSourceFilePathname() {
        return _sourceFilePathname;
    }


    public void setSourceFilePathname(String sourceFilePathname) {
        _sourceFilePathname = sourceFilePathname;
    }


    public int getFileTransferStatus() {
        return _fileTransferStatus;
    }


    public void setFileTransferStatus(int fileTransferStatus) {
        _fileTransferStatus = fileTransferStatus;
    }


    public String getTargetFilePathName() {
        return _targetFilePathname;
    }


    public void setTargetFilePathName(String targetFilePathName) {
        _targetFilePathname = targetFilePathName;
    }
}
