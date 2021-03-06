package com.zxcvs.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * 服务注册
 *
 * @author Xiaohui Yang
 * Create at 2019/3/24 17:31
 */

@Component
@Slf4j(topic = "ToyLogger")
public class ServiceRegistry {

    private CountDownLatch latch = new CountDownLatch(1);

    private String registryAddress = Constants.ZK_REGISTRY_ADDRESS;

    /** 服务注册 */
    public void register(String data) {
        if (StringUtils.isNotEmpty(data)) {
            ZooKeeper zk = connectServer();
            if (zk != null) {
                // add root node if not exist
                addRootNode(zk);
                createNode(zk, data);
            }
        }
    }

    private ZooKeeper connectServer() {
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(registryAddress, Constants.ZK_SESSION_TIMEOUT, event -> {
                if (event.getState() == KeeperState.SyncConnected) {
                    latch.countDown();
                }
            });
            latch.await();
        } catch (IOException | InterruptedException e) {
            log.error(ExceptionUtils.getStackTrace(e));
        }

        return zk;
    }

    /** 增加根节点 */
    private void addRootNode(ZooKeeper zk) {
        try {
            Stat stat = zk.exists(Constants.ZK_REGISTRY_PATH, false);
            if (stat == null) {
                zk.create(Constants.ZK_REGISTRY_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException | InterruptedException e) {
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    /** 创建 zk 节点 */
    private void createNode(ZooKeeper zk, String data) {
        try {
            byte[] bytes = data.getBytes();
            String path = zk.create(Constants.ZK_DATA_PATH, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            log.info("create zookeeper node: {} => {}", path, data);
        } catch (KeeperException | InterruptedException e) {
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }
}
