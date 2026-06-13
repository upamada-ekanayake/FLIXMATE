package com.flixmate.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration.class
})
@EnableScheduling
@EnableTransactionManagement
public class FlixMateApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlixMateApplication.class, args);
    }
}
