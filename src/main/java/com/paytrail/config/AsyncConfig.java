package com.paytrail.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "eventExecutor")
    public Executor eventExecutor() {
        return Executors.newFixedThreadPool(10);
    }
}
