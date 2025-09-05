package com.example.online_shop.service;

import com.example.online_shop.enumiration.ECartAction;
import com.example.online_shop.enumiration.ESort;
import com.example.online_shop.mapper.ItemInCartMapper;
import com.example.online_shop.mapper.ItemMapper;
import com.example.online_shop.model.dto.ItemCreateDto;
import com.example.online_shop.model.dto.ItemDto;
import com.example.online_shop.model.dto.ItemsWithPagingDto;
import com.example.online_shop.model.dto.PagingParametersDto;
import com.example.online_shop.model.entity.Item;
import com.example.online_shop.model.entity.ItemInCart;
import com.example.online_shop.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisKeyValueAdapter;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableRedisRepositories(enableKeyspaceEvents = RedisKeyValueAdapter.EnableKeyspaceEvents.ON_STARTUP)
public class ItemService {
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final CartService cartService;
    private final ItemInCacheService cacheService;
    private final ItemInCartService itemInCartService;
    private final ItemInCartMapper itemInCartMapper;

    @Value("${shop.items.row:5}")
    int itemsRowCount;

    public Mono<List<List<ItemDto>>> getItems(String search, String sort, int pageNumber, int pageSize, String login) {
        log.info("Start getItems: login={}", login);
        AtomicInteger index = new AtomicInteger();
        return cacheService.getItems(search, sort, pageNumber, pageSize)
                .flatMapIterable(itemsList -> itemsList)
                .log()
                .flatMap(itemDto -> itemInCartService.getCountByItemIdAndLogin(itemDto.getId(), login).defaultIfEmpty(0)
                        .zipWith(Mono.just(itemDto), (countInLoginCart, item) -> {
                            item.setCount(countInLoginCart);
                            return item;
                        }))
                .log()
                .collectList()
                .map(itemsListWithCount -> itemsListWithCount.stream()
                        .collect(Collectors.groupingBy(it -> index.getAndIncrement() / itemsRowCount))
                        .values()
                        .stream().toList());
    }

    public Mono<PagingParametersDto> getPaging(String search, String sort, int pageNumber, int pagesize) {
        return Mono.just(PagingParametersDto.builder()
                        .pageNumber(pageNumber)
                        .pageSize(pagesize)
                        .hasPrevious(pageNumber > 1)
                        .build())
                .zipWith(cacheService.getItems(search, sort, pageNumber, pagesize)
                        .flatMapIterable(itemList -> itemList).count()
                        .map(count -> pageNumber < Math.ceilDiv(count, pagesize)), (paging, isNextPage) -> {
                    paging.setHasNext(isNextPage);
                    return paging;
                });
    }


    public Mono<ItemDto> actionWithItemInCart(Long itemId, String action, String login) {
        return itemInCartService.getByItemIdAndLogin(itemId, login)
                .defaultIfEmpty(new ItemInCart())
                .log()
                .zipWith(getItemDtoById(itemId, login), (itemInCart, item) -> {
                    ItemInCart actualItemDto = itemInCart;
                    if (itemInCart.getCount() == 0) actualItemDto = itemInCartMapper.toItemInCart(item);
                    actualItemDto.setLogin(login);
                    return actualItemDto;
                })
                .log()
                .flatMap(itemInCart -> cartService.refresh(itemInCart, action, login))
                .log();
    }

    public Mono<ItemDto> getItemDtoById(Long id, String login) {
        return cacheService.getItemDtoById(id)
                .zipWith(itemInCartService.getCountByItemIdAndLogin(id, login).defaultIfEmpty(0), (itemDto, count) -> {
                    itemDto.setCount(count);
                    return itemDto;
                });
    }


    public Mono<byte[]> getImage(Long id) {
        return cacheService.geyImage(id);
    }

    @Transactional
    public Mono<ItemDto> saveItem(Mono<ItemCreateDto> itemCreatedDto) {
        log.debug("Start saveItem: item={}, thread={}", itemCreatedDto, Thread.currentThread().getName());
        return itemCreatedDto
                .map(dto -> {
                    if (dto.getImage() == null)
                        return itemCreatedDto
                                .map(itemMapper::toItem)
                                .flatMap(itemRepository::save)
                                .log()
                                .map(itemMapper::toDto);
                    return DataBufferUtils.join(dto.getImage().content())
                            .publishOn(Schedulers.boundedElastic())
                            .<byte[]>handle((dataBuffer, sink) -> {
                                try {
                                    sink.next(dataBuffer.asInputStream().readAllBytes());
                                } catch (IOException e) {
                                    sink.error(new RuntimeException(e));
                                }
                            })
                            .zipWith(itemCreatedDto.map(itemMapper::toItem), (byteArray, item) -> {
                                item.setImage(byteArray);
                                return item;
                            })
                            .flatMap(itemRepository::save)
                            .map(itemMapper::toDto);
                })
                .flatMap(Function.identity());
    }

}