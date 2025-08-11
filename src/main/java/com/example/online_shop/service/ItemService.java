package com.example.online_shop.service;

import com.example.online_shop.enumiration.ECartAction;
import com.example.online_shop.enumiration.ESort;
import com.example.online_shop.mapper.ItemMapper;
import com.example.online_shop.model.dto.ItemCreateDto;
import com.example.online_shop.model.dto.ItemDto;
import com.example.online_shop.model.dto.ItemsWithPagingDto;
import com.example.online_shop.model.dto.PagingParametersDto;
import com.example.online_shop.model.entity.Item;
import com.example.online_shop.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService {
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final CartService cartService;

    @Value("${shop.items.row:5}")
    int itemsRowCount;

    public Mono<ItemsWithPagingDto> getItems(String search, String sort, int pageNumber, int pageSize) {
        log.debug("Start getItems: pageNumber={}, pageSize={}", pageNumber, pageSize);

        Pageable page = switch (ESort.valueOf(sort.toUpperCase())) {
            case NO -> PageRequest.of(pageNumber - 1, pageSize);
            case ALPHA -> PageRequest.of(pageNumber - 1, pageSize, Sort.by("title"));
            case PRICE -> PageRequest.of(pageNumber - 1, pageSize, Sort.by("price"));
        };

        Flux<Item> itemsFlux = (search!= null &&!search.isBlank())? itemRepository.getItemsByTitleLike(search, page)
                : itemRepository.findBy(page);

        Mono<List<List<ItemDto>>> itemsDto = itemMapper.toListDto(itemsFlux).map(dto -> {
            dto.setCount(cartService.getItemCountInCart(dto.getId()));
            return dto;
        }).collectList().map(list -> {
            List<List<ItemDto>> grouped = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                int groupIndex = i / itemsRowCount;
                if (grouped.size() <= groupIndex) {
                    grouped.add(new ArrayList<>());
                }
                grouped.get(groupIndex).add(list.get(i));
            }
            return grouped;
        });

        Mono<Boolean> hasNext = itemRepository.count().map(count -> pageNumber < (int) Math.ceil((double) count / pageSize));

        Mono<PagingParametersDto> pagingDto = Mono.just(PagingParametersDto.builder().pageNumber(pageNumber)
                .pageSize(pageSize).hasPrevious(pageNumber > 1).build())
                .zipWith(hasNext, (paging, next) -> {
            paging.setHasNext(next);
            return paging;
        }).log();

        return Mono.zip(pagingDto, itemsDto).map(tuple -> {
            ItemsWithPagingDto result = new ItemsWithPagingDto();
            result.setPaging(tuple.getT1());
            result.setItems(tuple.getT2());
            return result;
        });
    }

    public Mono<ItemDto> actionWithItemInCart(Long itemId, String action) {
        Map<Long, ItemDto> itemsInCart = cartService.getItemsInCart();
        return (itemsInCart.containsKey(itemId) ? Mono.just(itemsInCart.get(itemId)) :
                getItemDtoById(itemId))
                .map(item -> {
                    switch (ECartAction.valueOf(action.toUpperCase())) {
                        case PLUS -> item.setCount(item.getCount() + 1);
                        case MINUS -> {
                            if (item.getCount() >= 1) item.setCount(item.getCount() - 1);
                        }
                        case DELETE -> item.setCount(0);
                    }
                    if (item.getCount() == 0) itemsInCart.remove(itemId);
                    else itemsInCart.put(itemId, item);
                    cartService.refresh(itemsInCart, item);
                    return item;
                });
    }

    public Mono<ItemDto> getItemDtoById(Long id) {
        return itemRepository.findById(id)
                .map(itemMapper::toDto)
                .map(itemDto -> {
                    itemDto.setCount(cartService.getItemCountInCart(id));
                    return itemDto;
                });
    }

    public Mono<byte[]> getImage(Long id) {
        return itemRepository.findById(id)
                .map(Item::getImage).onErrorComplete();
    }

    @Transactional
    public Mono<ItemDto> saveItem(Mono<ItemCreateDto> itemCreateDtoMono) {
        log.debug("Start saveItem: item={}, thread={}", itemCreateDtoMono, Thread.currentThread().getName());

        return itemCreateDtoMono.flatMap(dto -> {
            if (dto.getImage() == null) {
                // Нет картинки — просто мапим и сохраняем
                Item item = itemMapper.toItem(dto);
                return itemRepository.save(item).log().map(itemMapper::toDto);
            } else {
                // Есть картинка — считываем байты картинки реактивно
                return DataBufferUtils.join(dto.getImage().content()).publishOn(Schedulers.boundedElastic())
                        .map(dataBuffer -> {
                    try (var is = dataBuffer.asInputStream()) {
                        return is.readAllBytes();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).zipWith(Mono.just(itemMapper.toItem(dto)), (imageBytes, item) -> {
                    item.setImage(imageBytes);
                    return item;
                }).flatMap(itemRepository::save).map(itemMapper::toDto);
            }
        });
    }

}