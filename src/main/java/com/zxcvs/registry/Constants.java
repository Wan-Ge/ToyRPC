package com.zxcvs.registry;

/**
 * ZooKeeper Constants
 *
 * @author Xiaohui Yang
 * Create at 2019/3/24 16:58
 */

public class Constants {

    /** ZK session 超时时间 */
    public static final int ZK_SESSION_TIMEOUT = 5000;

    /** 注册中心地址*/
    public static final String ZK_REGISTRY_PATH = "/registry";

    /** 注册中心数据地址*/
    public static final String ZK_DATA_PATH = ZK_REGISTRY_PATH + "/data";

    /** ZK 注册地址 */
    public static final String ZK_REGISTRY_ADDRESS = "127.0.0.1:2181";

    public static final String SERVER_ADDRESS = "127.0.0.1:18866";


    /*
    * # zookeeper server
toyrpc.registry.address=127.0.0.1:2181

# rpc server
toyrpc.server.address=127.0.0.1:18866

# thread pool factory names
toyrpc.server.factory.name=Server
toyrpc.connectManager.factory.name=ConnectManager
toyrpc.client.factory.name=Client
    *
    * */
}
