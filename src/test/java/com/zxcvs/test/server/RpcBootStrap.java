package com.zxcvs.test.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/24 16:23
 */

@SpringBootApplication(scanBasePackages = {"com.zxcvs"})
public class RpcBootStrap {
    public static void main(String[] args) {
        SpringApplication.run(RpcBootStrap.class);
    }
}
