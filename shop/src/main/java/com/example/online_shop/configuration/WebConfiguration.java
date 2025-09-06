package com.example.online_shop.configuration;

import com.example.online_shop.model.dto.CartDto;
import com.example.online_shop.model.dto.ItemDto;
import com.example.online_shop.model.entity.ItemInCart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.context.annotation.SessionScope;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfiguration {

    private final RedisConnectionFactory redisConnectionFactory;
    private final Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder;
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public RedisCacheManager cacheManager() {
        var om = jackson2ObjectMapperBuilder.createXmlMapper(false).build();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        cacheConfigs.put("item", defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(5))
                .serializeValuesWith(RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(new Jackson2JsonRedisSerializer<>(ItemDto.class))));

        cacheConfigs.put("picture", defaultCacheConfig()
                .entryTtl(Duration.ofDays(1))
                .serializeValuesWith(RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(new Jackson2JsonRedisSerializer<>(byte[].class))));

        cacheConfigs.put("items", defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(5))
                .serializeValuesWith(RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(new Jackson2JsonRedisSerializer<>(om.getTypeFactory()
                                .constructCollectionType(List.class, ItemDto.class)))));

        cacheConfigs.put("itemsInCart", defaultCacheConfig()
                .entryTtl(Duration.ofDays(30))
                .serializeValuesWith(RedisSerializationContext
                        .SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(om.getTypeFactory()
                                .constructCollectionType(List.class, ItemInCart.class)))));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultCacheConfig()).withInitialCacheConfigurations(cacheConfigs).build();
    }

    private RedisCacheConfiguration defaultCacheConfig() {
        return RedisCacheConfiguration.defaultCacheConfig().disableCachingNullValues();
    }
}
