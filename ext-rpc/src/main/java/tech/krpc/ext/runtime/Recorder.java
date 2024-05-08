package tech.krpc.ext.runtime;

import java.net.MalformedURLException;
import java.util.function.Supplier;

import io.quarkus.runtime.RuntimeValue;
import tech.krpc.client.RpcClientFactory;
import tech.krpc.ext.runtime.bridge.FactoryConfig;

@io.quarkus.runtime.annotations.Recorder
public interface Recorder {

    RuntimeValue<FactoryConfig> createManagedChannel(String urlString) throws MalformedURLException;

    Supplier<RpcClientFactory> clientFactorySupplier(RuntimeValue<FactoryConfig> factoryConfigRuntimeValue,
                                                     String appName);

    Supplier<Object> rpcClientSupplier(Class rpcService, String app);
}




