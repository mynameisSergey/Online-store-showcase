package com.example.online_shop.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItemCreateDto {
    private Long id;
    private String title;
    private String description;
    private MultipartFile image; //нужен только для приёма файла из формы.
    @Builder.Default
    private BigDecimal price = BigDecimal.valueOf(0);
}
