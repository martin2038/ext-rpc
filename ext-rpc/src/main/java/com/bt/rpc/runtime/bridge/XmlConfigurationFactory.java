package com.bt.rpc.runtime.bridge;

public class XmlConfigurationFactory implements RPcClientFactory {



    private String mybatisConfigFile;

    public XmlConfigurationFactory(){

    }

    public XmlConfigurationFactory(String mybatisConfigFile){
        this.mybatisConfigFile = mybatisConfigFile;
    }


    @Override
    public Object createConfiguration() {
        //TODO
       return null;
    }

    public String getMybatisConfigFile() {
        return mybatisConfigFile;
    }

    public void setMybatisConfigFile(String mybatisConfigFile) {
        this.mybatisConfigFile = mybatisConfigFile;
    }

    @Override
    public String toString() {
        return "XmlConfigurationFactory{" +
                "mybatisConfigFile='" + mybatisConfigFile + '\'' +
                '}';
    }
}
