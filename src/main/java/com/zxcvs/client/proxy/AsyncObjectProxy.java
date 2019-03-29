package com.zxcvs.client.proxy;

import com.zxcvs.client.RpcFuture;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/28 13:04
 */

public interface AsyncObjectProxy {

    /**
     * 异步调用方法
     *
     * @param methodName 方法名
     * @param args       参数列表
     * @return 执行结果
     */
    RpcFuture call(String methodName, Object... args);

}
