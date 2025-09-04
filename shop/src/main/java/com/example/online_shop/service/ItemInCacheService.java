package com.example.online_shop.service;

import com.example.online_shop.enumiration.ESort;
import com.example.online_shop.mapper.ItemMapper;
import com.example.online_shop.model.dto.ItemDto;
import com.example.online_shop.model.entity.Item;
import com.example.online_shop.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemInCacheService {
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    @Value("${shop.items.row:5}")
    int itemsRowCount;

    @Cacheable(cacheNames = "items", key = "{#search, #sort, #pageNumber, #pageSize}")
    public Mono<List<ItemDto>> getItems(String search, String sort, int pageNumber, int pageSize) {
        log.debug("Start getItems: pageNumber={}, pageSize={}, sort={}, search={}", pageNumber, pageSize, sort, search);
        Pageable page = switch (ESort.valueOf(sort.toUpperCase())) {
            case NO -> PageRequest.of(pageNumber - 1, pageSize);
            case ALPHA -> PageRequest.of(pageNumber - 1, pageSize, Sort.by(Sort.Direction.ASC, "title"));
            case PRICE -> PageRequest.of(pageNumber - 1, pageSize, Sort.by(Sort.Direction.ASC, "price"));
        };
        Flux<Item> items;
        if (search != null && !search.isBlank())
            items = itemRepository.getItemsByTitleLike(search, page);
        else
            items = itemRepository.findBy(page);
        return itemMapper.toListDto(items).log().collectList();
    }

    @Cacheable(value = "item", key = "#id")
    public Mono<ItemDto> getItemDtoById(Long id) {
        log.debug("Start id={}", id);
        return itemRepository.findById(id)
                .map(itemMapper::toDto);
    }

    @Cacheable(value = "picture", key = "#id")
    public Mono<byte[]> geyImage(Long id) {
        return itemRepository.findById(id).map(Item::getImage).onErrorComplete();
    }
}
