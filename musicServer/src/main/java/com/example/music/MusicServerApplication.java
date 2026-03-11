package com.example.music;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.boot.CommandLineRunner;

@EnableCaching   // 开启Spring Boot基于注解的缓存管理支持
@SpringBootApplication
public class MusicServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MusicServerApplication.class, args);
    }

    @Bean  // 定义一个Bean，在应用启动时执行清除Redis缓存的操作
    public CommandLineRunner clearRedisCache(RedisConnectionFactory connectionFactory) {
        return args -> {
            System.out.println("Clearing Redis cache...");
            connectionFactory.getConnection().flushDb();
            System.out.println("Redis cache cleared.");
        };
    }

}
