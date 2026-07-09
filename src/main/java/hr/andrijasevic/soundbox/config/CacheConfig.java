package hr.andrijasevic.soundbox.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Redis-backed caching for external lookups. Cache failures (e.g. Redis unreachable)
 * are logged and ignored rather than propagated, so a Redis outage degrades to
 * "no cache" instead of breaking requests.
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    public static final String ALBUM_SEARCH_CACHE = "albumSearch";

    @Bean
    public RedisCacheManagerBuilderCustomizer cacheCustomizer() {
        // JSON value serialization so DTO records round-trip (they aren't java.io.Serializable).
        // WRAPPER_ARRAY typing tags every value — including the root List — so generic
        // collections deserialize back to the right type.
        RedisCacheConfiguration jsonConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(cacheObjectMapper())))
                .disableCachingNullValues();
        return builder -> builder
                .cacheDefaults(jsonConfig)
                .withCacheConfiguration(ALBUM_SEARCH_CACHE, jsonConfig.entryTtl(Duration.ofHours(1)));
    }

    private ObjectMapper cacheObjectMapper() {
        var typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("hr.andrijasevic.soundbox.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.lang.")
                .build();
        return JsonMapper.builder()
                .activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.EVERYTHING,
                        JsonTypeInfo.As.WRAPPER_ARRAY)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache get failed on '{}' (key={}), proceeding without cache: {}",
                        cache.getName(), key, e.toString());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Cache put failed on '{}' (key={}): {}", cache.getName(), key, e.toString());
            }
        };
    }
}
