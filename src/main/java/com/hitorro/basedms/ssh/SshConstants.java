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


public class SshConstants {

    public static final byte LINE_FEED = 0x0a;
    public static final int BUFFER_SIZE = 1024;

    public static final int SSH_NOTATTEMPED = -1;
    public static final int SSH_FILEXFER_OK = 0;
    public static final int SSH_FILEXFER_ERROR = 1;
    public static final int SSH_FILEXFER_FILENOTFOUND = 2;
    public static final int SSH_FILEXFER_DIRECTORYNOTFOUND = 4;
    public static final int SSH_FILEXFER_FATAL = 8;
    public static final int SSH_FILEDELETE_OK = 16;
    public static final int SSH_FILEDELETE_ERROR = 32;


    public static final int SSH_SHELLSTATUS_OK = 0;
    public static final int SSH_SHELLSTATUS_ERROR = 1;
    public static final int SSH_SHELLSTATUS_UNKNOWN = 255;
    public static final int SSH_SHELL_STATUS_OUTOFRANGE = 255;
}
