package com.zxcvs.client;

import com.zxcvs.common.ServerThreadFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;

import java.net.InetSocketAddress;
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
            for (RpcClientHandler handler : connectedHandlers) {

            }
        }
    }

    private void connectServerNode(InetSocketAddress serverNodeAddress) {

    }

    public void stop() {

    }
}
