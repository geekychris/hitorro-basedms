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
import com.hitorro.basedms.db.executor.Executor;
import com.hitorro.basedms.db.executor.Result;
import com.hitorro.basedms.db.executor.Script;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.jsontypesystem.propreaders.JVSProperties;
import com.hitorro.util.core.ListUtil;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.params.HTProperties;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.json.keys.StringProperty;
import com.hitorro.util.json.keys.propaccess.PropaccessError;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.TreeMap;


public class DatabaseUtil {
    public static final String RootDatabaseName = "mysql";

    public static final String DefaultDB = "defaultdb";

    /*   key key  */
    public static StringProperty DefaultDBKey = new StringProperty("defaultdbkey", "Default DB connect info", DefaultDB);

    /*  database connection keys  */
    public static StringProperty UsernameKey = new StringProperty("username", "database user name", null);
    public static StringProperty PasswordKey = new StringProperty("password", "database password", null);
    public static StringProperty DatabaseUrlKey = new StringProperty("url", "database url", null);
    public static StringProperty HostNameKey = new StringProperty("hostname", "host database resides on", "localhost");
    public static StringProperty DatabaseNameKey = new StringProperty("databasename", "database name", null);

    public static StringProperty RootUsernameKey = new StringProperty("rootusername", "database user name", null);
    public static StringProperty RootPasswordKey = new StringProperty("rootpassword", "database password name", null);
    public static StringProperty RootDatabaseUrlKey = new StringProperty("rooturl", "system database url", null);


    /**
     * Create a user given default HTProperties in conjunction with username, password.
     *
     * @param dbProperties - default HTProperties TreeMap.
     * @param username     - name of user to create.   Override value found in dbProperties.
     * @param password     - newly created user's associated password.  Override value found in dbProperties.
     * @return true if user creation suceeded.  false if user creation failed.
     * @throws IOException
     * @throws SQLException
     */
    public static boolean createUser(JVS dbProperties,
                                           String username,
                                           String password) throws IOException, SQLException, PropaccessError {
        JsonNode connectInfo = getConnectionInfoMap(dbProperties);

        String databaseName = DatabaseUtil.DatabaseNameKey.apply(connectInfo);
        String rootUrl = RootDatabaseUrlKey.apply(connectInfo);
        String rootUsername = RootUsernameKey.apply(connectInfo);
        String rootPassword = RootPasswordKey.apply(connectInfo);


        dbProperties.set(getFullyQualifiedKey(UsernameKey), username);
        dbProperties.set(getFullyQualifiedKey(PasswordKey), password);
        return createUser(rootUsername, rootPassword, rootUrl, username, password, databaseName);

    }


    /**
     * Create a user given default HTProperties.   creates user based on username, password encoded in default
     * HTProperties.
     *
     * @param dbProperties - default HTProperties TreeMap.
     * @return true if user creation suceeded.  false if user creation failed.
     * @throws IOException
     * @throws SQLException
     * @see DatabaseUtil#getDefaultDBContext()
     */
    public static boolean createUser(JVS dbProperties) throws IOException, SQLException, PropaccessError {
        JsonNode connectInfo = getConnectionInfoMap(dbProperties);

        String databaseName = DatabaseUtil.DatabaseNameKey.apply(connectInfo);
        String username = DatabaseUtil.UsernameKey.apply(connectInfo);
        String password = DatabaseUtil.PasswordKey.apply(connectInfo);

        String rootUrl = RootDatabaseUrlKey.apply(connectInfo);
        String rootUsername = RootUsernameKey.apply(connectInfo);
        String rootPassword = RootPasswordKey.apply(connectInfo);

        return createUser(rootUsername, rootPassword, rootUrl, username, password, databaseName);
    }


