package com.bt.rpc.deployment;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import com.bt.rpc.annotation.Doc;
import com.bt.rpc.annotation.RpcService;
import com.bt.rpc.client.RpcClientFactory;
import com.bt.rpc.model.RpcResult;
import com.bt.rpc.runtime.ClientConfig;
import com.bt.rpc.runtime.RpcRecorder;
import com.bt.rpc.runtime.ServerApp;
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
    void regRpcServiceForNative(
                           BuildProducer<ReflectiveClassBuildItem> reflective,
                           BuildProducer<NativeImageProxyDefinitionBuildItem> proxy,
                           CombinedIndexBuildItem indexBuildItem) {


        boolean clientExists = new IsClient().getAsBoolean();
        boolean serverExists = isServer();

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


    static class IsClient implements BooleanSupplier {

        public boolean getAsBoolean() {
            return checkExists("com.bt.rpc.client.ClientContext");
        }
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(onlyIf = IsClient.class)
    void genRpcClientFactorys(ClientConfig config,BuildProducer<RpcServiceMBI> clientServiceMBIS,
                                    BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
                                RpcRecorder recorder,CombinedIndexBuildItem indexBuildItem) throws  Exception {

        if( config.apps.isEmpty()){
            LOG.info("==== SKip genRpcClientFactorys ..");
            return;
        }

        var services =  indexBuildItem.getIndex().getAnnotations(RPC_SERVICE)
                    .stream().map(it-> {
                    try {
                        return Class.forName(it.target().asClass().name().toString());
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toSet());


        for (Entry<String, ServerApp> entry : config.apps.entrySet()) {
            String app = entry.getKey();
            ServerApp host = entry.getValue();

            var matched = services.stream().filter(it->host.isMatch(it.getName())).collect(Collectors.toList());

            if(matched.isEmpty()){
                LOG.info("=== Ignore Empty Service Found for : "+ host);
                continue;
            }

            matched.forEach(s->{
                clientServiceMBIS.produce(new RpcServiceMBI(s,app));
                LOG.info("=== Mapping : " + s  +" -> " +app);
            });


            var channelRuntime = recorder.createManagedChannel(host.url);

            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(RpcClientFactory.class)
                    .scope(ApplicationScoped.class)
                    .unremovable()
                    //.destroyer(ClientDestory.class)
                    .supplier(recorder.clientFactorySupplier(channelRuntime,app));

            configurator.defaultBean();
            configurator.addQualifier().annotation(Named.class).addValue("value", app).done();


            LOG.info("=== STATIC_INIT CDI RpcClientFactory :"+host.url+"/"+app);
            syntheticBeanBuildItemBuildProducer.produce(configurator.done());
        }
    }

    //static class ClientDestory implements BeanDestroyer<RpcClientFactory> {
    //
    //    @Override
    //    public void destroy(RpcClientFactory instance, CreationalContext creationalContext, Map params) {
    //        LOG.info("=== close : "+ instance);
    //        if(null != instance){
    //            instance.close();
    //        }
    //    }
    //}

    //
    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsClient.class)
    void genRpcClients(RpcRecorder recorder,
                             List<RpcServiceMBI> serviceMBIS,
                             BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) throws ClassNotFoundException {
        for (RpcServiceMBI i : serviceMBIS) {

            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(i.getName())
                    .scope(ApplicationScoped.class)//.scope(Singleton.class)
                    .setRuntimeInit()
                    .unremovable()
                    .supplier(recorder.rpcClientSupplier( i.getName(), i.getApp()));
            configurator.defaultBean();
            configurator.addQualifier().annotation(Named.class).addValue("value", i.getApp()).done();

            LOG.info("=== RUNTIME_INIT CDI RpcService : " +i.getName() +" -> " + i.getApp());
            syntheticBeanBuildItemBuildProducer.produce(configurator.done());
        }

    }




    static boolean isServer() {
        return   checkExists("com.bt.rpc.server.ServerContext");
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
