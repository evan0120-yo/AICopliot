package com.citrus.rewardbridge.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(RewardBridgeProperties.class)
public class RewardBridgeConfiguration {

    @Bean(destroyMethod = "close")
    public ExecutorService rewardBridgeExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