    /**
     * Create a user strictly via parameters.   pass in root username, password, url access with which to complete the
     * creation of the desired user.
     *
     * @param rootUsername - root user to create the desired user.
     * @param rootPassword - root user password to make the root user connection.
     * @param rootUrl      - url to access rdbms system database & catalogs.
     * @param username     - name of user to create.
     * @param password     - newly created user's associated password.
     * @param databaseName - name of target database to grant user privileges in.
     * @return
     * @throws IOException
     * @throws SQLException
     */
    public static boolean createUser(String rootUsername,
                                           String rootPassword,
                                           String rootUrl,
                                           String username,
                                           String password,
                                           String databaseName) throws IOException, SQLException {
        Log.util.debug("dbms util creating user %s in database %s", username, databaseName);

        HTProperties properties = new HTProperties();
        properties.setProperty(Script.PROPERTY_USER_NAME, username);
        properties.setProperty(Script.PROPERTY_USER_PASSWORD, password);
        properties.setProperty(Script.PROPERTY_DATABASE_NAME, databaseName);
        properties.setProperty(Script.PROPERTY_HOST_NAME, getHostFromUrl(rootUrl));
        Executor executor = createExecutor(rootUsername, rootPassword, rootUrl);
        Script script = new Script(Script.SCRIPT_CREATE_USER_GRANT_PRIVS);

        executor.execute(script, properties);
        List<Result> results = executor.getResults();
        return !ListUtil.nullOrEmpty(results);

    }


    /**
     * Can the root user connect to the rdbms system url.
     *
     * @param dbProperties - default HTProperties TreeMap.
     * @return true if the root user can connect.  false if the root user cannot connect.
     * @see DatabaseUtil#getDefaultDBContext()
     */
    public static boolean canRootUserConnect(JVS dbProperties) throws PropaccessError {
        JsonNode connectInfo = getConnectionInfoMap(dbProperties);
        String rootUrl = RootDatabaseUrlKey.apply(connectInfo);
        String rootUsername = RootUsernameKey.apply(connectInfo);
        String rootPassword = RootPasswordKey.apply(connectInfo);

        return canUserConnect(rootUsername, rootPassword, rootUrl);

    }


    /**
     * Can an ordinary user connect to the specific database found in the default HTProperties.
     *
     * @param dbProperties - default HTProperties TreeMap.
     * @return true if the root user can connect.  false if the root user cannot connect.
     * @see DatabaseUtil#getDefaultDBContext()
     */
    public static boolean canUserConnect(JVS dbProperties) throws PropaccessError {
        JsonNode connectInfo = getConnectionInfoMap(dbProperties);
        String url = DatabaseUrlKey.apply(connectInfo);
        String username = UsernameKey.apply(connectInfo);
        String password = PasswordKey.apply(connectInfo);

        return canUserConnect(username, password, url);

    }


    /**
     * Can an ordinary user connect to a database specified strictly in parameters.
     *
     * @param username - name of user to attempt to connect as.
     * @param password - user's associated password.
     * @param url      - target database url to connect to.
     * @return true if the root user can connect.  false if the root user cannot connect.
     * @throws IOException
     * @throws SQLException
     * @see DatabaseUtil#getDefaultDBContext()
     */
    public static boolean canUserConnect(String username, String password, String url) {
        try {
            Log.util.debug("dbms util attempting connection: username: %s; password: %s; url: %s.", username, password, url);
            Connection connection = Executor.newConnection(username, password, url);
            connection.close();
        } catch (SQLException e) {
            return false;
        }

        return true;
    }


    /**
     * Permanently drop specified database.
     *
     * @param dbProperties - default HTProperties TreeMap.
     * @param databaseName - database to destroy.  Override value found in dbProperties.
     * @return True if database was sucessfully dropped.  false if there was a failure in dropping the database.
     * @throws IOException
     * @throws SQLException
     * @see DatabaseUtil#getDefaultDBContext()
     */
    public static boolean dropDatabase(JVS dbProperties, String databaseName) throws IOException, SQLException, PropaccessError {
        JsonNode connectInfo = getConnectionInfoMap(dbProperties);
        String rootUsername = RootUsernameKey.apply(connectInfo);
        String rootPassword = RootPasswordKey.apply(connectInfo);
        String rootDatabaseUrl = RootDatabaseUrlKey.apply(connectInfo);

        dbProperties.set(getFullyQualifiedKey(DatabaseNameKey), databaseName);


        return dropDatabase(rootUsername, rootPassword, rootDatabaseUrl, databaseName);
    }


