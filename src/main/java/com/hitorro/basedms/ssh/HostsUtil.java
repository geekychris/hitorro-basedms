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

import com.hitorro.util.core.params.HTProperties;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.json.keys.BooleanProperty;
import com.hitorro.util.json.keys.IntegerProperty;
import com.hitorro.util.json.keys.StringProperty;

/**
 * Utility class for managing all the host related parameters used in such things as secure copy.
 */
public abstract class HostsUtil {
    //   constants
    public static final String HostPath = "host";
    public static final String HostsPath = "hosts";
    public static final String propHost = "host";
    public static final String propPort = "port";
    public static final String propTrust = "trust";
    public static final String propUsername = "username";
    public static final String propPassword = "password";
    public static final String propTargetBasePath = "targetbasepath";
    public static final String propTargetBaseUrl = "targetbaseurl";


    //   individual host keys
    public static final StringProperty HostKey = new StringProperty(HostsUtil.propHost, "Host Name", null);
    public static final IntegerProperty PortKey = new IntegerProperty(HostsUtil.propPort, "Hosts port number", 80);
    public static final BooleanProperty TrustKey = new BooleanProperty(HostsUtil.propTrust, "Trust key", false);

    public static final StringProperty UserKey = new StringProperty(HostsUtil.propUsername, "Host Name", null);
    public static final StringProperty PasswordKey = new StringProperty(HostsUtil.propPassword, "Login password", null);
    public static final StringProperty TargetBasePathKey = new StringProperty(HostsUtil.propTargetBasePath, "Target base path", null);
    public static final StringProperty TargetBaseUrlKey = new StringProperty(HostsUtil.propTargetBaseUrl, "Target base url", null);


    public static String getHostNameByCategory(String category) {
        return HTProperties.getProperties().get(Fmt.S("%s.%s.%s", HostPath, category, "hostname"));
    }
}
