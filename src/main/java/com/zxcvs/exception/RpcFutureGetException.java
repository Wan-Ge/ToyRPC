package com.zxcvs.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/28 19:37
 */

@Getter
@ToString
@AllArgsConstructor
public class RpcFutureGetException extends RuntimeException {

    private String requestId;

    private String className;

    private String methodName;

}
