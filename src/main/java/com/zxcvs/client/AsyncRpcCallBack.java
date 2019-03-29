package com.zxcvs.client;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/28 13:03
 */

public interface AsyncRpcCallBack {

    void success(Object result);

    void fail(Exception e);

}
