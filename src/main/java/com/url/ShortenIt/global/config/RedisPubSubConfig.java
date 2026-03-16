package com.url.ShortenIt.global.config;

import com.url.ShortenIt.domain.url.service.CacheInvalidationSubscriber;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisPubSubConfig {

    public static final String CACHE_INVALIDATION_TOPIC = "cache:invalidation";
    public static final String LISTENER_METHOD = "onMessage";

    @Bean
    public ChannelTopic cacheInvalidationTopic() {
        return new ChannelTopic(CACHE_INVALIDATION_TOPIC);
    }

    @Bean
    public MessageListenerAdapter messageListenerAdapter(CacheInvalidationSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, LISTENER_METHOD);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter messageListenerAdapter,
            ChannelTopic cacheInvalidationTopic) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(messageListenerAdapter, cacheInvalidationTopic);
        return container;
    }
}
