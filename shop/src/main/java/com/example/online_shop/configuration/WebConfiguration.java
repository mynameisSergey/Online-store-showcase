package com.example.online_shop.configuration;

import com.example.online_shop.model.dto.CartDto;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.SessionScope;

@Slf4j
@Configuration
public class WebConfiguration {
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean("cart") //создаем корзину для товаров
    public CartDto cart() {
        log.info("Initialize cart");
        return new CartDto();

    }
}
