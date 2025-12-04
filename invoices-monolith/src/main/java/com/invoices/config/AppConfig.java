package com.invoices.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.Executor;

@Configuration
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
@Slf4j
public class AppConfig {

    private ThreadPoolTaskExecutor executor;

    @Value("${async.executor.core-pool-size:10}")
    private int corePoolSize;

    @Value("${async.executor.max-pool-size:20}")
    private int maxPoolSize;

    @Value("${async.executor.queue-capacity:100}")
    private int queueCapacity;

    /**
     * Configure explicit executor for @Async methods.
     * Prevents unbounded thread creation with default SimpleAsyncTaskExecutor.
     *
     * @return configured ThreadPoolTaskExecutor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Proper shutdown of the task executor to prevent thread leaks.
     */
    @PreDestroy
    public void shutdown() {
        if (executor != null) {
            log.info("Shutting down TaskExecutor...");
            executor.shutdown();
            log.info("TaskExecutor shutdown complete");
        }
    }
}
