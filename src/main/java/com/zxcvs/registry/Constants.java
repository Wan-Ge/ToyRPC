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
}
