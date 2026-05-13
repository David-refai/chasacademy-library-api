package com.library.api.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;

// Configures Redis as the cache backend
// Redis is a fast in-memory store used to cache frequently accessed data,
// which avoids repeated database queries and significantly reduces response times
@Configuration
public class RedisConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Store cached objects as JSON (human-readable and language-agnostic)
        var jsonSerializer = new GenericJackson2JsonRedisSerializer();

        // Default configuration applied to all caches unless overridden
        var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))    // Cache entries expire after 10 minutes
                .disableCachingNullValues()           // Never cache null values
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(jsonSerializer)
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                // Books cache lives 30 minutes — book data rarely changes
                .withCacheConfiguration("books",
                        defaultConfig.entryTtl(Duration.ofMinutes(30)))
                // Loans cache lives 5 minutes — loan state changes more frequently
                .withCacheConfiguration("loans",
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .build();
    }
}
