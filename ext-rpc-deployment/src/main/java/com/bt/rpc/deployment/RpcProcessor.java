package com.bt.rpc.deployment;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import com.bt.rpc.annotation.Doc;
import com.bt.rpc.annotation.RpcService;
import com.bt.rpc.model.RpcResult;
import com.bt.rpc.runtime.ClientConfig;
import com.bt.rpc.runtime.ClientRecorder;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.logging.Logger;

public class RpcProcessor {

    static final Logger LOG = Logger.getLogger(RpcProcessor.class);
    static final String  FEATURE     = "ext-rpc";


    static final DotName RPC_SERVICE = DotName.createSimple(RpcService.class.getName());

    static final DotName RPC_RESULT = DotName.createSimple(RpcResult.class.getName());

    static final DotName DOC_ANNO = DotName.createSimple(Doc.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }



    @BuildStep
    void regRpcServiceForNative(ClientConfig config,
                                BuildProducer<ReflectiveClassBuildItem> reflective,
                           BuildProducer<NativeImageProxyDefinitionBuildItem> proxy,
                           CombinedIndexBuildItem indexBuildItem) {

        boolean serverExists = isServer();
        var dtoSet = new HashSet<String>();
        var annotationSetForMetaData = new HashSet<DotName>();

        if(serverExists) {
            annotationSetForMetaData.add(DOC_ANNO);
            reflective.produce(new ReflectiveClassBuildItem(true, false, DOC_ANNO.toString()));
            for (AnnotationInstance i : indexBuildItem.getIndex().getAnnotations(DOC_ANNO)) {
                if (i.target().kind() == AnnotationTarget.Kind.FIELD) {
                    i.target().asField().annotations()
                            .stream().map(AnnotationInstance::name)
                            .forEach(annotationSetForMetaData::add);
                }
            }
        }

        var serverList = new ArrayList<DotName>();
        for (AnnotationInstance i : indexBuildItem.getIndex().getAnnotations(RPC_SERVICE)) {
            var cls = i.target().asClass();
            //var dotName = cls.name().toString();
            serverList.add( cls.name());

            var methods = cls.methods();
            var thisSet = new HashSet<DotName>();
            for (var m : methods){
                if(serverExists) {
                    m.annotations().forEach(it -> annotationSetForMetaData.add(it.name()));
                }
                recursionParameterizedType(thisSet,m.returnType());
                m.parameters().forEach(it->recursionParameterizedType(thisSet,it));
                //addRefDtoClass(dtoSet, thisSet);

                thisSet.stream().map(DotName::toString)
                        .filter(c->! c.startsWith("java."))
                        .forEach(dtoSet::add);
            }
        }


        if( serverList.size() > 0){
            var clientClsList = new ArrayList<DotName>();
            boolean clientExists = new IsClient().getAsBoolean();
            if(clientExists && config !=null && config.apps != null &&  config.apps.size() > 0){
                config.apps.values().forEach(host->
                    serverList.forEach(s -> {
                        var clz = s.toString();
                        if (host.isMatch(clz)) {
                            clientClsList.add(s);
                            // 貌似代理批量执行有bug
                            // UnsupportedFeatureError: Proxy class defined by interfaces [interface CaptchaService] not found.
                            // Generating proxy classes at runtime is not supported.
                            proxy.produce(new NativeImageProxyDefinitionBuildItem(clz));
                        }
                    })
                );
            }

            if(clientClsList.size()>0){
                LOG.info("=== [ "+ clientClsList.size() +" RpcClient  ]  : " +
                        clientClsList.stream().map(DotName::withoutPackagePrefix).collect(Collectors.joining(",")));

                // 默认client和server不共存
                serverList.removeAll(clientClsList);
            }

            if(serverExists && serverList.size()>0){
                var array = serverList.stream().map(DotName::toString).toArray(String[]::new);
                reflective.produce(new ReflectiveClassBuildItem(true, false, array));
                LOG.info("=== [ "+ serverList.size() +" RpcService  ]  : " +
                        serverList.stream().map(DotName::withoutPackagePrefix).collect(Collectors.joining(",")));
            }

        }

        LOG.info("=== [ "+annotationSetForMetaData.size()+" Annotation ] For MetaDataService : " +
                annotationSetForMetaData.stream().map(DotName::withoutPackagePrefix).collect(Collectors.joining(","))
        );
        if(annotationSetForMetaData.size()>0) {
            reflective.produce(new ReflectiveClassBuildItem(true, false,
                    annotationSetForMetaData.stream().map(DotName::toString).toArray(String[]::new)));
        }

        LOG.info("=== [ "+dtoSet.size()+" DTO ] For Jackson : "+ dtoSet.stream()
                .map(name->name.substring(name.lastIndexOf('.')+1))
                .collect(Collectors.joining(","))
        );
        if(dtoSet.size() > 0) {
            reflective.produce(new ReflectiveClassBuildItem(true, true, dtoSet.toArray(new String[0])));

            var childSet = recursionNestDtoType(dtoSet);
            if(childSet.size()>0){
                var grandChild = recursionNestDtoType(childSet);
                childSet.addAll(grandChild);
                reflective.produce(new ReflectiveClassBuildItem(true, true, childSet.toArray(new String[0])));
                LOG.info("=== [ "+childSet.size()+" Nest DTO ] For Jackson : "+ childSet.stream()
                        .map(name->name.substring(name.lastIndexOf('.')+1))
                        .collect(Collectors.joining(","))
                );
            }
        }


    }

