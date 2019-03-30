package com.zxcvs.client;

import com.zxcvs.client.proxy.AsyncObjectProxy;
import com.zxcvs.client.proxy.AsyncObjectProxyImpl;
import com.zxcvs.common.ServerThreadFactory;
import com.zxcvs.registry.ServiceDiscovery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Proxy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/28 13:25
 */

@Component
@ComponentScan(basePackages = "com.zxcvs")
public class RpcClient {

    @Value("${client.factory.name}")
    private static String factoryName;

    @Value("${server.address}")
    private String serverAddress;

    @Resource
    private ServiceDiscovery serviceDiscovery;

    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(16, 16, 600L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(65536), new ServerThreadFactory(factoryName));

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> interfaceClass) {
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass},
                new AsyncObjectProxyImpl<>(interfaceClass));
    }

    public static <T>AsyncObjectProxy createAsync(Class<T> interfaceClass) {
        return new AsyncObjectProxyImpl<>(interfaceClass);
    }

    public static void submit(Runnable task) {
        executor.submit(task);
    }

    public void  stop() {
        executor.shutdown();
        serviceDiscovery.stop();
        ConnectManager.getInstance().stop();
    }
}
