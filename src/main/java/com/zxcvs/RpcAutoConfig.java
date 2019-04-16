package com.zxcvs;

import com.zxcvs.client.RpcClient;
import com.zxcvs.registry.ServiceDiscovery;
import com.zxcvs.registry.ServiceRegistry;
import com.zxcvs.server.RpcServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Xiaohui Yang
 * Create at 2019/4/16 18:31
 */

@Configuration
public class RpcAutoConfig {

    @Bean
    public RpcServer serverConfig() {
        return new RpcServer();
    }

    @Bean
    public RpcClient clientConfig() {
        return new RpcClient();
    }

    @Bean
    public ServiceDiscovery sdConfig() {
        return new ServiceDiscovery();
    }

    @Bean
    public ServiceRegistry srConfig() {
        return new ServiceRegistry();
    }
}
