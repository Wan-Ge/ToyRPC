package com.zxcvs.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/30 16:02
 */

@Getter
@AllArgsConstructor
public class RpcNoSuchMethodException extends RuntimeException {

    private String requestId;

    private String classFullName;

}
