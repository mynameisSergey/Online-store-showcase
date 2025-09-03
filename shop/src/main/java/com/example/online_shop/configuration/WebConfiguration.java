package com.example.online_shop.configuration;

import com.example.online_shop.model.dto.CartDto;
import com.example.online_shop.model.dto.ItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.context.annotation.SessionScope;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfiguration {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer itemCacheCustomizer() {
        return builder -> builder.withCacheConfiguration(
                "item",
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofSeconds(5))
                        .serializeValuesWith(RedisSerializationContext
                                .SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(ItemDto.class))));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer itemPictureCacheCustomizer() {
        return builder -> builder.withCacheConfiguration(
                "picture",
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofDays(1))
                        .serializeValuesWith(RedisSerializationContext
                                .SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(byte[].class))));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer itemsCacheCustomizer(Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder) {
        var om = jacksonObjectMapperBuilder.createXmlMapper(false).build();
        return builder -> builder.withCacheConfiguration(
                "items",
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofSeconds(5))
                        .serializeValuesWith(RedisSerializationContext
                                .SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(om.getTypeFactory()
                                        .constructCollectionType(List.class, ItemDto.class)))));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer itemsInCartCacheCustomizer(Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder) {
        var om = jacksonObjectMapperBuilder.createXmlMapper(false).build();
        return builder -> builder.withCacheConfiguration(
                "itemsInCart",
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofDays(30))
                        .serializeValuesWith(RedisSerializationContext
                                .SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(om.getTypeFactory()
                                        .constructCollectionType(List.class, ItemInCart.class)))));
    }

}
