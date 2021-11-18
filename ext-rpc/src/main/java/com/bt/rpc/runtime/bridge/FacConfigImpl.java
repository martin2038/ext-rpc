/**
 * Botaoyx.com Inc.
 * Copyright (c) 2021-2021 All Rights Reserved.
 */
package com.bt.rpc.runtime.bridge;

import java.util.List;

/**
 *
 * @author Martin.C
 * @version 2021/11/18 7:17 PM
 */
public class FacConfigImpl implements FactoryConfig{


    String host;

    int port;

    boolean tls;


    List<Class> globalFilters;

    public FacConfigImpl(String host, int port, boolean tls) {
        this.host = host;
        this.port = port;
        this.tls = tls;
    }


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isTls() {
        return tls;
    }

    public void setTls(boolean tls) {
        this.tls = tls;
    }

    public List<Class> getGlobalFilters() {
        return globalFilters;
    }

    public void setGlobalFilters(List<Class> globalFilters) {
        this.globalFilters = globalFilters;
    }
}