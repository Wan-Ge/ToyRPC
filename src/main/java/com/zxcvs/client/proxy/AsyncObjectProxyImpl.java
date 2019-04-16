package com.zxcvs.client.proxy;

import com.zxcvs.client.ConnectManager;
import com.zxcvs.client.RpcClientHandler;
import com.zxcvs.client.RpcFuture;
import com.zxcvs.protocol.RpcRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/28 13:08
 */

@Slf4j(topic = "ToyLogger")
@AllArgsConstructor
public class AsyncObjectProxyImpl<T> implements InvocationHandler, AsyncObjectProxy {

    private static final String EQUALS = "equals";

    private static final String HASH_CODE = "hashCode";

    private static final String TO_STRING = "toString";

    private Class<T> clazz;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class == method.getDeclaringClass()) {
            String name = method.getName();
            if (EQUALS.equals(name)) {
                return proxy == args[0];
            } else if (HASH_CODE.equals(name)) {
                return System.identityHashCode(proxy);
            } else if (TO_STRING.equals(name)) {
                return proxy.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(proxy)) + ", " +
                        "with InvocationHandler " + this;
            } else {
                throw new IllegalStateException(String.valueOf(method));
            }
        }

        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);

        log.info("send request:{}", request.getRequestId());
        log.info("className:{},methodName:{},parameterTypes:{},args:{}", method.getDeclaringClass().getName(),
                method.getName(), method.getParameterTypes(), args);

        RpcClientHandler handler = ConnectManager.getInstance().chooseHandler();
        RpcFuture rpcFuture = handler.sendRequest(request);

        return rpcFuture.get();
    }

    @Override
    public RpcFuture call(String methodName, Object... args) {
        RpcClientHandler handler = ConnectManager.getInstance().chooseHandler();
        RpcRequest request = createRequest(clazz.getName(), methodName, args);

        return handler.sendRequest(request);
    }

    private RpcRequest createRequest(String className, String methodName, Object[] args) {
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(className);
        request.setMethodName(methodName);
        request.setParameters(args);

        Class[] parameterTypes = new Class[args.length];
        // Get the right class type
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = getClassType(args[i]);
        }
        request.setParameterTypes(parameterTypes);

        log.info("className:{},methodName:{},parameterTypes:{},args:{}", className, methodName, parameterTypes, args);

        return request;
    }

    private Class<?> getClassType(Object obj) {
        Class<?> classType = obj.getClass();
        String typeName = classType.getName();
        switch (typeName) {
            case "java.lang.Integer":
                return Integer.TYPE;
            case "java.lang.Long":
                return Long.TYPE;
            case "java.lang.Float":
                return Float.TYPE;
            case "java.lang.Double":
                return Double.TYPE;
            case "java.lang.Character":
                return Character.TYPE;
            case "java.lang.Boolean":
                return Boolean.TYPE;
            case "java.lang.Short":
                return Short.TYPE;
            case "java.lang.Byte":
                return Byte.TYPE;
            default:
                throw new RuntimeException("No this type");
        }
    }
}
