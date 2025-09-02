package com.example.online_shop.logger;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
public class RequestLoggerWebFilter implements WebFilter {

    private final Level level;
    private final boolean enabled;

    public RequestLoggerWebFilter(Level level, boolean enabled) {
        this.level = level;
        this.enabled = enabled;
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (enabled &&!exchange.getRequest().getURI().getPath().contains("image")) {
            log.atLevel(level).log("Получен {} запрос {}", exchange.getRequest().getMethod(), exchange.getRequest().getURI().getPath());
        }
        return chain.filter(exchange);
    }
}

