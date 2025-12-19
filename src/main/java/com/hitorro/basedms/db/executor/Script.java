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
package com.hitorro.basedms.db.executor;

import com.hitorro.util.core.Env;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: Chris Date: Nov 14, 2006 Time: 6:30:17 PM
 * <p/>
 * Parse a SQL script into a List of Statements
 */
public class Script {

    public static final String DELIMITER = ";;;";       //  SQL Script statement delimiter

    //  Properties that can referenced and resolved in Scripts
    public static final String PROPERTY_HOST_NAME = "host.name";            //  Connection: Host
    public static final String PROPERTY_USER_NAME = "user.name";            //  Connection: User Name
    public static final String PROPERTY_USER_PASSWORD = "user.password";    //  Connection: User PasswordKey

    public static final String PROPERTY_TABLE_NAME = "table.name";          //  Script variable for table name
    public static final String PROPERTY_DATABASE_NAME = "database.name";    //  Script variable for database name

    public static final String PROPERTY_USER_PRIVILEGES = "user.privileges";    //  User Privileges String used by GRANT Script
    public static final String PROPERTY_USER_PRIVILEGES_DATABASE = "user.privileges.database";    //  Database Objects String used by GRANT Script

    public static final String PROPERTY_FOREIGN_KEY_CHECK = "foreign_key.check";    //  String True/False for enabling/disabling foreign key checking

    //  Constant Property Values
    public static final String MYSQL_ALL_PRIVILEGES = "ALL PRIVILEGES";     //  All Privileges to Grant to the User
    public static final String MYSQL_ALL_DB_OBJECTS = "*.*";                //  The Database Objects to which these Privileges apply

    //  Fundamental Database Scripts   
    public static final String SCRIPT_CREATE_DATABASE = "user/scripts/db/create_database.sql";
    public static final String SCRIPT_CREATE_USER = "user/scripts/db/create_user.sql";
    public static final String SCRIPT_DROP_DATABASE = "user/scripts/db/drop_database.sql";
    public static final String SCRIPT_DROP_TABLE = "user/scripts/db/drop_table.sql";
    public static final String SCRIPT_ENABLE_FOREIGN_KEY_CHECK = "user/scripts/db/enable_foreign_key_check.sql";
    public static final String SCRIPT_GRANT_PRIVILEGES = "user/scripts/db/grant_privileges.sql";
    public static final String SCRIPT_CREATE_USER_GRANT_PRIVS = "user/scripts/db/create_and_grant.sql";

    private List<String> _statements = new ArrayList<String>();

    public Script() {
    }

    public Script(String fileName) throws IOException {
        File file = new File(Env.getBin(), fileName);
        loadScripts(file);
    }

    public Script(File file) throws IOException {
        loadScripts(file);
    }

    /**
     * @return List of String sql statements
     */
    public List<String> getStatements() {
        return _statements;
    }

    /**
     * Load SQL Script into List of Statements
     *
     * @param file File containing SQL Script
     * @throws IOException
     */
    private void loadScripts(File file) throws IOException {
        //  Prepare File Reader
        BufferedReader r = new BufferedReader(new FileReader(file));

        //  Read file line-by-line, using DELIMITER to extract List of Statements
        String line = null;
        StringBuilder statementBuilder = new StringBuilder();
        while ((line = r.readLine()) != null) {
            addStatement(statementBuilder, line);
        }

        //  Add last undelimited string
        addStatement(statementBuilder.toString());

    }

    /**
     * @param statement Statement to be added (potentially contains variables to be resolved)
     */
    public void addStatement(String statement) {
        StringBuilder statementBuilder = new StringBuilder();
        addStatement(statementBuilder, statement);

        String finalStatement = statementBuilder.toString().trim();
        if (finalStatement.length() > 0) {
            _statements.add(finalStatement);
        }
    }

    /**
     * @param statementBuilder StringBuilder used to append strings
     * @param statement        String Statement to parse
     */
    private void addStatement(StringBuilder statementBuilder, String statement) {
        int indexDelimiter = statement.indexOf(DELIMITER);
        while (indexDelimiter >= 0) {
            statementBuilder.append(" ").append(statement, 0, indexDelimiter);

            _statements.add(statementBuilder.toString());

            statementBuilder.setLength(0);
            statement = statement.substring(indexDelimiter + DELIMITER.length());
            indexDelimiter = statement.indexOf(DELIMITER);
        }

        statementBuilder.append(" ").append(statement);
    }

}