    /**
     * Permanently drop specified database in HTProperties treemap.
     *
     * @param dbProperties - default HTProperties TreeMap.
     * @return True if database was sucessfully dropped.  false if there was a failure in dropping the database.
     * @throws IOException
     * @throws SQLException
     * @see DatabaseUtil#getDefaultDBContext()
     */
    public static boolean dropDatabase(JVS dbProperties) throws IOException, SQLException, PropaccessError {
        JsonNode connectInfo = getConnectionInfoMap(dbProperties);
        String rootUsername = RootUsernameKey.apply(connectInfo);
        String rootPassword = RootPasswordKey.apply(connectInfo);
        String rootDatabaseUrl = RootDatabaseUrlKey.apply(connectInfo);
        String databaseName = DatabaseNameKey.apply(connectInfo);


        return dropDatabase(rootUsername, rootPassword, rootDatabaseUrl, databaseName);

    }


    /**
     * Permanently drop database strictly specified by parameters.
     *
     * @param rootUsername - root user to drop the specified database.
     * @param rootPassword - root user password to make the root user connection.
     * @param rootUrl      - url to access rdbms system database & catalogs.
     * @param databaseName - database to destroy.
     * @return
     * @throws IOException
     * @throws SQLException
     */
    public static boolean dropDatabase(String rootUsername,
                                             String rootPassword,
                                             String rootUrl,
                                             String databaseName) throws IOException, SQLException {
        Log.util.info("dbms util dropping database %s", databaseName);
        HTProperties properties = new HTProperties();
        properties.setProperty(Script.PROPERTY_DATABASE_NAME, databaseName);
        Executor executor = createExecutor(rootUsername, rootPassword, rootUrl);
        Script script = new Script(Script.SCRIPT_DROP_DATABASE);

        executor.execute(script, properties);
        List<Result> results = executor.getResults();
        return !ListUtil.nullOrEmpty(results);
    }


    /**
     * Create a database via HTProperties default TreeMap.
     *
     * @param dbProperties - default HTProperties TreeMap.
     * @param databaseName - database to create.  Override value found in dbProperties.
     * @return true if database was sucessfully created.  false if the database failed to be created.
     * @throws IOException
     * @throws SQLException
     * @see DatabaseUtil#getDefaultDBContext()
     */
    public static boolean createDatabase(JVS dbProperties, String databaseName) throws IOException, SQLException, PropaccessError {
        JsonNode connectInfo = getConnectionInfoMap(dbProperties);
        String rootUsername = RootUsernameKey.apply(connectInfo);
        String rootPassword = RootPasswordKey.apply(connectInfo);
        String rootDatabaseUrl = RootDatabaseUrlKey.apply(connectInfo);

        dbProperties.set(getFullyQualifiedKey(DatabaseNameKey), databaseName);


        return createDatabase(rootUsername, rootPassword, rootDatabaseUrl, databaseName);
    }


    /**
     * Create a database via HTProperties default TreeMap.
     *
     * @param dbProperties - default HTProperties TreeMap.
     * @return true if database was sucessfully created.  false if the database failed to be created.
     * @throws IOException
     * @throws SQLException
     * @see DatabaseUtil#getDefaultDBContext()
     */
    public static boolean createDatabase(JVS dbProperties) throws IOException, SQLException, PropaccessError {
        JsonNode connectInfo = getConnectionInfoMap(dbProperties);
        String rootUsername = RootUsernameKey.apply(connectInfo);
        String rootPassword = RootPasswordKey.apply(connectInfo);
        String rootDatabaseUrl = RootDatabaseUrlKey.apply(connectInfo);
        String databaseName = DatabaseNameKey.apply(connectInfo);


        return createDatabase(rootUsername, rootPassword, rootDatabaseUrl, databaseName);

    }


