package com.example.online_shop.logger;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.server.WebFilter;

@AutoConfiguration
@EnableConfigurationProperties(RequestLoggerProperties.class)
@ConditionalOnProperty(
        prefix = "online-shop.http.logging",
        value = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class RequestLoggerAutoConfiguration {
    @Bean
    public WebFilter httpLogger(RequestLoggerProperties properties) {
        return new RequestLoggerWebFilter(properties.getLevel(), properties.isEnabled());
    }
}
