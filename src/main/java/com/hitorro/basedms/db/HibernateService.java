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
import com.hitorro.base.service.BasicService;
import com.hitorro.base.typesystem.Load;
import com.hitorro.base.typesystem.commands.DumpCommand;
import com.hitorro.basedms.cache.ObjectVersionsCache;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.basedms.session.DumpSessionInfo;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.jsontypesystem.propreaders.JVSProperties;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.json.keys.BooleanProperty;
import com.hitorro.util.json.keys.StringProperty;
import com.hitorro.util.json.keys.propaccess.PropaccessError;
import com.hitorro.util.objects.ObjectVersions;
import com.hitorro.util.startupframework.phases.ServiceDefinition;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.Statistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@ServiceDefinition(dependentService = {BasicService.class, com.hitorro.util.typesystem.TypeManagerService.class, AutoDBCreateService.class},
        shortName = "hibernate",
        description = "Hibernate OR persistence integration",
        debugCommands = {DumpCommand.class, Load.class, Hql.class, DumpHibernateStatistics.class, DumpSessionInfo.class},
        typeManagedClasses = {},
        uiDirectories = {})
public class HibernateService {
    public static final StringProperty DerbyDB = new StringProperty("db.embedded.name", "", null);
    public static final StringProperty DefaultDBKey = DatabaseUtil.DefaultDBKey;
    public static final StringProperty UserName = DatabaseUtil.UsernameKey;
    public static final StringProperty Password = DatabaseUtil.PasswordKey;
    public static final StringProperty DBUrl = DatabaseUtil.DatabaseUrlKey;
    public static final BooleanProperty AllowUpgrade = new BooleanProperty("allowupgrade", "", false);
    public static final StringProperty DBConfigKey = new StringProperty("dbconfigkey", "Key used to get the hibernate properties", null);
    public static final StringProperty ConnectionKey = new StringProperty("connectionkey", "Connection info", null);
    public static final BooleanProperty HibnernateStatistics = new BooleanProperty("hibernate.enablestats", "", false);
    public static boolean s_isInitialized = false;
    public static HibernateService s_service;
    BootstrapServiceRegistry bootstrapRegistry;
    StandardServiceRegistry standardRegistry;
    private Statistics m_statistics;
    private ObjectVersionsCache objectVersionCache = new ObjectVersionsCache();

    public String init(boolean dbInit, final boolean upgrading, final long currentVersion, final long targetVersion) {
        com.hitorro.util.typesystem.BaseSessionFactory.setFactory(new DMSSessionFactory());

        Configuration cfg = new Configuration(getBootstrapRegistry());
        com.hitorro.util.typesystem.TypeManager tm = com.hitorro.util.typesystem.TypeManager.getTypeManager();

        List<com.hitorro.util.typesystem.Type> persistedTypes = tm.getListOfPersistedTypes();
        s_service = this;

        for (com.hitorro.util.typesystem.Type type : persistedTypes) {
            cfg.addClass(type.getImplementationClass());
        }

        String connectionKey = DefaultDBKey.apply();

        try {
            createConfigFromProps(cfg,
                    "dbconfig",
                    connectionKey,
                    DefaultDBKey.apply(),
                    dbInit,
                    true,
                    false,
                    DerbyDB.apply());
        } catch (PropaccessError propaccessError) {
            return null;
        }
        if (HibnernateStatistics.apply()) {
            m_statistics = HibernateUtil.getStatistics();
            m_statistics.setStatisticsEnabled(true);
        }
        s_isInitialized = true;
        return validatePersistedTypes();
    }

    public boolean ensureSessionFactory(String key) throws PropaccessError {
        SessionFactory sf = HibernateUtil.getSessionFactory(key);
        if (sf == null) {
            JsonNode connectInfo = JVSProperties.getProperties().get(key);
            if (connectInfo == null) {
                return false;
            }
            String dbConfig = DBConfigKey.apply(connectInfo);
            String dbConnect = ConnectionKey.apply(connectInfo);
            Configuration conf = new Configuration();
            return createConfigFromProps(conf, dbConfig, dbConnect, key, false, false, true, DerbyDB.apply());
        } else {
            // already exists
            return true;
        }
    }

    private final boolean createConfigFromProps(Configuration conf,
                                                String databaseConfigKey,
                                                String connectConfigKey,
                                                String sessionKey,
                                                boolean init,
                                                boolean defaultKey,
                                                boolean subdueUpdates,
                                                String derbyName) throws PropaccessError {

        if (!StringUtil.nullOrEmptyString(derbyName)) {
            databaseConfigKey = "dbconfigderby";
            connectConfigKey = "derbydb";
        }
        //TODO UPDATE
        //addEventHandlers(conf);
        //addEventHandlersNew(conf);
        //Properties props = HTProperties.getProperties().getSubProperties(databaseConfigKey);

        JVS propsJVS = new JVS(JVSProperties.getProperties().get(databaseConfigKey));
        Properties props = propsJVS.getAsProperties();
        JsonNode connectInfo = JVSProperties.getProperties().get(connectConfigKey);

        String username = UserName.apply(connectInfo);
        String password = Password.apply(connectInfo);
        if (StringUtil.nullOrEmptyOrBlankString(username) || password == null) {
            // bad configuration
            return false;
        }
        HibernateUtil.setUsername(props, username, password);
        if (init) {
            HibernateUtil.setCreateMode(props, "create");
        } else {
            if (!subdueUpdates) {
                HibernateUtil.setCreateMode(props, "update");
            }
        }
        HibernateUtil.setUrl(props, DBUrl.apply(connectInfo));
        conf.setProperties(props);

        HibernateUtil.setSessionFactory(sessionKey, conf, defaultKey);
        return true;
    }

