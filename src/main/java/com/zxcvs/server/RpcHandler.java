package com.zxcvs.server;

import com.zxcvs.protocol.RpcRequest;
import com.zxcvs.protocol.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.cglib.reflect.FastClass;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/25 17:14
 */

@Slf4j(topic = "ToyLogger")
public class RpcHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private final Map<String, Object> handlerMap;

    public RpcHandler(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, RpcRequest request) throws Exception {
        RpcServer.submit(() -> {
            log.info("Receive request " + request.getRequestId());
            RpcResponse response = new RpcResponse();
            response.setRequestId(request.getRequestId());
            try {
                Object result = handle(request);
                response.setResult(result);
            } catch (Exception e) {
                response.setError(false);
                response.setErrorMsg("Handler Error");
                response.setThrowable(e);
                log.info("RPC server handle request error!{}", ExceptionUtils.getStackTrace(e));
            }
            context.writeAndFlush(response).addListener((ChannelFutureListener) channelFuture -> log.info("send " +
                    "response for request:{}", request.getRequestId()));
        });
    }

    private Object handle(RpcRequest request) throws Exception {
        String className = request.getClassName();
        Object serviceBean = handlerMap.get(className);

        Class<?> serviceClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();

        log.info("className:{}", serviceClass.getName());
        log.info("methodName:{}", methodName);
        log.info("parameterTypes:{}", Arrays.stream(parameterTypes).map(Class::getName).collect(Collectors.toList()));
        log.info("parameters:{}", parameters);

        FastClass serviceFastClass = FastClass.create(serviceClass);
        int methodIndex = serviceFastClass.getIndex(methodName, parameterTypes);
        return serviceFastClass.invoke(methodIndex, serviceBean, parameters);
    }
}
