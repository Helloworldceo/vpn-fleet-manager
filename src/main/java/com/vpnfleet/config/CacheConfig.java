package com.vpnfleet.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Cached values (DTO records) are serialized as JSON rather than via the
     * JDK default, which requires every cached type to implement
     * {@link java.io.Serializable} -- records used purely as response bodies
     * shouldn't need to care about that.
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        RedisCacheConfiguration jsonValues = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return builder -> builder
                .cacheDefaults(jsonValues)
                .withCacheConfiguration("nodes", jsonValues.entryTtl(Duration.ofSeconds(15)));
    }
}
