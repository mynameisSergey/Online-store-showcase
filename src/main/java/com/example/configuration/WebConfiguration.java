package com.example.configuration;

import com.example.online_shop.model.dto.CartDto;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.SessionScope;

@Configuration
@Slf4j
public class WebConfiguration {
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean("cart") //создаем корзину для товаров
    @SessionScope // создает новую корзину для каждого пользователя
    public CartDto cart() {
        log.info("Initialize cart");
        return new CartDto();

    }
}
