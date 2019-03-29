package com.zxcvs.test.app;

import com.zxcvs.client.RpcClient;
import com.zxcvs.test.client.HelloService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/29 14:10
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ComponentScan(basePackages = "com.zxcvs.*")
@PropertySource({"classpath:rpc.properties"})
public class ServiceTest {

    @Resource
    private HelloService helloService;

    @Resource
    private RpcClient rpcClient;

    @Test
    public void testHello1() {
        HelloService service = rpcClient.create(HelloService.class);
        String result = service.hello("World");
        Assert.assertEquals("Hello! World", result);
    }
}