    /**
     * Create a database strictly specified via parameters.
     *
     * @param rootUsername - root user to create the specified database.
     * @param rootPassword - root user password to make the root user connection.
     * @param rootUrl      - url to access rdbms system database & catalogs.
     * @param databaseName - name of database to create.
     * @return true if database was sucessfully created.  false if the database failed to be created.
     * @throws IOException
     * @throws SQLException
     */
    public static boolean createDatabase(String rootUsername,
                                               String rootPassword,
                                               String rootUrl,
                                               String databaseName) throws IOException, SQLException {
        try {
            Log.util.info("dbms util creating database %s", databaseName);
            HTProperties properties = new HTProperties();
            properties.setProperty(Script.PROPERTY_DATABASE_NAME, databaseName);

            Executor executor = createExecutor(rootUsername, rootPassword, rootUrl);
            Script script = new Script(Script.SCRIPT_CREATE_DATABASE);

            executor.execute(script, properties);
            List<Result> results = executor.getResults();
            if (ListUtil.nullOrEmpty(results)) {
                return false;
            }
        } catch (SQLException e) {
            String message = e.getMessage();
            if (message.indexOf("database exists") == -1) {
                throw e;
            }
        }

        return true;

    }


    /**
     * wrap Executor constructor with a getter.
     *
     * @return new instance of Executor.
     */
    public static Executor createExecutor(String username, String password, String url) {
        return new Executor(username, password, url);
    }


    /**
     * Given a jdbc rdbms url, extract the hostname.
     *
     * @param url
     * @return hostname
     */
    public static final String getHostFromUrl(String url) {
        int beginIndex = url.indexOf(Executor.MYSQL_URL) + Executor.MYSQL_URL.length();

        if (beginIndex == -1) {
            return "";
        }

        int endIndex = url.indexOf("/", beginIndex);

        if (endIndex == -1) {
            return "";
        }

        return url.substring(beginIndex, endIndex);

    }


    /**
     * Given a jdbc rdbms url, extract the database name.
     *
     * @param url
     * @return database name
     */
    public static final String getDatabaseFromUrl(String url) {
        /*   get past the '//' found in the url.   */
        int beginIndex = url.indexOf(Executor.MYSQL_URL) + Executor.MYSQL_URL.length();

        if (beginIndex == -1) {
            return "";
        }

        /*  get past the next '/' found in the url.  */
        beginIndex = url.indexOf("/", beginIndex) + 1;

        if (beginIndex == -1) {
            return "";
        }

        /*   want the text between '/' and '?'   */
        int endIndex = url.indexOf("?", beginIndex);

        if (endIndex == -1) {
            return "";
        }


        return url.substring(beginIndex, endIndex);

    }


    /**
     * Given a jdbc rdbms url for any database in an instance, construct a url to access the system database / catalogs
     * as a root user.
     *
     * @param url to any database in an instance.
     * @return url for root user to obtain direct access to the system database / catalogs.
     */
    public static final String getRootUrl(String url) {
        String rootUrl = "";
        String host = getHostFromUrl(url);

        if (!StringUtil.nullOrEmptyString(host)) {
            rootUrl = getDatabaseUrl(host, "mysql");
        }

        return rootUrl;

    }


    /**
     * Given a hostname and a database name, construct a jdbc rdbms url to uniquely access that rdbms instance.
     *
     * @param hostname - host rdbms instance is running on.
     * @param database - database within the specified host rdbms instance.
     * @return url to connect to the database.
     */
    public static final String getDatabaseUrl(String hostname, String database) {
        return Executor.generateURL(hostname, database);
    }


