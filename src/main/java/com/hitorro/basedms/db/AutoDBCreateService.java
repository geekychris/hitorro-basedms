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
package com.hitorro.basedms.db;


import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.jsontypesystem.propreaders.JVSProperties;
import com.hitorro.util.core.Env;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.json.keys.BooleanProperty;
import com.hitorro.util.json.keys.propaccess.PropaccessError;
import com.hitorro.util.startupframework.ServiceContext;
import com.hitorro.util.startupframework.phases.ServiceDefinition;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

/**
 * if enabled will detect if the user, password
 */
@ServiceDefinition(dependentService = {},
        shortName = "autodbcreate",
        description = "automatically create or recreate a database schema",
        debugCommands = {},
        typeManagedClasses = {},
        uiDirectories = {})
public class AutoDBCreateService {
    public static final String DestroyDB = "destroyDB.txt";
    public static final String InitDB = "initDbDone.txt";
    public static final String AutoCreateKey = "autodbcreate";
    public static BooleanProperty DefaultDBKey = new BooleanProperty(AutoCreateKey, "Test for the db user, create it and run initdb", false);

    private String m_username;
    private String m_password;
    private String m_url;

    private String m_rootUsername = "root";
    private String m_rootPassword = "";
    private String m_rootUrl;
    private String m_host;
    private String m_databaseName;
    private Properties m_props;
    private JVS HiTorroProps;

    /**
     * If enables, we should check to see if we have:
     * <p/>
     * initdb.auto if so, do nothing as we are complete if not, determine if we have the db user. If so,
     *
     * @param dbInit
     * @param upgrading
     * @param currentVersion
     * @param targetVersion  @return
     */
    public String init(boolean dbInit, final boolean upgrading, final long currentVersion, final long targetVersion) {
        try {
            Log.hibernate.info("Considering AutoDBCreate");

            if (DefaultDBKey.apply()) {
                Log.hibernate.info("AutoDBCreate set");

                getDBParams("dbconfig", DatabaseUtil.DefaultDBKey.apply());
                // we are marked for db user detection etc.
                File autoInitDir = new File(Env.getHome(), Fmt.S("%s-%s", m_host, m_databaseName));

                File initdbDone = new File(autoInitDir, InitDB);
                File destroyDB = new File(Env.getHome(), DestroyDB);

                if (destroyDB.exists()) {
                    Log.hibernate.info("AutoDBCreate drop db marker found");

                    if (!destroyDB.delete()) {
                        Log.util.fatal("destroyDB.txt file exists but cannot remove, ignorning request to drop db");
                        return "destroyDB.txt file exists for this instance but this file cannot be removed.";
                    }

                    Log.hibernate.info("Dropping db");
                    initdbDone.delete();
                    DatabaseUtil.dropDatabase(HiTorroProps);

                }
                if (!autoInitDir.exists()) {
                    Log.hibernate.info("AutoDBCreate set but initdb marker in place.");
                    autoInitDir.mkdir();
                }


                if (initdbDone.exists()) {
                    // we are done, initdb is done.
                    return "";
                }

                // should check to see if the db exists
                boolean canConnectAsUser = DatabaseUtil.canUserConnect(m_username, m_password, m_url);
                if (canConnectAsUser) {
                    Log.hibernate.info("User already exists and can connect to db, just doing initdb without db create");

                    return initDb(initdbDone);
                }
                boolean canConnectAsRoot = DatabaseUtil.canUserConnect(this.m_rootUsername, this.m_rootPassword, this.m_rootUrl);

                if (canConnectAsRoot) {
                    Log.hibernate.debug("AutoDBCreate can connect as root user");

                    if (DatabaseUtil.createDatabase(HiTorroProps)) {
                        Log.hibernate.info("AutoDBCreate created database %s", m_databaseName);

                        if (DatabaseUtil.createUser(HiTorroProps)) {
                            Log.hibernate.info("AutoDBCreate created user %s", m_username);

                            return initDb(initdbDone);
                        }
                    }
                } else {
                    return "Unable to connect as root user on mysql";
                }
            }
        } catch (IOException ioe) {
            return ioe.getMessage();
        } catch (SQLException sqle) {
            return sqle.getMessage();
        } catch (PropaccessError e) {
            return e.getMessage();
        }
        return "";
    }


    private String initDb(File initdbDone) throws IOException {
        FileUtil.writeLongValToFile(initdbDone, System.currentTimeMillis());
        this.setCreateMode();
        return "";
    }


    private final boolean getDBParams(String databaseConfigKey,
                                      String connectConfigKey) throws PropaccessError {
        //  hibernate-related database configuration properties:  dbconfig
        m_props = new JVS(JVSProperties.getProperties().get(databaseConfigKey)).getAsProperties();

        //   default database configuration properties: defaultdb 
        HiTorroProps = DatabaseUtil.getDefaultDBContext();
        JsonNode connectInfo = HiTorroProps.get(connectConfigKey);

        m_username = DatabaseUtil.UsernameKey.apply(connectInfo);
        m_password = DatabaseUtil.PasswordKey.apply(connectInfo);
        m_url = DatabaseUtil.DatabaseUrlKey.apply(connectInfo);
        m_host = DatabaseUtil.getHostFromUrl(m_url);
        m_databaseName = DatabaseUtil.getDatabaseFromUrl(m_url);

        m_rootUsername = DatabaseUtil.RootUsernameKey.apply(connectInfo);
        m_rootPassword = DatabaseUtil.RootPasswordKey.apply(connectInfo);
        m_rootUrl = DatabaseUtil.getRootUrl(m_url);

        return !StringUtil.nullOrEmptyString(m_host) &&
                !StringUtil.nullOrEmptyString(m_databaseName);
    }


    /**
     * Tell hibernate to create the db and also switch the Services compContext into initdb=true mode.
     */
    private void setCreateMode() {
        HibernateUtil.setCreateMode(m_props, "create");
        ServiceContext.getSC().setInitDb(true);
    }

    public String start(boolean dbInit) {
        return null;
    }


    public String run() {
        return null;
    }

    public String deInit() {
        return null;
    }
}