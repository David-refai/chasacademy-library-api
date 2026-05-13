package com.library.api.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;

// إعداد Redis كمستودع للـ cache
// Redis = مخزن بيانات سريع جداً في الذاكرة يُستخدم لتخزين نتائج الاستعلامات الثقيلة
@Configuration
public class RedisConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // نُخزّن البيانات في Redis بصيغة JSON (يمكن قراءتها بسهولة)
        var jsonSerializer = new GenericJackson2JsonRedisSerializer();

        // الإعداد الافتراضي لكل الـ caches
        var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))    // الـ cache ينتهي بعد 10 دقائق
                .disableCachingNullValues()           // لا نُخزّن null لأنه لا فائدة منه
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(jsonSerializer)
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                // cache الكتب يعيش 30 دقيقة لأن بياناتها لا تتغير كثيراً
                .withCacheConfiguration("books",
                        defaultConfig.entryTtl(Duration.ofMinutes(30)))
                // cache الاستعارات يعيش 5 دقائق فقط لأن حالتها تتغير أكثر
                .withCacheConfiguration("loans",
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .build();
    }
}
