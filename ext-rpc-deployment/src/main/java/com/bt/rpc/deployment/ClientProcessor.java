/**
 * Botaoyx.com Inc.
 * Copyright (c) 2021-2021 All Rights Reserved.
 */
package com.bt.rpc.deployment;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import com.bt.rpc.client.RpcClientFactory;
import com.bt.rpc.runtime.ClientConfig;
import com.bt.rpc.runtime.ClientRecorder;
import com.bt.rpc.runtime.ServerApp;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.logging.Logger;

import static com.bt.rpc.deployment.RpcProcessor.RPC_SERVICE;

/**
 *
 * @author Martin.C
 * @version 2021/12/07 6:01 PM
 */
public interface ClientProcessor {
    Logger LOG = Logger.getLogger(ClientProcessor.class);

    static void genRpcClientFactorys(ClientConfig config, BuildProducer<RpcServiceMBI> clientServiceMBIS,
                                      BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
                                      ClientRecorder recorder, CombinedIndexBuildItem indexBuildItem) throws MalformedURLException {

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


            LOG.info("=== Static Setup RpcClientFactory :"+host.url+"/"+app);
            syntheticBeanBuildItemBuildProducer.produce(configurator.done());
        }
    }


    static void genRpcClients(ClientRecorder recorder,
                               List<RpcServiceMBI> serviceMBIS,
                               BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
        for (RpcServiceMBI i : serviceMBIS) {

            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(i.getName())
                    .scope(ApplicationScoped.class)//.scope(Singleton.class)
                    .setRuntimeInit()
                    .unremovable()
                    .supplier(recorder.rpcClientSupplier( i.getName(), i.getApp()));
            configurator.defaultBean();
            configurator.addQualifier().annotation(Named.class).addValue("value", i.getApp()).done();

            LOG.info("CDI RpcService : " +i.getName() +" -> " + i.getApp());
            syntheticBeanBuildItemBuildProducer.produce(configurator.done());
        }

    }

}