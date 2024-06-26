/**
 * Martin.Cong
 * Copyright (c) 2021-2021 All Rights Reserved.
 */
package tech.krpc.ext.deployment;

import java.util.List;
import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;
import tech.krpc.client.RpcClientFactory;

/**
 *
 * @author Martin.C
 * @version 2021/11/18 1:43 PM
 */

public final class ClientFactoryMBI extends MultiBuildItem {

    private RuntimeValue<RpcClientFactory> factoryRuntimeValue;

    public ClientFactoryMBI(RuntimeValue<RpcClientFactory> factoryRuntimeValue) {
        this.factoryRuntimeValue = factoryRuntimeValue;
    }

    private List<Class> globalFilters;

    private Map<Class, List<Class>> filters;

    String app;

    public String getApp() {
        return app;
    }

    public RuntimeValue<RpcClientFactory> getFactoryRuntimeValue() {
        return factoryRuntimeValue;
    }
}