    private void addEventHandlersNew(Configuration conf) {
        getBootstrapRegistry();


        StandardServiceRegistryBuilder standardRegistryBuilder = new StandardServiceRegistryBuilder(bootstrapRegistry);
        standardRegistry = standardRegistryBuilder.build();
    }

    private BootstrapServiceRegistry getBootstrapRegistry() {
        BootstrapServiceRegistryBuilder builder = new BootstrapServiceRegistryBuilder();
        builder.applyIntegrator(new HTIntegrator());
        return builder.build();
    }

    public String start(boolean dbInit) {
        return null;
    }

    public String run() {
        return null;
    }

    public Statistics getStatistics() {
        return m_statistics;
    }

    public String deInit() {
        s_service = null;
        s_isInitialized = false;
        return null;
    }

    public String validatePersistedTypes() {

        return validateTypes();
    }

    /**
     * @return empty string if all good, else an error message to process back to the initializing service
     */
    private String validateTypes() {
        List<ObjectVersions> commitList = new ArrayList<ObjectVersions>();
        objectVersionCache.load();
        boolean allowUpgrade = AllowUpgrade.apply();
        for (com.hitorro.util.typesystem.Type t : com.hitorro.util.typesystem.TypeManager.getTypeManager().getTypes()) {
            String response = validateType(t, commitList, allowUpgrade);
            if (!StringUtil.nullOrEmptyOrBlankString(response)) {
                return response;
            }
        }
        com.hitorro.util.typesystem.BaseSession session = DMSSessionFactory.getFactory().getSession();


        try {
            session.commitList(commitList);
            com.hitorro.util.typesystem.Log.typemanager.debug("Commited new ObjectVersions to the disk");
        } finally {
            DMSSessionFactory.closeSession(session);
        }
        return "";
    }

    private String validateType(com.hitorro.util.typesystem.Type t, List<ObjectVersions> commitList, boolean allowUpgrade) {
        if (t.getTypeMeta() == null) {
            com.hitorro.util.typesystem.Log.typemanager.fatal("Type does not have meta data %s", t.getImplementationClass());
            return null;
        }
        if (t.getTypeMeta().isPersisted()) {
            String shortName = t.getName();
            if (StringUtil.nullOrEmptyOrBlankString(shortName)) {
                String s = Fmt.S("Type %s does not have a short name defined",
                        t.getImplementationClass().getCanonicalName());
                com.hitorro.util.typesystem.Log.typemanager.fatal(s);
                return s;
            }

            ObjectVersions version = objectVersionCache.get(shortName);
            if (version == null) {
                // version doesnt exist, its new
                com.hitorro.util.typesystem.Log.typemanager.info("Type %s (%s) is new to the schema, creating version object", shortName, t.getShortClassName());
                ObjectVersions ov = new ObjectVersions();
                ov.setName(t.getName());
                ov.setObjectVersion(t.getSchemaVersion());
                commitList.add(ov);
            } else {
                int sVersion = t.getSchemaVersion();
                long objVersion = version.getObjectVersion();
                if (objVersion != sVersion) {

                    if (allowUpgrade) {
                        try {
                            Object instance = t.getImplementationClass().newInstance();
                            if (instance instanceof com.hitorro.util.typesystem.BaseType) {
                                com.hitorro.util.typesystem.BaseType bt = (com.hitorro.util.typesystem.BaseType) instance;
                                if (bt.upgradeAllInstances(version.getObjectVersion())) {
                                    // able to upgradeAllInstances it!
                                    com.hitorro.util.typesystem.BaseSession session = DMSSessionFactory.getFactory().getSession();
                                    try {
                                        version.setObjectVersion(sVersion);
                                        session.saveOrUpdate(version);
                                        session.commit();
                                    } finally {
                                        DMSSessionFactory.getFactory().rollbackClose(session);
                                    }

                                    return null;
                                }
                            }
                        } catch (InstantiationException e) {
                            com.hitorro.util.typesystem.Log.typemanager.error("%s %e", e, e);
                        } catch (IllegalAccessException e) {
                            com.hitorro.util.typesystem.Log.typemanager.error("%s %e", e, e);
                        }

                    }
                    String s = Fmt.S("Type %s(%s) has persisted schema version %s but object schema version of %s",
                            shortName, t.getShortClassName(), version.getObjectVersion(), sVersion);

                    com.hitorro.util.typesystem.Log.typemanager.fatal(s);
                    return s;
                } else {
                    com.hitorro.util.typesystem.Log.typemanager.debug("Type %s(%s) has persisted schema version of %s",
                            shortName,
                            t.getShortClassName(),
                            sVersion);
                }
            }
        }
        return null;
    }
}
