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

import com.alibaba.fastjson.JSON;
import com.zxcvs.protocol.RpcRequest;
import com.zxcvs.protocol.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * RPC handler - RPC request processor
 *
 * @author Xiaohui Yang
 * Create at 2018/8/27 13:48
 */

@Slf4j
public class RpcHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private final Map<String, Object> handlerMap;

    public RpcHandler(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcRequest rpcRequest) {
        RpcServer.submit(() -> {
            log.debug("Receive request: " + rpcRequest.getRequestId());
            RpcResponse rpcResponse = new RpcResponse();
            rpcResponse.setRequestId(rpcRequest.getRequestId());
            try {
                Object result = handle(rpcRequest);
                rpcResponse.setResult(result);
            } catch (InvocationTargetException e) {
                rpcResponse.setError(e.toString());
                log.error("RPC server handle request error: {}", e);
            }

            channelHandlerContext.writeAndFlush(rpcResponse).addListener((ChannelFutureListener) future ->
                    log.debug("Send response for request: {}", rpcRequest.getRequestId()));
        });
    }

    private Object handle(RpcRequest request) throws InvocationTargetException {
        String className = request.getClassName();
        Object serviceBean = handlerMap.get(className);

        // get class and other info
        Class<?> serviceClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] paramTypes = request.getParamTypes();
        Object[] params = request.getParams();

        log.debug("ClassName: {}, methodName: {}", serviceClass.getName(), methodName);
        log.debug("params:[{}, {}]", JSON.toJSONString(paramTypes), JSON.toJSONString(params));

        // cglib reflect
        final FastClass serviceFastClass = FastClass.create(serviceClass);
        final FastMethod method = serviceFastClass.getMethod(methodName, paramTypes);
        return method.invoke(serviceBean, params);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("server caught exception: {}", cause);
        ctx.close();
    }
}