    static Set<String> recursionNestDtoType(Set<String> total){
        var childSet = new HashSet<String>();
        for(var dtoName : total){

            try {
                var base = Class.forName(dtoName);
                do {
                    extractSuper(childSet,total , base);
                    base = base.getSuperclass();
                    recursionNestDtoType( childSet,total,base);
                }while (base != Object.class);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return childSet;
    }

    private static void extractSuper( HashSet<String> childSet,Set<String> total, Class clz)  {
        for(var f : clz.getDeclaredFields()){
            var type = f.getGenericType();
            recursionNestDtoType(childSet, total,type);
        }
    }

    static void recursionNestDtoType(HashSet<String> childSet,Set<String> total,java.lang.reflect.Type type) {
        if(type instanceof Class ){
            var fClz = ((Class<?>) type);
            var fName = fClz.getName();
            // skip Array , use list
            if(!fClz.isPrimitive()
                    && ! fClz.isArray()
                    && ! fClz.isEnum()
                    && !fName.startsWith("java.")
                    && !total.contains(fName)){
                childSet.add(fName);
            }
        } else if(type instanceof ParameterizedType) {
            var pt = (ParameterizedType)type;
            recursionNestDtoType(childSet,total,pt.getRawType());
            for (var t : pt.getActualTypeArguments()){
                recursionNestDtoType(childSet,total,t);
            }
        }
    }

    static class IsClient implements BooleanSupplier {

        public boolean getAsBoolean() {
            return checkExists("com.bt.rpc.client.ClientContext");
        }
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(onlyIf = IsClient.class)
    void genRpcClientFactorys(ClientConfig config, BuildProducer<RpcServiceMBI> clientServiceMBIS,
                              BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
                              //BuildProducer<NativeImageProxyDefinitionBuildItem> proxy,
                              ClientRecorder recorder, CombinedIndexBuildItem indexBuildItem) throws  Exception {
        ClientProcessor.genRpcClientFactorys(config, clientServiceMBIS, syntheticBeanBuildItemBuildProducer, recorder, indexBuildItem);
    }


    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsClient.class)
    void genRpcClients(ClientRecorder recorder,
                       List<RpcServiceMBI> serviceMBIS,

                       BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {

        ClientProcessor.genRpcClients(recorder, serviceMBIS,syntheticBeanBuildItemBuildProducer);
    }




    static boolean isServer() {
        return   checkExists("com.bt.rpc.server.ServerContext");
    }

    static boolean checkExists(String client){
        try{
            Class.forName(client);
            return true;
        } catch (ClassNotFoundException e) {
            LOG.debug("=== IGNORE NotFound : " + client);
        }
        return false;

    }

    static void recursionParameterizedType(Set<DotName> total, Type t){
        if(t.kind() == Kind.CLASS){
            total.add(t.name());
        }else if(t.kind() == Kind.PARAMETERIZED_TYPE){
            var pt = t.asParameterizedType();
            if(! RPC_RESULT.equals(pt.name())) {
                total.add(t.name());
            }
            for(var s : pt.arguments()){
                recursionParameterizedType(total,s);
            }
        }
    }


}
