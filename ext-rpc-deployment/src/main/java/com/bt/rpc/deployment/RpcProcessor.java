package com.bt.rpc.deployment;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.bt.rpc.annotation.RpcService;
import com.bt.rpc.model.RpcResult;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.logging.Logger;

public class RpcProcessor {

    private static final Logger LOG = Logger.getLogger(RpcProcessor.class);
    private static final String  FEATURE     = "ext-rpc";
    private static final DotName RPC_SERVICE = DotName.createSimple(RpcService.class.getName());

    private static final DotName RPC_RESULT = DotName.createSimple(RpcResult.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }



    @BuildStep
    void regRpcServiceForNative(//BuildProducer<MyBatisMapperBuildItem> mappers,
                           BuildProducer<ReflectiveClassBuildItem> reflective,
                           BuildProducer<NativeImageProxyDefinitionBuildItem> proxy,
                           CombinedIndexBuildItem indexBuildItem) {


        boolean clientExists =  checkExists("com.bt.rpc.client.ClientContext");
        boolean serverExists =  checkExists("com.bt.rpc.server.ServerContext");

        var reflectiveSet = new HashSet<DotName>();

        for (AnnotationInstance i : indexBuildItem.getIndex().getAnnotations(RPC_SERVICE)) {
            var cls = i.target().asClass();
            DotName dotName = cls.name();
            if(clientExists){
                proxy.produce(new NativeImageProxyDefinitionBuildItem(dotName.toString()));
                LOG.info("=== Client === Proxy : " + dotName);
            }
            if(serverExists){
                reflective.produce(new ReflectiveClassBuildItem(true, false, dotName.toString()));
                LOG.info("=== Server === Reflective : " + dotName);
            }

            var methods = cls.methods();
            var thisSet = new HashSet<DotName>();
            for (var m : methods){
                recursionParameterizedType(thisSet,m.returnType());
                m.parameters().forEach(it->recursionParameterizedType(thisSet,it));
                addRefDtoClass(reflectiveSet, reflective, thisSet);
            }
        }

    }

    static boolean checkExists(String client){

        try{
            Class.forName(client);
            return true;
        } catch (ClassNotFoundException e) {
            LOG.warn("=== IGNORE " + client);
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

    private static void addRefDtoClass(Set<DotName> set, BuildProducer<ReflectiveClassBuildItem> reflective, Collection<DotName> clz){
        for(var c : clz){
            if (set.add(c) && ! c.toString().startsWith("java.") ){
                LOG.info("====== Reflective DTO :: "+ c);
                reflective.produce(new ReflectiveClassBuildItem(true, true,  c.toString()));
            }
        }
    }

}
