package com.zxcvs.client;

import com.zxcvs.common.ServerThreadFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/28 13:25
 */

@Slf4j(topic = "ToyLogger")
@NoArgsConstructor
public class ConnectManager {

    @Value("${connectManager.factory.name}")
    private static String factoryName;

    private volatile static ConnectManager connectManager;

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(16, 16, 600L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(65535), new ServerThreadFactory(factoryName));

    private CopyOnWriteArrayList<RpcClientHandler> connectedHandlers = new CopyOnWriteArrayList<>();

    private Map<InetSocketAddress, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();

    private ReentrantLock lock = new ReentrantLock();

    private Condition connected = lock.newCondition();

    private long connectTimeoutMillis = 6000;

    private AtomicInteger roundRobin = new AtomicInteger(0);

    private volatile boolean isRunning = true;


    /** 饱汉 double check 单例 */
    public static ConnectManager getInstance() {
        if (connectManager == null) {
            synchronized (ConnectManager.class) {
                if (connectManager == null) {
                    connectManager = new ConnectManager();
                }
            }
        }

        return connectManager;
    }

    public void updateConnectedServer(List<String> allServerAddress) {
        if (CollectionUtils.isNotEmpty(allServerAddress)) {
            // update local server nodes cache
            HashSet<InetSocketAddress> newAllServerNodeSet = new HashSet<>();
            for (String serverAddress : allServerAddress) {
                String[] array = serverAddress.split(":");
                // should check ip and port
                if (array.length == 2) {
                    String host = array[0];
                    int port = Integer.parseInt(array[1]);
                    final InetSocketAddress remotePeer = new InetSocketAddress(host, port);
                    newAllServerNodeSet.add(remotePeer);
                }
            }

            // add new server node
            for (final InetSocketAddress serverNodeAddress : newAllServerNodeSet) {
                if (!connectedServerNodes.keySet().contains(serverNodeAddress)) {
                    connectServerNode(serverNodeAddress);
                }
            }

            // close and remove invalid server nodes
            for (RpcClientHandler connectedServerHandler : connectedHandlers) {
                SocketAddress remotePeer = connectedServerHandler.getRemotePeer();
                if (!newAllServerNodeSet.contains(remotePeer)) {
                    log.info("remove invalid server node:{}", remotePeer);
                    RpcClientHandler handler = connectedServerNodes.get(remotePeer);
                    if (handler != null) {
                        handler.close();
                    }
                    connectedServerNodes.remove(remotePeer);
                    connectedHandlers.remove(connectedServerHandler);
                }
            }
        } else {
            log.error("No available server node, All server nodes are down.");
            for (final RpcClientHandler connectedServerHandler : connectedHandlers) {
                SocketAddress remotePeer = connectedServerHandler.getRemotePeer();
                RpcClientHandler handler = connectedServerNodes.get(remotePeer);
                handler.close();
                connectedServerNodes.remove(connectedServerHandler);
            }
            connectedHandlers.clear();
        }
    }

    public void reconnect(final RpcClientHandler handler, final SocketAddress remotePeer) {
        if (handler != null) {
            connectedHandlers.remove(handler);
            connectedServerNodes.remove(handler.getRemotePeer());
        }
        connectServerNode((InetSocketAddress) remotePeer);
    }

    private void connectServerNode(final InetSocketAddress remotePeer) {
        executor.submit(() -> {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class).handler(new RpcClientInitializer());
            ChannelFuture channelFuture = bootstrap.connect(remotePeer);
            channelFuture.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("successfully connect to remote server, remote peer:{}", remotePeer);
                    RpcClientHandler handler = future.channel().pipeline().get(RpcClientHandler.class);
                    addHandler(handler);
                }
            });
        });
    }

    private void addHandler(RpcClientHandler handler) {
        connectedHandlers.add(handler);
        InetSocketAddress remoteAddress = (InetSocketAddress) handler.getChannel().remoteAddress();
        connectedServerNodes.put(remoteAddress, handler);
        signalAvailableHandler();
    }

    private void signalAvailableHandler() {
        lock.lock();
        try {
            connected.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private boolean waitingForHandler() throws InterruptedException {
        lock.lock();
        try {
            return connected.await(connectTimeoutMillis, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }

    public RpcClientHandler chooseHandler() {
        int size = connectedHandlers.size();
        while (isRunning && size <= 0) {
            try {
                boolean available = waitingForHandler();
                if (available) {
                    size = connectedHandlers.size();
                }
            } catch (InterruptedException e) {
                log.error("waiting for available node is interrupted.{}", ExceptionUtils.getStackTrace(e));
                throw new RuntimeException("can't connect any servers.", e);
            }
        }
        int index = (roundRobin.getAndAdd(1) + size) % size;
        return connectedHandlers.get(index);
    }

    public void stop() {
        isRunning = false;
        connectedHandlers.forEach(RpcClientHandler::close);
        signalAvailableHandler();
        executor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }
}
