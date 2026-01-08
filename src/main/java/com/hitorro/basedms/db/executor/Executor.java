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

import com.hitorro.util.core.Log;
import com.hitorro.util.core.params.HTProperties;
import com.hitorro.util.core.string.Fmt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <p/>
 * Execute SQL Scripts containing one or more statements
 */
public class Executor {

    public static final String MYSQL_URL = "jdbc:mysql://";
    public static final String AUTO_RECONNECT = "?autoReconnect=true";
    public static final String CHARACTER_ENCODING = "&characterEncoding=UTF-8";

    //  Load & Register MySQL Database Driver
    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String _user = null;
    private String _password = null;
    private String _url = null;
    private List<Result> _results = null;


    public Executor(String user, String password, String host, String database) {
        _user = user;
        _password = password;
        _url = generateURL(host, database);
    }


    public Executor(String user, String password, String url) {
        _user = user;
        _password = password;
        _url = url;
    }


    public Executor(ConnectionProperties properties) {
        _user = properties.getUsername();
        _password = properties.getPassword();
        _url = properties.getUrl();
    }


    /**
     * @param host
     * @param database
     * @return Database Connection URL
     */
    public static String generateURL(String host, String database) {
        return Fmt.S("%s%s/%s%s%s", MYSQL_URL, host, database, AUTO_RECONNECT, CHARACTER_ENCODING);
    }


    /**
     * @param user     - database user name
     * @param password - database user's password
     * @param host     - address of server hosting database
     * @param database - name of database with which to establish a connection
     * @return New Database Connection
     * @throws SQLException
     */
    public static Connection newConnection(String user, String password, String host, String database) throws SQLException {
        String url = generateURL(host, database);
        return newConnection(user, password, url);
    }


    public static Connection newConnection(String user, String password, String url) throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }


    public static Connection newConnection(ConnectionProperties properties) throws SQLException {
        return DriverManager.getConnection(properties.getUrl(), properties.getUsername(), properties.getPassword());
    }


    /**
     * Execute single string statement
     *
     * @param statement
     * @throws SQLException
     */
    public void execute(String statement) throws SQLException {
        Script script = new Script();
        script.addStatement(statement);
        execute(script, null);
    }


    /**
     * Execute all statements in the specified SQL Script
     *
     * @param script SQL Script containing one or more sql statements to be executed
     * @throws SQLException
     */
    public void execute(Script script) throws SQLException {
        execute(script, null);
    }


    public void execute(Script script, HTProperties properties) throws SQLException {

        Connection connection = null;
        Statement stmt = null;

        _results = new ArrayList<Result>();

        try {
            connection = newConnection(_user, _password, _url);
            stmt = connection.createStatement();
            List<String> sqlStatements = script.getStatements();

            for (String sqlStatement : sqlStatements) {

                //  If properties are specified, resolve any variables in the script
                if (properties != null) {
                    sqlStatement = properties.resolveVariable(sqlStatement, true).trim();
                }
                if (sqlStatement.startsWith("#")) {
                    continue;
                }

                stmt.execute(sqlStatement);

                Result result = new Result(sqlStatement);

                //  To iterate over multiple statement results:
                //  => check initial state of statement (update count && resultset)
                //  => if update count == -1 AND resultset is null

                boolean moreResults = true;
                while (moreResults) {
                    int updateCount = stmt.getUpdateCount();
                    ResultSet rs = stmt.getResultSet();
                    if (updateCount >= 0) {
                        //  Output Rows Affected
                        ResultTable table = new ResultTable();
                        table.addColumn(Result.COLUMN_ROWS_AFFECTED);
                        table.addRow(Integer.toString(updateCount));
                        result.addResultTable(table);
                    } else {
                        if (rs != null) {

                            ResultTable table = new ResultTable();

                            //  Output Columns of Data
                            ResultSetMetaData md = rs.getMetaData();
                            if (md != null) {

                                //  Read Columns from metadata & Output
                                int columns = md.getColumnCount();
                                StringBuilder sb = new StringBuilder();
                                for (int i = 1; i <= columns; i++) {
                                    if (sb.length() > 0) {
                                        sb.append("\t");
                                    }
                                    sb.append(md.getColumnName(i));
                                }
                                table.addRow(sb.toString());

                                //  Output Data
                                while (rs.next()) {
                                    sb.setLength(0);
                                    for (int i = 1; i <= columns; i++) {
                                        if (sb.length() > 0) {
                                            sb.append("\t");
                                        }
                                        sb.append(rs.getString(i));
                                    }
                                    table.addRow(sb.toString());
                                }
                            }
                            rs.close();
                            result.addResultTable(table);
                        }
                    }

                    moreResults = stmt.getMoreResults();
                }

                _results.add(result);
            }

        } catch (SQLException e) {
            Log.dbms.error("Error executing SQL statement: %e %s", e, e);
            throw (e);
        } finally {
            if (stmt != null) {
                stmt.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }


    /**
     * @return List of Results returned by these SQL Scripts
     */
    public List<Result> getResults() {
        return _results;
    }


    /**
     * @return Results as a String
     */
    public String getResultsString() {
        StringBuilder sb = new StringBuilder();
        if (_results != null) {
            for (Result r : _results) {
                sb.append(r.getStatement()).append("\r\n");
                List<ResultTable> tables = r.getResults();
                if (tables != null) {
                    for (ResultTable table : tables) {

                        //  Output Columns
                        List<String> columns = table.getColumns();
                        if (columns != null) {
                            StringBuilder sbColumn = new StringBuilder();
                            for (String col : columns) {
                                if (sbColumn.length() > 0) {
                                    sbColumn.append("\t");
                                }
                                sbColumn.append(col);
                            }
                            sb.append(sbColumn.toString());
                        }

                        //  Output Rows
                        List<String> rows = table.getRows();
                        if (rows != null) {
                            for (String row : rows) {
                                sb.append(row).append("\r\n");
                            }
                        }
                    }
                }
                sb.append("\r\n\r\n");  //  two lines
            }
        }
        return sb.toString();
    }

}
