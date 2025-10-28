package com.hhd.jewelry.entity;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequest {

    private Integer orderId;

    private List<ItemReview> reviews;
}