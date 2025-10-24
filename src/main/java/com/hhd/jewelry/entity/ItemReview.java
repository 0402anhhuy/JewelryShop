package com.hhd.jewelry.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public  class ItemReview {
    @Id
    private Long id;
    private Integer itemId;           // ID sản phẩm hoặc OrderItem
    private Integer rating;           // Số sao
    private String comment;           // Nhận xét
    private List<String> mediaPaths;  // Danh sách đường dẫn ảnh/video
}