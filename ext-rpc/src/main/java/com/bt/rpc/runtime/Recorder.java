package com.bt.rpc.runtime;

import java.net.MalformedURLException;
import java.util.function.Supplier;

import com.bt.rpc.client.RpcClientFactory;
import com.bt.rpc.runtime.bridge.FactoryConfig;
import io.quarkus.runtime.RuntimeValue;

@io.quarkus.runtime.annotations.Recorder
public interface Recorder {

    RuntimeValue<FactoryConfig> createManagedChannel(String urlString) throws MalformedURLException;

    Supplier<RpcClientFactory> clientFactorySupplier(RuntimeValue<FactoryConfig> factoryConfigRuntimeValue,
                                                     String appName);

    Supplier<Object> rpcClientSupplier(Class rpcService, String app);
}




