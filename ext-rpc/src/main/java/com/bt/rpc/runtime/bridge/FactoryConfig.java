package com.bt.rpc.runtime.bridge;

import java.util.List;

public interface FactoryConfig {

    String getHost();

    int getPort();

    boolean isTls();


    List<Class> getGlobalFilters();



}
