package com.example.online_shop.service;

import com.example.online_shop.enumiration.EUserRole;
import com.example.online_shop.exception.UserAlreadyExistsException;
import com.example.online_shop.mapper.UserMapper;
import com.example.online_shop.model.dto.NewUserDto;
import com.example.online_shop.model.dto.UserDto;
import com.example.online_shop.model.entity.User;
import com.example.online_shop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class UserService implements ReactiveUserDetailsService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder encoder;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findUserByLoginIgnoreCase(username)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new UsernameNotFoundException("User Not Found"))))
                .map(userMapper::toUserDto);
    }


    public Mono<String> addUser(Mono<NewUserDto> userMono) {
        return userMono
                .flatMap((NewUserDto newUser) ->
                        userRepository.findUserByLoginIgnoreCase(newUser.getLogin())
                                .flatMap(found ->
                                        Mono.<NewUserDto>error(new UserAlreadyExistsException(found.getLogin())))
                                .switchIfEmpty(Mono.defer(() -> {
                                    newUser.setPassword(encoder.encode(newUser.getPassword()));
                                    newUser.setRoles(EUserRole.ROLE_USER.name());
                                    return Mono.just(newUser);
                                }))
                )
                .flatMap(nu -> userRepository.save(userMapper.toUser(nu))) // теперь nu — NewUserDto
                .map(User::getLogin);
    }

}