    /**
     * Construct a HTProperties TreeMap containing just the 'defaultdb' database-related properties, including root
     * username & password, non-priviledged username & password, database connection url, hostname, database name root
     * database connection url.
     *
     * @return HTProperties TreeMap narrowed to just database-related properties.
     */
    public static JVS getDefaultDBContext() throws PropaccessError {

        JsonNode connectInfo = getConnectionInfoMap(JVSProperties.getProperties());
        String databaseUrl = DatabaseUtil.DatabaseUrlKey.apply(connectInfo);
        String databaseName = DatabaseUtil.getDatabaseFromUrl(databaseUrl);
        String hostname = DatabaseUtil.getHostFromUrl(databaseUrl);
        String username = DatabaseUtil.UsernameKey.apply(connectInfo);
        String password = DatabaseUtil.PasswordKey.apply(connectInfo);

        String rootUrl = DatabaseUtil.getDatabaseUrl(hostname, RootDatabaseName);
        String rootUsername = RootUsernameKey.apply(connectInfo);
        String rootPassword = RootPasswordKey.apply(connectInfo);

        if (StringUtil.nullOrEmptyString(rootUsername)) {
            rootUsername = "root";
            rootPassword = "";
        }

        return getDBContext(username, password, hostname, databaseName, rootUsername, rootPassword);

    }


    /**
     * Construct a HTProperties TreeMap containing just the 'defaultdb' database-related properties, including root
     * username & password, non-priviledged username & password, database connection url, hostname, database name root
     * database connection url.
     *
     * @param databaseName - database within the specified host rdbms instance.
     * @return HTProperties TreeMap narrowed to just database-related properties.
     */
    public static JVS getDefaultDBContext(String databaseName) throws PropaccessError {
        JVS properties = DatabaseUtil.getDefaultDBContext();
        properties.set(getFullyQualifiedKey(DatabaseNameKey), databaseName);

        return properties;


    }


    /**
     * Construct a HTProperties TreeMap containing just the 'defaultdb' database-related properties by passing in all
     * the properties strictly as parameters.
     *
     * @param username     - non-priviledged user that can connect to the specified database.
     * @param password     - password associated with the non-priviledged user.
     * @param hostname     - host the rdbms instance resides on.
     * @param databaseName - database within the rdbms instance.
     * @param rootUsername - root user to create the specified database.
     * @param rootPassword - root user password to make the root user connection.
     * @return HTProperties TreeMap narrowed to just database-related properties.
     */
    public static JVS getDBContext(String username,
                                         String password,
                                         String hostname,
                                         String databaseName,
                                         String rootUsername,
                                         String rootPassword) throws PropaccessError {
        String databaseUrl = getDatabaseUrl(hostname, databaseName);
        String rootUrl = getDatabaseUrl(hostname, RootDatabaseName);

        JVS properties = new JVS();
        properties.set(getFullyQualifiedKey(DatabaseUrlKey), databaseUrl);
        properties.set(getFullyQualifiedKey(HostNameKey), hostname);
        properties.set(getFullyQualifiedKey(UsernameKey), username);
        properties.set(getFullyQualifiedKey(PasswordKey), password);
        properties.set(getFullyQualifiedKey(DatabaseNameKey), databaseName);
        properties.set(getFullyQualifiedKey(RootDatabaseUrlKey), rootUrl);
        properties.set(getFullyQualifiedKey(RootUsernameKey), rootUsername);
        properties.set(getFullyQualifiedKey(RootPasswordKey), rootPassword);
        return properties;
    }


    private static String getFullyQualifiedKey(StringProperty keyToQualify) {
        return (Fmt.S("%s.%s", DefaultDBKey.apply(), keyToQualify.toString()));
    }


    /**
     * Given a HTProperties TreeMap, extract sub-TreeMap of just the database connection properties with keys trimmed to
     * just the unique key name.
     *
     * @param dbProperties
     * @return TreeMap of database connection properties.
     */
    public static JsonNode getConnectionInfoMap(JVS dbProperties) throws PropaccessError {
        return dbProperties.get(DatabaseUtil.DefaultDBKey.apply());
    }


    /**
     * Given a HTProperties TreeMap, extract sub-TreeMap of just the database connection properties with keys trimmed to
     * just the unique key name.
     *
     * @param dbProperties     - default HTProperties TreeMap.
     * @param connectConfigKey - String with which to trim down the database connection properties keys.
     * @return TreeMap of database connection properties.
     */
    public static TreeMap<String, String> getConnectionInfoMap(HTProperties dbProperties, String connectConfigKey) {
        return dbProperties.getSubMap(connectConfigKey);
    }

}
