package com.bt.rpc.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class RpcServiceMBI extends MultiBuildItem {
    private final Class name;
    private final String app;

    public RpcServiceMBI(Class name, String app) {
        this.name = name;
        this.app = app;
    }

    public Class getName() {
        return name;
    }

    public String getApp() {
        return app;
    }
}
