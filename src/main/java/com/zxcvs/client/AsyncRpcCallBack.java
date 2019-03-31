package com.zxcvs.client;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/28 13:03
 */

public interface AsyncRpcCallBack {

    /**
     * success to get result of invoke
     *
     * @param result invoke result
     */
    void success(Object result);

    /**
     * failed to invoke method
     * @param e cause
     */
    void fail(Exception e);

}
