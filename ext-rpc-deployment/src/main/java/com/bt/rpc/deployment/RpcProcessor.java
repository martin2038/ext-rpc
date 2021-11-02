package com.bt.rpc.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.bt.rpc.annotation.RpcService;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

public class RpcProcessor {

    private static final Logger LOG = Logger.getLogger(RpcProcessor.class);
    private static final String  FEATURE     = "ext-rpc";
    private static final DotName RPC_SERVICE = DotName.createSimple(RpcService.class.getName());
    //private static final DotName MYBATIS_TYPE_HANDLER = DotName.createSimple(MappedTypes.class.getName());
    //private static final DotName MYBATIS_JDBC_TYPE_HANDLER = DotName.createSimple(MappedJdbcTypes.class.getName());
    //private static final DotName MYBATIS_MAPPER_DATA_SOURCE = DotName.createSimple(MapperDataSource.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
    //
    //@BuildStep
    //void runtimeInitialzed(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit) {
    //    runtimeInit.produce(new RuntimeInitializedClassBuildItem(Log4jImpl.class.getName()));
    //}
    //
    //@BuildStep
    //void refProxyClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
    //                       BuildProducer<NativeImageProxyDefinitionBuildItem> proxyClass) {
    //
    //}






    @BuildStep
    void addMyBatisMappers(//BuildProducer<MyBatisMapperBuildItem> mappers,
                           BuildProducer<ReflectiveClassBuildItem> reflective,
                           BuildProducer<NativeImageProxyDefinitionBuildItem> proxy,
                           CombinedIndexBuildItem indexBuildItem) throws URISyntaxException, IOException {
        var set = new HashSet<Class>();
        LOG.info("begin hanler : "+ RPC_SERVICE);

        //Set<Class<?>> annotated =
        //        reflections.getTypesAnnotatedWith(RpcService.class);
        //
        //LOG.info("found com.btyx RpcService :" + annotated);


        LOG.info("found com.btyx ClassFinder :" + new ClassFinder("com.btyx").getClasses());


        for (AnnotationInstance i : indexBuildItem.getIndex().getAnnotations(RPC_SERVICE)) {
            LOG.info("found " + i.target() + " , kind :" + i.target().kind());

            if (i.target().kind() == AnnotationTarget.Kind.CLASS) {
                DotName dotName = i.target().asClass().name();
                reflective.produce(new ReflectiveClassBuildItem(true, false, dotName.toString()));
                try {
                    var methods = Class.forName(dotName.toString()).getDeclaredMethods();
                    var thisSet = new HashSet<Class>();
                    for (var m : methods){
                        recursionParameterizedType(thisSet,m.getGenericReturnType());
                        for(var t : m.getGenericParameterTypes()){
                            recursionParameterizedType(thisSet,t);
                        }
                        addSqlParamReflectiveClass(set, reflective, thisSet);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }



                // TODO rpc client proxy

                //proxy.produce(new NativeImageProxyDefinitionBuildItem(dotName.toString()));
                //
                //Optional<AnnotationInstance> mapperDatasource = i.target().asClass().annotations().entrySet().stream()
                //        .filter(entry -> entry.getKey().equals(MYBATIS_MAPPER_DATA_SOURCE))
                //        .map(Map.Entry::getValue)
                //        .map(annotationList -> annotationList.get(0))
                //        .findFirst();
                //if (mapperDatasource.isPresent()) {
                //    String dataSourceName = mapperDatasource.get().value().asString();
                //    mappers.produce(new MyBatisMapperBuildItem(dotName, dataSourceName));
                //} else {
                //    mappers.produce(new MyBatisMapperBuildItem(dotName, "<default>"));
                //}
            }
        }
    }

    static void recursionParameterizedType(Set<Class> total,Type t){
        if(t instanceof Class){
            total.add((Class) t);
        }else if(t instanceof ParameterizedType){
            var pt = ((ParameterizedType) t).getRawType();
            recursionParameterizedType(total,pt);
            var sub = ((ParameterizedType) t).getActualTypeArguments();
            for(var s : sub){
                recursionParameterizedType(total,s);
            }
        }
    }

    //@BuildStep
    //void addConfigurations(MyConfig config,
    //                       BuildProducer<ConfigurationMBI> configurations,
    //                       BuildProducer<MapperMBI> mappers,
    //                       BuildProducer<NativeImageResourceBuildItem> nativeResources,
    //                       BuildProducer<ReflectiveClassBuildItem> reflective,
    //                       BuildProducer<NativeImageProxyDefinitionBuildItem> proxy) throws Exception{
    //
    //    var files = config.configFiles.split(",");
    //    //var defaultAlias =  new Configuration().getTypeAliasRegistry().getTypeAliases();
    //    var clsSet = new HashSet<Class>();
    //    for(var configFile : files){
    //        var cfg =  new XmlConfigurationFactory(configFile).createConfiguration();
    //
    //        String xmlFolder = cfg.getVariables().getProperty("SqlXmlFolder");
    //        if(xmlFolder.charAt(0)=='/'){
    //            xmlFolder = xmlFolder.substring(1);
    //        }
    //        var folder = xmlFolder;
    //
    //        URL resource =  Resources.getResourceURL(folder);
    //
    //        LOG.info("=== addConfigurations : " + configFile +" -> "+ folder +", url : "+resource);
    //
    //        var sqlMaps =  Files.walk(Paths.get(resource.toURI()))
    //                .filter(Files::isRegularFile)
    //                .map(x ->   folder +"/" + x.getName(x.getNameCount()-1))
    //                .filter(it->it.endsWith(".xml"))
    //                .collect(Collectors.toList());
    //
    //        var ds =  (QuarkusDataSource)cfg.getEnvironment().getDataSource();
    //
    //        var dsName = ds.getDataSourceName();
    //
    //       for (var mapCls : cfg.getMapperRegistry().getMappers()){
    //
    //           if(! clsSet.add(mapCls)){
    //               LOG.info("=== Skip Exists Mapper Class :: "+mapCls.getName());
    //               continue;
    //           }
    //
    //           reflective.produce(new ReflectiveClassBuildItem(true, false, mapCls));
    //           proxy.produce(new NativeImageProxyDefinitionBuildItem(mapCls.getName()));
    //
    //           LOG.info("=== Reflective & Proxy Mapper Class :: "+mapCls.getName());
    //           mappers.produce(new MapperMBI(DotName.createSimple(mapCls.getName()), dsName));
    //           for(var m :  mapCls.getDeclaredMethods()){
    //               addSqlParamReflectiveClass(clsSet,reflective,m.getReturnType());
    //               addSqlParamReflectiveClass(clsSet,reflective,m.getParameterTypes());
    //           }
    //       }
    //
    //       //var alias = cfg.getTypeAliasRegistry().getTypeAliases();
    //       //alias.forEach((k,clz)->{
    //       //    if(! defaultAlias.containsKey(k) && clz != QuarkusDataSourceFactory.class){
    //       //        //LOG.info("=== NO Reflective Alias Class :: " +k+"->"+clz.getName());
    //       //        //reflective.produce(new ReflectiveClassBuildItem(true, true, clz));
    //       //    }
    //       //});
    //
    //        configurations.produce(new ConfigurationMBI(configFile,dsName,sqlMaps));
    //    }
    //}
    //

    private static void addSqlParamReflectiveClass(Set<Class> set, BuildProducer<ReflectiveClassBuildItem> reflective, Collection<Class> clz){
        for(var c : clz){
            //if(type instanceof  Class){
            //
            //}
            if (set.add(c)
                    && ! c.isPrimitive()
                    && ! c.isArray()
                    && ! c.isEnum()
                    && ! CharSequence.class.isAssignableFrom(c)
            && ! Number.class.isAssignableFrom(c)
            && ! Iterable.class.isAssignableFrom(c)
            && ! Map.class.isAssignableFrom(c)
            ){
                LOG.info("====== Reflective From RpcService  :: "+ c);
                reflective.produce(new ReflectiveClassBuildItem(true, false, c));
            }
        }
    }

    //@BuildStep
    //NativeImageResourceBuildItem nativeImageResourceBuildItem(List<ConfigurationMBI> configurationMBIS) {
    //    List<String> resources = new ArrayList<>();
    //    configurationMBIS.forEach(it->resources.addAll(it.getMapperXml()));
    //    LOG.info("=== Reg NativeImageResource: "  + resources);
    //    return new NativeImageResourceBuildItem(resources);
    //}

    //@Record(ExecutionTime.STATIC_INIT)
    //@BuildStep
    //void generateSqlSessionFactorys(List<ConfigurationMBI> configurationMBIS,
    //                                BuildProducer<SqlSessionMBI> sqlSessionMBIBuildProducer,
    //                                BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
    //        RpcRecorder recorder) throws  Exception {
    //    for(var cbi : configurationMBIS) {
    //        var factoryRuntime  = recorder.createSqlSessionFactory(
    //                new XmlConfigurationFactory(cbi.getMybatisConfigFile()),cbi.getMapperXml());
    //        var sqlSessionMBI  = new SqlSessionMBI(factoryRuntime
    //                        , recorder.createSqlSessionManager(factoryRuntime)
    //                        , cbi.getDataSourceName(), cbi.isDefaultDs());
    //        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
    //                .configure(SqlSessionFactory.class)
    //                .scope(Singleton.class)
    //                .unremovable()
    //                .supplier(recorder.MyBatisSqlSessionFactorySupplier(sqlSessionMBI.getSqlSessionFactory()));
    //        // Unable to serialize objects of type class com.bt.mybatis.deployment.BtMybatisProcessor$$Lambda$1906/0x0000000801b1a200 to
    //        // bytecode as it has no default constructor
    //                //.supplier(sqlSessionMBI.getSqlSessionFactory()::getValue);
    //        String dataSourceName = sqlSessionMBI.getDataSourceName();
    //        if (!sqlSessionMBI.isDefaultDataSource()) {
    //            configurator.defaultBean();
    //            configurator.addQualifier().annotation(Named.class).addValue("value", dataSourceName).done();
    //        }
    //        LOG.info("=== STATIC_INIT CDI SqlSessionFactory :"+ sqlSessionMBI);
    //        sqlSessionMBIBuildProducer.produce(sqlSessionMBI);
    //        syntheticBeanBuildItemBuildProducer.produce(configurator.done());
    //    }
    //}


    //@Record(ExecutionTime.RUNTIME_INIT)
    //@BuildStep
    //void generateMapperBeans(RpcRecorder recorder,
    //                         List<MapperMBI> mapperMBIS,
    //                         //List<MyBatisMappedTypeBuildItem> myBatisMappedTypesBuildItems,
    //                         //List<MyBatisMappedJdbcTypeBuildItem> myBatisMappedJdbcTypesBuildItems,
    //                         List<SqlSessionMBI> sqlSessionFacItems,
    //                         BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
    //    var dataSourceToSessionManagers = sqlSessionFacItems.stream()
    //            .collect(Collectors.toMap(SqlSessionMBI::getDataSourceName, SqlSessionMBI::getSqlSessionManager));
    //    for (MapperMBI i : mapperMBIS) {
    //        var sqlSessionManager = dataSourceToSessionManagers.get(i.getDataSourceName());
    //        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
    //                .configure(i.getMapperName())
    //                .scope(Singleton.class)
    //                .setRuntimeInit()
    //                .unremovable()
    //                .supplier(recorder.MyBatisMapperSupplier(i.getMapperName().toString(),
    //                        sqlSessionManager));
    //        LOG.info("=== RUNTIME_INIT CDI Mapper Bean : " + i.getMapperName());
    //        syntheticBeanBuildItemBuildProducer.produce(configurator.done());
    //    }
    //
    //}
    //for (MyBatisMappedTypeBuildItem i : myBatisMappedTypesBuildItems) {
    //    SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
    //            .configure(i.getMappedTypeName())
    //            .scope(Singleton.class)
    //            .setRuntimeInit()
    //            .unremovable()
    //            .supplier(recorder.MyBatisMappedTypeSupplier(i.getMappedTypeName().toString(),
    //                    defaultSqlSessionManagerBuildItem.getSqlSessionManager()));
    //    syntheticBeanBuildItemBuildProducer.produce(configurator.done());
    //}
    //for (MyBatisMappedJdbcTypeBuildItem i : myBatisMappedJdbcTypesBuildItems) {
    //    SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
    //            .configure(i.getMappedJdbcTypeName())
    //            .scope(Singleton.class)
    //            .setRuntimeInit()
    //            .unremovable()
    //            .supplier(recorder.MyBatisMappedJdbcTypeSupplier(i.getMappedJdbcTypeName().toString(),
    //                    defaultSqlSessionManagerBuildItem.getSqlSessionManager()));
    //    syntheticBeanBuildItemBuildProducer.produce(configurator.done());
    //}


}
