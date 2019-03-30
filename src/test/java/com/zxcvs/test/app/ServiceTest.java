package com.zxcvs.test.app;

import com.zxcvs.client.RpcClient;
import com.zxcvs.test.client.HelloService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/29 14:10
 */

@Slf4j(topic = "ToyLogger")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {RpcClient.class})
public class ServiceTest {

    @Test
    public void testHello1() {
        HelloService service = RpcClient.create(HelloService.class);
        String result = service.hello("World");
        log.info("{}", result);
        Assert.assertEquals("Hello! World", result);
    }
}
