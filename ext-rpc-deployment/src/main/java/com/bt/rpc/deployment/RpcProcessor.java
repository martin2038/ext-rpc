package com.bt.rpc.deployment;

import java.util.HashSet;
import java.util.Set;

import com.bt.rpc.annotation.Doc;
import com.bt.rpc.annotation.RpcService;
import com.bt.rpc.model.RpcResult;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
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

    private static final Logger LOG = Logger.getLogger(RpcProcessor.class);
    private static final String  FEATURE     = "ext-rpc";


    private static final DotName RPC_SERVICE = DotName.createSimple(RpcService.class.getName());

    private static final DotName RPC_RESULT = DotName.createSimple(RpcResult.class.getName());

    private static final DotName DOC_ANNO = DotName.createSimple(Doc.class.getName());

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

        var dtoSet = new HashSet<String>();
        var annoSet = new HashSet<DotName>();

        if(serverExists) {
            annoSet.add(DOC_ANNO);
            reflective.produce(new ReflectiveClassBuildItem(true, false, DOC_ANNO.toString()));
            for (AnnotationInstance i : indexBuildItem.getIndex().getAnnotations(DOC_ANNO)) {
                if (i.target().kind() == AnnotationTarget.Kind.FIELD) {
                    i.target().asField().annotations()
                            .stream().map(AnnotationInstance::name)
                            .forEach(annoSet::add);
                }
            }
        }

        for (AnnotationInstance i : indexBuildItem.getIndex().getAnnotations(RPC_SERVICE)) {
            var cls = i.target().asClass();
            var dotName = cls.name().toString();
            if(clientExists){
                proxy.produce(new NativeImageProxyDefinitionBuildItem(dotName));
                LOG.info("=== Client Proxy : " + dotName);
            }
            if(serverExists){
                reflective.produce(new ReflectiveClassBuildItem(true, false, dotName));
                LOG.info("=== Server Reflective : " + dotName);
            }

            var methods = cls.methods();
            var thisSet = new HashSet<DotName>();
            for (var m : methods){

                if(serverExists) {
                    m.annotations().forEach(it -> annoSet.add(it.name()));
                }
                
                recursionParameterizedType(thisSet,m.returnType());
                m.parameters().forEach(it->recursionParameterizedType(thisSet,it));
                //addRefDtoClass(dtoSet, thisSet);

                thisSet.stream().map(DotName::toString)
                        .filter(c->! c.startsWith("java."))
                        .forEach(dtoSet::add);
            }
        }

        LOG.info("=== Annotation Reflective : " + annoSet.size());
        if(annoSet.size()>0) {
            reflective.produce(new ReflectiveClassBuildItem(true, false,
                    annoSet.stream().map(DotName::toString).toArray(String[]::new)));
            LOG.debug("=== Annotation Reflective :"+annoSet);
        }

        LOG.info("=== DTO Reflective : "+ dtoSet.size());
        if(dtoSet.size() > 0) {
            reflective.produce(new ReflectiveClassBuildItem(true, true, dtoSet.toArray(new String[0])));
            LOG.debug("=== DTO Reflective :"+dtoSet);
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


}
