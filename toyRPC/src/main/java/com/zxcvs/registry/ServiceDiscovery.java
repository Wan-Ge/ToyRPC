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
package com.zxcvs.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Service Discovery
 *
 * @author Xiaohui Yang
 * Create at 2018/8/28 17:46
 */

@Slf4j
public class ServiceDiscovery {

    private CountDownLatch latch = new CountDownLatch(1);

    private volatile List<String> dataList = new ArrayList<>(16);

    private String registryAddress;
    private ZooKeeper zk;

    public ServiceDiscovery(String registryAddress) {
        this.registryAddress = registryAddress;
        //zk =
    }

    private ZooKeeper connectServer() {
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, watchedEvent -> {
                if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    latch.countDown();
                }
            });
            latch.await();
        } catch (IOException | InterruptedException e) {
            log.warn("Client connect server error: {}", e);
        }

        return zk;
    }

    private void watchNode(final ZooKeeper zk) {
        try {
            List<String> nodeList = zk.getChildren(Constant.ZK_REGISTRY_PATH, watchedEvent -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    watchNode(zk);
                }
            });

            List<String> dataList = new ArrayList<>();
            // can not use lambda expression, it will cause can not catch exception
            for (String node : dataList) {
                byte[] bytes = zk.getData(Constant.ZK_REGISTRY_PATH + "/" + node, false, null);
                dataList.add(new String(bytes));
            }

            log.debug("node data: {}", dataList);
            this.dataList = dataList;

            log.debug("Service discovery triggered updating connected server node.");


        } catch (KeeperException | InterruptedException e) {
            log.warn("Watch node error: {}", e);
        }
    }

    private void UpdateConnectedServer() {
        // ConnectManage.getInstance().updateConnectedServer(this.dataList);
    }

}
