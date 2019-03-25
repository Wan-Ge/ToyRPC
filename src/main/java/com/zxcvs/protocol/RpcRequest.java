package com.zxcvs.protocol;

import lombok.Data;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/25 17:08
 */

@Data
public class RpcRequest {

    private String requestId;

    private String className;

    private String methodName;

    private Class<?>[] parameterTypes;

    private Object[] parameters;

}
