package tech.krpc.ext.runtime;

import java.util.Map;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "rpc.client", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class ClientConfig {

    public static final String GLOBAL  = "global";

    /**
     * 服务地址相关配置
     */
    @ConfigDocSection
    @ConfigDocMapKey("server-app-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, ServerApp> apps;

    /**
     * 配置服务的过滤器 ， 全局使用 ,多个英文逗号分隔
     */
    @ConfigItem
    public Map<String, Set<String>> filters;



    ///**
    // * cache provider
    // */
    //@ConfigItem//(defaultValue = "SESSION")
    //public String cache;

    @Override
    public String toString() {
        return "ClientConfig{" +
                "apps=" + apps +
                ", filters=" + filters  +
                '}';
    }

    //
    ///**
    // * MyBatis jdbcTypeForNull
    // */
    //@ConfigItem(defaultValue = "OTHER")
    //public JdbcType jdbcTypeForNull;
    //
    ///**
    // * MyBatis lazyLoadTriggerMethods
    // */
    //@ConfigItem(defaultValue = "equals,clone,hashCode,toString")
    //public Set<String> lazyLoadTriggerMethods;
    //
    ///**
    // * MyBatis defaultScriptingLanguage
    // */
    //@ConfigItem(defaultValue = "org.apache.ibatis.scripting.xmltags.XMLLanguageDriver")
    //public String defaultScriptingLanguage;
    //
    ///**
    // * MyBatis defaultEnumTypeHandler
    // */
    //@ConfigItem(defaultValue = "org.apache.ibatis.type.EnumTypeHandler")
    //public String defaultEnumTypeHandler;
    //
    ///**
    // * MyBatis callSettersOnNulls
    // */
    //@ConfigItem(defaultValue = "false")
    //public boolean callSettersOnNulls;
    //
    ///**
    // * MyBatis returnInstanceForEmptyRow
    // */
    //@ConfigItem(defaultValue = "false")
    //public boolean returnInstanceForEmptyRow;
    //
    ///**
    // * MyBatis logPrefix
    // */
    //@ConfigItem
    //public Optional<String> logPrefix;
    //
    ///**
    // * MyBatis logImpl
    // */
    //@ConfigItem
    //public Optional<String> logImpl;
    //
    ///**
    // * MyBatis proxyFactory
    // */
    //@ConfigItem(defaultValue = "JAVASSIST")
    //public String proxyFactory;
    //
    ///**
    // * MyBatis vfsImpl
    // */
    //@ConfigItem
    //public Optional<String> vfsImpl;
    //
    ///**
    // * MyBatis useActualParamName
    // */
    //@ConfigItem(defaultValue = "true")
    //public boolean useActualParamName;
    //
    ///**
    // * MyBatis configurationFactory
    // */
    //@ConfigItem
    //public Optional<String> configurationFactory;
    //
    ///**
    // * MyBatis shrinkWhitespacesInSql
    // */
    //@ConfigItem(defaultValue = "false")
    //public boolean shrinkWhitespacesInSql;
    //
    ///**
    // * MyBatis defaultSqlProviderType
    // */
    //@ConfigItem
    //public Optional<String> defaultSqlProviderType;
}
