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

import com.zxcvs.client.HelloService;
import com.zxcvs.registry.ServiceRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Xiaohui Yang
 * Create at 2018/8/28 18:20
 */

@Slf4j
public class RpcBootstrapWithoutSpring {
    public static void main(String[] args) {
        String serverAddress = "127.0.0.1:18866";
        ServiceRegistry serviceRegistry = new ServiceRegistry("127.0.0.1:2181");
        RpcServer rpcServer = new RpcServer(serverAddress, serviceRegistry);
        HelloService helloService = new HelloServiceImpl();
        rpcServer.addService("com.zxcvs.client.HelloService", helloService);
        try {
            rpcServer.start();
        } catch (Exception ex) {
            log.error("Exception: {}", ex);
        }
    }
}
