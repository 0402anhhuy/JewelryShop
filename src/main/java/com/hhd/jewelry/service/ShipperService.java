package com.hhd.jewelry.service;

import com.hhd.jewelry.entity.Shipper;
import com.hhd.jewelry.repository.ShipperRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipperService {

    private final ShipperRepository shipperRepo;

    // ✅ Lấy toàn bộ (dùng cho dropdown chọn shipper)
    public List<Shipper> findAll() {
        return shipperRepo.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    // ✅ Lấy danh sách có phân trang (dùng cho trang /admin/shippers)
    public Page<Shipper> findAll(Pageable pageable) {
        return shipperRepo.findAll(pageable);
    }

    // ✅ Lưu shipper
    public void save(Shipper shipper) {
        shipperRepo.save(shipper);
    }

    // ✅ Xóa shipper theo id
    public void deleteById(Integer id) {
        shipperRepo.deleteById(id);
    }

    // ✅ Tìm theo id
    public Shipper findById(Integer id) {
        return shipperRepo.findById(id).orElse(null);
    }
}
