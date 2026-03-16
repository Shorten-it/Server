package com.url.ShortenIt.urlservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisPubSubConfig {
    // url-service는 Publisher 역할만 하므로 별도 Listener 설정 불필요
}
