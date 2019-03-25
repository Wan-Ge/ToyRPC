package com.zxcvs.test;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/24 16:23
 */

@Component
@ComponentScan(basePackages = "com.zxcvs.*")
@PropertySource({"classpath:rpc.properties"})
public class RpcBootStrap {
    public static void main(String[] args) {
        SpringApplication.run(RpcBootStrap.class);
    }
}
