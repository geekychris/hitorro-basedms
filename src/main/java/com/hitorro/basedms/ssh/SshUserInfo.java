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

import com.jcraft.jsch.UserInfo;


public class SshUserInfo implements UserInfo {
    // todo: chris: unsecure.  no .rhosts files; no keys.  Trust is currently wide open.  ok inside the lan for now?

    private String password = null;
    private boolean trustAllCertificates = false;
    private boolean firstTime = true;

    public SshUserInfo(String password, boolean trustAllCertificates) {
        this.password = password;
        this.trustAllCertificates = trustAllCertificates;
    }

    public String getPassword() {
        return password;
    }

    public boolean promptYesNo(String str) {
        return trustAllCertificates;
    }

    public String getPassphrase() {
        return null;
    }

    public boolean promptPassphrase(String message) {
        return false;
    }

    public boolean promptPassword(String message) {
        Boolean prompt = firstTime;

        firstTime = false;
        return prompt;
    }

    public void showMessage(String message) {
        System.out.println(message);
    }


}
