package com.zxcvs.registry;

import com.zxcvs.client.ConnectManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Xiaohui Yang
 * Create at 2019/3/28 13:37
 */

@Component
@Slf4j(topic = "ToyLogger")
public class ServiceDiscovery {

    private CountDownLatch latch = new CountDownLatch(1);

    private volatile List<String> dataList = new ArrayList<>();

    @Value("${registry.address}")
    private String registryAddress;

    private ZooKeeper zooKeeper;

    public ServiceDiscovery() {
         zooKeeper = connectServer();
         if (zooKeeper != null) {
             watchNode(zooKeeper);
         }
    }

    public String discover() {
        String data = null;
        int size = dataList.size();
        if (size > 0) {
            if (size == 1) {
                data = dataList.get(0);
                log.info("using only data:{}", data);
            } else {
                data = dataList.get(ThreadLocalRandom.current().nextInt(size));
                log.info("using random data:{}", data);
            }
        }

        return data;
    }

    public ZooKeeper connectServer() {
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(registryAddress, Constants.ZK_SESSION_TIMEOUT, event -> {
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    latch.countDown();
                }
            });
            latch.await();
        } catch (IOException | InterruptedException e) {
            log.error("connect server error:{}", ExceptionUtils.getStackTrace(e));
        }

        return zk;
    }

    private void watchNode(final ZooKeeper zk) {
        try {
            List<String> nodeList = zk.getChildren(Constants.ZK_REGISTRY_PATH, event -> {
                if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    watchNode(zk);
                }
            });
            List<String> dataList = new ArrayList<>();
            for (String node : nodeList) {
                byte[] bytes = zk.getData(Constants.ZK_REGISTRY_PATH + "/" + node, false, null);
                dataList.add(new String(bytes));
            }
            log.info("service discovery triggered updating connected server node,node data:{}", dataList);
            this.dataList = dataList;
            updateConnectedServer();
        } catch (KeeperException | InterruptedException e) {
            log.error("watch node error:{}", ExceptionUtils.getStackTrace(e));
        }
    }

    private void updateConnectedServer() {
        ConnectManager.getInstance().updateConnectedServer(this.dataList);
    }

    public void stop() {
        if (zooKeeper != null) {
            try {
                zooKeeper.close();
            } catch (InterruptedException e) {
                log.error("stop server error:{}", ExceptionUtils.getStackTrace(e));
            }
        }
    }

}
