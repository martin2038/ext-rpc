/**
 * Botaoyx.com Inc.
 * Copyright (c) 2021-2021 All Rights Reserved.
 */
package com.bt.rpc.runtime;

import java.util.Set;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 *
 * @author Martin.C
 * @version 2021/11/18 10:52 AM
 */
@ConfigGroup
public class ServerApp {

    /**
     * 服务器地址,如 https://backoffice-api.botaoyx.com
     */
    @ConfigItem
    public String url;

    /**
     * 默认序列化方式 SerialEnum JSON,HESSIAN,KRYO
     */
    @ConfigItem(defaultValue = "JSON")
    public String serial;



    /**
     * 服务扫描的package，多个英文逗号隔开,如 com.btyx.admin.auth
     */
    @ConfigItem
    public Set<String> scan;

    @Override
    public String toString() {
        return "ServerApp{" +
                "url='" + url + '\'' +
                ", scan='" + scan + '\'' +
                '}';
    }


    public boolean isMatch(String rpcClass){
        for(var s : scan){
            if(rpcClass.startsWith(s)){
                return true;
            }
        }
        return false;
    }

    ///**
    // * MyBatis environment id
    // */
    //@ConfigItem
    //public Optional<String> environment;
    //
    //
    ///**
    // * MyBatis initial sql
    // */
    //@ConfigItem(name = "initial-sql")
    //public Optional<String> initialSql;
}