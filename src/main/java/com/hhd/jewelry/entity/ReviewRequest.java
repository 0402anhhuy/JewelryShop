package com.hhd.jewelry.entity;

import com.hhd.jewelry.entity.ItemReview;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequest {

    // 🔹 ID đơn hàng
    private Integer orderId;

    // 🔹 Danh sách các review trong đơn
    private List<ItemReview> reviews;
}