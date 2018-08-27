/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zxcvs.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zxcvs.protocol.RpcDecoder;
import com.zxcvs.protocol.RpcEncoder;
import com.zxcvs.protocol.RpcRequest;
import com.zxcvs.protocol.RpcResponse;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * RPC Server
 *
 * @author Xiaohui Yang
 * Create at 2018/8/27 14:15
 */

@Slf4j
public class RpcServer implements ApplicationContextAware, InitializingBean {

    /** server address */
    private String serverAddress;

    /** service registry class */
    private ServiceRegistry serviceRegistry;


    private Map<String, Object> handlerMap = new HashMap<>(16);

    private static ThreadPoolExecutor threadPoolExecutor;

    /** leader and workers event loop group */
    private EventLoopGroup leaderGroup = null;
    private EventLoopGroup workersGroup = null;

    public RpcServer(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public RpcServer(String serverAddress, ServiceRegistry serviceRegistry) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        Map<String, Object> serviceBeanMap = ctx.getBeansWithAnnotation(ToyRpcService.class);
        if (MapUtils.isEmpty(serviceBeanMap)) {
            serviceBeanMap.values().forEach(serviceBean -> {
                String interfaceName = serviceBean.getClass().getAnnotation(ToyRpcService.class).value().getName();
                log.info("Loading service: {}", interfaceName);
                handlerMap.put(interfaceName, serviceBean);
            });
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        start();
    }

    private void start() throws Exception {
        if (leaderGroup == null && workersGroup == null) {
            leaderGroup = new NioEventLoopGroup();
            workersGroup = new NioEventLoopGroup();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(leaderGroup, workersGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline()
                                    .addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0))
                                    .addLast(new RpcDecoder(RpcRequest.class))
                                    .addLast(new RpcEncoder(RpcResponse.class))
                                    .addLast(new RpcHandler(handlerMap));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            String[] res = serverAddress.split(":");
            String host = res[0];
            int port = Integer.parseInt(res[1]);

            ChannelFuture future;
            try {
                future = bootstrap.bind(host, port).sync();
            } catch (InterruptedException e) {
                log.error("start server failed: {}", e);
                throw e;
            }
            log.info("Server start on port: {}", port);

            if (serviceRegistry != null) {
                serviceRegistry.registry(serverAddress);
            }

            future.channel().closeFuture().sync();
        }
    }

    public RpcServer addService(String interfaceName, Object serviceBean) {
        if (!handlerMap.containsKey(interfaceName)) {
            log.info("Loading service: {}", interfaceName);
            handlerMap.put(interfaceName, serviceBean);
        }

        return this;
    }

    public static void submit(Runnable task) {
        if (threadPoolExecutor == null) {
            synchronized (RpcServer.class) {
                if (threadPoolExecutor == null) {
                    // named factory
                    ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("server-pool-%d").build();
                    threadPoolExecutor = new ThreadPoolExecutor(16, 16, 600L,
                            TimeUnit.SECONDS, new ArrayBlockingQueue<>(65536), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
                }
            }
        }

        threadPoolExecutor.submit(task);
    }

    public void stop() {
        if (leaderGroup != null) {
            leaderGroup.shutdownGracefully();
        }
        if (workersGroup != null) {
            workersGroup.shutdownGracefully();
        }
    }
}
