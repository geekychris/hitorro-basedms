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
package com.hitorro.db;

import com.hitorro.basedms.transformer.Log;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.startupframework.phases.ServiceDefinition;
import org.apache.derby.jdbc.EmbeddedDriver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Setup a derby database as a service.  This is a lightweight DB that can be used for storing small amounts of data in,
 * that can be manipulated through sql
 */
@ServiceDefinition(dependentService = {},
        shortName = "derbymanager",
        description = "manage a derby database",
        debugCommands = {},
        typeManagedClasses = {},
        uiDirectories = {},
        dependentServiceInterfaces = {})
public class DerbyService {
    static EmbeddedDriver ed;
    private static String protocol = "jdbc:derby:";
    private String driver = "org.apache.derby.jdbc.EmbeddedDriver";

    public static Connection getConnection() throws SQLException {
        Properties props = new Properties(); // connection properties

        props.put("user", "user1");
        props.put("password", "user1");

        //props.put("rollForwardRecoveryFrom", "/hthome/derby/ccrollforward");
        //props.put("logDevice", "/hthome/derby/cclogdevice");
        //rollForwardRecoveryFrom  backup path for rollforward recovery
        //logDevice  log directory path
        String dbName = "derbyDB"; // the name of the database

        String url = Fmt.S("%s%s;create=true;logDevice=/hthome/derbylog", protocol, dbName);
        //DriverPropertyInfo[] info = ed.getPropertyInfo(url, props);
        return DriverManager.getConnection(url, props);
    }

    public String init(boolean dbInit, final boolean upgrading, final long currentVersion, final long targetVersion) {
        return loadDriver();
    }

    public String start(boolean dbInit) {
        return null;
    }

    public String run() {
        return null;
    }

    public String deInit() {
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException se) {
            return Fmt.S("Derby did not shut down normally %s %e", se, se);
        }
        return null;
    }

    private String loadDriver() {

        try {
            ed = (EmbeddedDriver) Class.forName(driver).newInstance();
            Log.db.error("Loaded the appropriate driver");
        } catch (ClassNotFoundException cnfe) {
            return Fmt.S("Unable to load the JDBC driver %s %s %e", driver, cnfe, cnfe);
        } catch (InstantiationException ie) {
            return Fmt.S("Unable to instantiate the JDBC driver %s %s %e", driver, ie, ie);
        } catch (IllegalAccessException iae) {
            return Fmt.S("Not allowed to access the JDBC driver %s %s %e", driver, iae, iae);
        }
        return null;
    }
}
