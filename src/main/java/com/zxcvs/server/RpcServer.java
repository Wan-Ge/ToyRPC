package com.zxcvs.server;

import com.zxcvs.common.ServerThreadFactory;
import com.zxcvs.registry.ServiceRegistry;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/24 21:31
 */

@Component
@Slf4j(topic = "ServerLogger")
public class RpcServer implements ApplicationContextAware, InitializingBean {

    private String serverAddress;

    private ServiceRegistry serviceRegistry;

    private HashMap<String, Object> handlerMap = new HashMap<>();

    private static ThreadPoolExecutor executor;

    @Resource
    private static ServerThreadFactory threadFactory;

    private EventLoopGroup masterGroup = null;

    private EventLoopGroup workerGroup = null;

    public RpcServer(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public RpcServer(String serverAddress, ServiceRegistry serviceRegistry) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        start();
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        Map<String, Object> serviceBeanMap = context.getBeansWithAnnotation(RpcService.class);
        if (MapUtils.isNotEmpty(serviceBeanMap)) {
            serviceBeanMap.values().forEach(service -> {
                String interfaceName = service.getClass().getAnnotation(RpcService.class).value().getName();
                log.info("Loading service:{}", interfaceName);
                handlerMap.put(interfaceName, service);
            });
        }
    }

    public static void submit(Runnable task) {
        if (executor == null) {
            synchronized (RpcService.class) {
                if (executor == null) {
                    executor = new ThreadPoolExecutor(16, 16, 600L, TimeUnit.SECONDS,
                            new ArrayBlockingQueue<>(65536), threadFactory);
                }
            }
        }
        executor.submit(task);
    }

    public RpcServer addService(String interfaceName, Object serviceBean) {
        if (!handlerMap.containsKey(interfaceName)) {
            log.info("Loading service:{}", interfaceName);
            handlerMap.put(interfaceName, serviceBean);
        }

        return this;
    }

    public void start() throws Exception {

    }

    public void stop() {
        if (masterGroup != null) {
            masterGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}
