package com.example.online_shop.repository;

import com.example.online_shop.model.entity.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    Mono<User> findUserByLoginIgnoreCase(String login);
}
