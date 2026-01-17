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
import com.hitorro.jsontypesystem.propreaders.JVSProperties;

/**
 * Factory for providing database configuration to HibernateService.
 * 
 * By default, reads from JVSProperties (the standard Hitorro way).
 * Spring Boot integration can override this to provide database configuration
 * from Spring's DataSource instead.
 * 
 * This allows Spring's application.yml to be the single source of truth for
 * database configuration.
 */
public class DatabaseConfigProvider {
    
    private static DatabaseConfigProvider instance = new DefaultDatabaseConfigProvider();
    
    /**
     * Set a custom database config provider.
     * Spring Boot integration uses this to override with Spring's DataSource configuration.
     */
    public static void setProvider(DatabaseConfigProvider provider) {
        instance = provider;
    }
    
    /**
     * Get the current database config provider.
     */
    public static DatabaseConfigProvider getProvider() {
        return instance;
    }
    
    /**
     * Get the database configuration for the given connection key.
     * 
     * @param connectionKey The database connection key (e.g., "defaultdb", "testdb")
     * @return JsonNode containing url, username, password
     */
    public JsonNode getDatabaseConfig(String connectionKey) {
        // Override in subclasses
        return null;
    }
    
    /**
     * Get the database configuration properties key (e.g., "dbconfig").
     * 
     * @param databaseConfigKey The config key prefix
     * @return JsonNode containing Hibernate configuration properties
     */
    public JsonNode getHibernateConfig(String databaseConfigKey) {
        // Override in subclasses
        return null;
    }
    
    /**
     * Default implementation that reads from JVSProperties.
     */
    static class DefaultDatabaseConfigProvider extends DatabaseConfigProvider {
        
        @Override
        public JsonNode getDatabaseConfig(String connectionKey) {
            return JVSProperties.getProperties().get(connectionKey);
        }
        
        @Override
        public JsonNode getHibernateConfig(String databaseConfigKey) {
            return JVSProperties.getProperties().get(databaseConfigKey);
        }
    }
}
