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
package com.hitorro.base.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.basedms.db.HibernateService;
import com.hitorro.jsontypesystem.propreaders.JVSProperties;
import com.hitorro.util.core.Console;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.json.keys.propaccess.PropaccessError;
import com.hitorro.util.startupframework.RunnableService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved.
 * <p/>
 * User: chris
 */
public class TestJDBCConnection extends RunnableService {
    public String init(boolean dbInit, final boolean upgrading, final long currentVersion, final long targetVersion) {
        return null;
    }

    public String run() {
        try {
            String defaultDBKey = HibernateService.DefaultDBKey.apply();
            JsonNode props = JVSProperties.getProperties().get("dbconfig");
            JsonNode connectInfo = JVSProperties.getProperties().get(defaultDBKey);
            String username = HibernateService.UserName.apply(connectInfo);
            String password = HibernateService.Password.apply(connectInfo);
            if (StringUtil.nullOrEmptyOrBlankString(username) || password == null) {
                // bad configuration
                return "password or username not set";
            }
            String url = HibernateService.DBUrl.apply(connectInfo);

            try {
                Console.println("Getting driver");
                Class.forName("com.mysql.jdbc.Driver"); //Or any other driver
            } catch (Exception x) {
                Console.println("Failed to get driver %s", x.getMessage());
                return "Unable to load the driver class!";
            }
            try {
                Console.println("Connecting user: %s", username);
                Console.println("Connecting password: %s", password);
                Console.println("Connecting url: %s", url);
                Connection dbConnection = DriverManager.getConnection(url, username, password);
                Console.println("Connected");
            } catch (SQLException x) {
                Console.println("Could not connect %s", x.getMessage());
                return "Couldn't get connection! " + x.getMessage();
            }
        } catch (PropaccessError e) {
            return Fmt.S("Error getting parameters for DB connection %s %e", e, e);
        }

        return null;
    }

    public String deInit() {
        return null;
    }

    public String start(boolean dbInit) {
        return null;
    }
}
