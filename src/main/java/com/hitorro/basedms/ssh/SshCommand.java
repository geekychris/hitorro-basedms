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

import com.hitorro.util.core.string.StringUtil;

import java.io.File;


public class SshCommand extends SshExec {

    public SshCommand(SshSession session) {
        super(session);
    }


    public static SshCommand getSshCommand(SshSession session) {
        SshCommand sshCommand = new SshCommand(session);

        return sshCommand;
    }


    public String[] ls(String filePathname) {
        filePathname = getQuotedFilename(filePathname);

        String command = StringUtil.strcat("ls", " ", filePathname);
        return executeArray(command);
    }


    public String[] ls_al(String filePathname) {
        filePathname = getQuotedFilename(filePathname);

        String command = StringUtil.strcat("ls -al", " ", filePathname);
        return executeArray(command);
    }


    public int rm(String filePathname) {
        filePathname = getQuotedFilename(filePathname);

        String command = StringUtil.strcat("rm", " ", filePathname);
        String status = execute(getStatusCommand(command));

        return getExitStatus(status);
    }


    public int mkdir(String filePath) {
        filePath = getQuotedFilename(filePath);

        String command = StringUtil.strcat("mkdir", " ", filePath);
        String status = execute(getStatusCommand(command));

        return getExitStatus(status);
    }


    public int mkdirs(String filePath) {
        int status = SshConstants.SSH_SHELLSTATUS_UNKNOWN;

        filePath = getQuotedFilename(filePath);

        String[] directories = StringUtil.tokenizeFromSingleChar(filePath, File.separator, true);
        StringBuilder targetDirectory = new StringBuilder();

        for (String directory : directories) {
            targetDirectory.append(StringUtil.strcat(File.separator, directory));
            status = mkdir(targetDirectory.toString());
        }
        return status;
    }


}
