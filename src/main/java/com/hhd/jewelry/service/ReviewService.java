package com.hhd.jewelry.service;

import com.hhd.jewelry.entity.ReviewRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ReviewService {
    void saveAll(ReviewRequest request, Map<Integer, List<MultipartFile>> filesMap);
}