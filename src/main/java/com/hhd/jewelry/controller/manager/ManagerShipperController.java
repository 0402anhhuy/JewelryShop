package com.hhd.jewelry.controller.manager;

import com.hhd.jewelry.entity.Shipper;
import com.hhd.jewelry.service.ShipperService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/manager/shippers")
@RequiredArgsConstructor
public class ManagerShipperController {

    private final ShipperService shipperService;

    // Danh sách Shipper có phân trang
    @GetMapping
    public String list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        Page<Shipper> shipperPage = shipperService.findAll(pageable);

        model.addAttribute("shippers", shipperPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", shipperPage.getTotalPages());
        model.addAttribute("totalItems", shipperPage.getTotalElements());
        model.addAttribute("shipper", new Shipper());
        model.addAttribute("page", "shippers"); // sidebar active

        return "manager/shippers/list";
    }

    // Lưu shipper
    @PostMapping("/save")
    public String save(@ModelAttribute("shipper") Shipper shipper) {
        shipperService.save(shipper);
        return "redirect:/manager/shippers";
    }

    // Xóa shipper
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        shipperService.deleteById(id);
        return "redirect:/manager/shippers";
    }

    // Xem chi tiết shipper
    @GetMapping("/{id}")
    public String view(@PathVariable Integer id, Model model) {
        Shipper shipper = shipperService.findById(id);
        if (shipper == null) return "redirect:/manager/shippers";
        model.addAttribute("shipper", shipper);
        model.addAttribute("orders", shipper.getOrders());
        model.addAttribute("page", "shippers");
        return "manager/shippers/detail";
    }
}
