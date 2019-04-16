package com.zxcvs.client;

import com.zxcvs.protocol.RpcRequest;
import com.zxcvs.protocol.RpcResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/28 13:28
 */

@Slf4j(topic = "ToyLogger")
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private ConcurrentHashMap<String, RpcFuture> pendingRpc = new ConcurrentHashMap<>();

    @Getter
    private volatile Channel channel;

    @Getter
    private SocketAddress remotePeer;

    @Override
    public void channelActive(ChannelHandlerContext context) throws Exception {
        super.channelActive(context);
        remotePeer = channel.remoteAddress();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext context) throws Exception {
        super.channelRegistered(context);
        channel = context.channel();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
        log.error("client caught exception:{}", ExceptionUtils.getStackTrace(cause));
        context.close();
    }

    /** 接收来自 Server 的响应 */
    @Override
    protected void channelRead0(ChannelHandlerContext context, RpcResponse response) throws Exception {
        String requestId = response.getRequestId();
        RpcFuture future = pendingRpc.get(requestId);
        if (future != null) {
            pendingRpc.remove(requestId);
            future.done(response);
        }
    }

    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    public RpcFuture sendRequest(RpcRequest request) {
        final CountDownLatch latch = new CountDownLatch(1);
        RpcFuture future = new RpcFuture(request);
        pendingRpc.put(request.getRequestId(), future);
        channel.writeAndFlush(request).addListener((ChannelFutureListener) channelFuture -> latch.countDown());
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("send request error:{}", ExceptionUtils.getStackTrace(e));
        }

        return future;
    }
}
