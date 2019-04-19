package com.zxcvs.server;

import com.zxcvs.common.ServerThreadFactory;
import com.zxcvs.protocol.RpcDecoder;
import com.zxcvs.protocol.RpcEncoder;
import com.zxcvs.protocol.RpcRequest;
import com.zxcvs.protocol.RpcResponse;
import com.zxcvs.registry.Constants;
import com.zxcvs.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ComponentScan;
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
@ComponentScan(basePackages = "com.zxcvs.registry")
@Slf4j(topic = "ToyLogger")
public class RpcServer implements ApplicationContextAware, InitializingBean {

    private String serverAddress = Constants.SERVER_ADDRESS;

    private static String factoryName = "Server";

    @Resource
    private ServiceRegistry serviceRegistry;

    private HashMap<String, Object> handlerMap = new HashMap<>();

    private static ThreadPoolExecutor executor;

    private EventLoopGroup masterGroup = null;

    private EventLoopGroup workerGroup = null;

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("toy rpc is launching");
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
                            new ArrayBlockingQueue<>(65536), new ServerThreadFactory(factoryName));
                }
            }
        }
        executor.submit(task);
    }

    public RpcServer addService(String interfaceName, Object serviceBean) {
        if (!handlerMap.containsKey(interfaceName)) {
            log.info("Add and loading service:{}", interfaceName);
            handlerMap.put(interfaceName, serviceBean);
        }

        return this;
    }

    public void start() throws Exception {
        if (masterGroup == null && workerGroup == null) {
            masterGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(masterGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            channel.pipeline()
                                    .addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0))
                                    .addLast(new RpcDecoder(RpcRequest.class))
                                    .addLast(new RpcEncoder(RpcResponse.class))
                                    .addLast(new RpcServerHandler(handlerMap));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            String[] array = serverAddress.split(":");
            String host = array[0];
            int port = Integer.parseInt(array[1]);

            ChannelFuture future;
            try {
                future = bootstrap.bind(host, port).sync();
            } catch (Exception e) {
                log.warn("port[{}] bind failed, retry port[{}]", port, port + 1);
                port++;
                future = bootstrap.bind(host, port).sync();
            }

            log.info("host:{}, port[{}] bind success", host, port);
            // replace serverAddress
            serverAddress = host + ":" + port;

            if (serviceRegistry != null) {
                serviceRegistry.register(serverAddress);
            }

            future.channel().closeFuture();
        }
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
