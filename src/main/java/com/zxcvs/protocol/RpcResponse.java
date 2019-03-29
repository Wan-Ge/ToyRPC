package com.zxcvs.protocol;

import lombok.Data;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/25 17:11
 */

@Data
public class RpcResponse {

    private String requestId;

    private boolean error;

    private String errorMsg;

    private Throwable throwable;

    private Object result;

}
