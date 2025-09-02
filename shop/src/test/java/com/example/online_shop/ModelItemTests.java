package com.example.online_shop;

import com.example.online_shop.mapper.ItemMapper;
import com.example.online_shop.model.dto.ItemCreateDto;
import com.example.online_shop.model.dto.ItemDto;
import com.example.online_shop.model.entity.Item;
import com.example.online_shop.repository.ItemRepository;
import com.example.online_shop.service.CartService;
import com.example.online_shop.service.ItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelItemTests {

    private static final byte[] FAKE_IMAGE = new byte[]{1, 2, 3};
    private static final String IMAGE_PATH = "http://localhost:8084/items/image/";

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private ItemMapper itemMapper;
    @Mock
    private CartService cartService;

    @InjectMocks
    private ItemService itemService;

    @Test
    void testAddItem() {

        // Создаём DTO с фиктивной картинкой
        MultipartFile picture = new MockMultipartFile("shop.png", FAKE_IMAGE);

        ItemCreateDto itemCreateDto = ItemCreateDto.builder().title("Товар 1").description("Товар для mock проверки")
                .price(BigDecimal.valueOf(10.50)).image((FilePart) picture).build();

        Item item = Item.builder().title("Товар 1").description("Товар для mock проверки")
                .price(BigDecimal.valueOf(10.50)).image(FAKE_IMAGE).build();

        ItemDto itemDto = ItemDto.builder().title("Товар 1").description("Товар для mock проверки")
                .price(BigDecimal.valueOf(10.50)).imagePath(IMAGE_PATH + "1L").build();

        when(itemMapper.toItem(any(ItemCreateDto.class))).thenReturn(item);
        when(itemRepository.save(any(Item.class))).thenReturn(Mono.just(item));
        when(itemMapper.toDto(any(Item.class))).thenReturn(itemDto);

        itemService.saveItem(Mono.just(itemCreateDto))
                .doOnNext(itemRes -> assertThat(itemRes).isEqualTo(itemDto))
                .subscribe();
    }

    @Test
    void testGetImage() {

        Item item = Item.builder().id(1L).title("Товар 1").description("Товар для mock проверки")
                .price(BigDecimal.valueOf(10)).image(FAKE_IMAGE).build();

        when(itemRepository.findById(any(Long.class))).thenReturn(Mono.just(item));

        itemService.getImage(1L)
                .doOnNext(image -> assertThat(image).isEqualTo(item.getImage()))
                .subscribe();
        verify(itemRepository).findById(1L);
    }

    @Test
    void testGetItemDtoById() {

        ItemDto itemDto = ItemDto.builder().title("Товар 1").description("Товар для mock проверки")
                .price(BigDecimal.valueOf(10)).imagePath(IMAGE_PATH + "1L").build();

        Item item = Item.builder().id(1L).title("Товар 1").description("Товар для mock проверки")
                .price(BigDecimal.valueOf(10)).image(FAKE_IMAGE).build();

        when(itemRepository.findById(any(Long.class))).thenReturn(Mono.just(item));
        when(itemMapper.toDto(any(Item.class))).thenReturn(itemDto);
        when(cartService.getItemsInCart()).thenReturn(new HashMap<>());

        itemService.getItemDtoById(1L)
                .doOnNext(itemRes -> assertThat(itemRes).isEqualTo(itemDto))
                .subscribe();
    }
}
