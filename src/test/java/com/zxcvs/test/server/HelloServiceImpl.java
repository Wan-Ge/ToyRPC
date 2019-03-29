package com.zxcvs.test.server;

import com.zxcvs.server.RpcService;
import com.zxcvs.test.client.HelloService;
import com.zxcvs.test.client.Person;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/29 14:08
 */

@RpcService(HelloService.class)
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String name) {
        return "Hello! " + name;
    }

    @Override
    public String hello(Person person) {
        return "Hello! " + person.getFirstName() + " " + person.getLastName();
    }
}
