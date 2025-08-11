package com.example.online_shop.logger;

import lombok.Data;
import org.slf4j.event.Level;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "online-shop.http.logging")
public class RequestLoggerProperties {
    private Level level = Level.INFO;
    private boolean enabled = true;
}
