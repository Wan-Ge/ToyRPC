package com.zxcvs.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/28 19:56
 */

@Getter
@AllArgsConstructor
public class RpcFutureCallbackException extends RuntimeException {

    private String errorMsg;

    private Throwable e;

}
