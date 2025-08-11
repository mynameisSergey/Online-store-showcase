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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Optional;

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

        ItemCreateDto itemCreateDto = ItemCreateDto.builder().title("Товар 1").description("Товар для mock проверки").price(BigDecimal.valueOf(10.50)).image(picture).build();

        Item item = Item.builder().title("Товар 1").description("Товар для mock проверки").price(BigDecimal.valueOf(10.50)).image(FAKE_IMAGE).build();

        ItemDto itemDto = ItemDto.builder().title("Товар 1").description("Товар для mock проверки").price(BigDecimal.valueOf(10.50)).imagePath(IMAGE_PATH + "1L").build();

        when(itemMapper.toItem(any(ItemCreateDto.class))).thenReturn(item);
        when(itemRepository.save(any(Item.class))).thenReturn(item);
        when(itemMapper.toDto(any(Item.class))).thenReturn(itemDto);

        ItemDto result = itemService.saveItem(itemCreateDto);
        assertThat(result).isEqualTo(itemDto);
    }

    @Test
    void testGetImage() {

        Item item = Item.builder().id(1L).title("Товар 1").description("Товар для mock проверки").price(BigDecimal.valueOf(10)).image(FAKE_IMAGE).build();

        when(itemRepository.findById(any(Long.class))).thenReturn(Optional.of(item));

        byte[] image = itemService.getImage(1L);

        assertThat(image).isEqualTo(FAKE_IMAGE);
        verify(itemRepository).findById(1L);
    }

    @Test
    void testGetItemDtoById() {

        ItemDto itemDto = ItemDto.builder().title("Товар 1").description("Товар для mock проверки").price(BigDecimal.valueOf(10)).imagePath(IMAGE_PATH + "1L").build();

        Item item = Item.builder().id(1L).title("Товар 1").description("Товар для mock проверки").price(BigDecimal.valueOf(10)).image(FAKE_IMAGE).build();

        when(itemRepository.findById(any(Long.class))).thenReturn(Optional.of(item));
        when(itemMapper.toDto(any(Item.class))).thenReturn(itemDto);
        when(cartService.getItemsInCart()).thenReturn(new HashMap<>());

        ItemDto itemRes = itemService.getItemDtoById(1L);

        assertThat(itemRes).isEqualTo(itemDto);
    }
}
