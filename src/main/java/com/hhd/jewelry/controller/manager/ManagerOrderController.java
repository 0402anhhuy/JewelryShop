package com.hhd.jewelry.controller.manager;

import com.hhd.jewelry.entity.Order;
import com.hhd.jewelry.repository.OrderRepository;
import com.hhd.jewelry.repository.ShipperRepository;
import com.hhd.jewelry.service.OrderExportService;
import com.hhd.jewelry.service.specification.OrderSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;


@Controller
@RequestMapping("/manager/orders")
@RequiredArgsConstructor
public class ManagerOrderController {

    private final OrderRepository orderRepo;
    private final ShipperRepository shipperRepo;
    private final OrderExportService orderExportService;
    @GetMapping({"", "/"})
    public String listOrders(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year,
            Model model
    ) {
        int pageSize = 5;
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));

        LocalDate from = null;
        if (fromDate != null && !fromDate.isBlank()) {
            try { from = LocalDate.parse(fromDate); } catch (Exception e) {}
        }

        LocalDate to = null;
        if (toDate != null && !toDate.isBlank()) {
            try { to = LocalDate.parse(toDate); } catch (Exception e) {}
        }

        // ✅ Kết hợp các Specification
        // Bắt đầu với một Specification không làm gì cả (luôn đúng)
        Specification<Order> spec = Specification.not(null);

        // Nối các điều kiện (các hàm Specs trả về null sẽ tự động bị bỏ qua)
        spec = spec.and(OrderSpecs.byKeyword(keyword));
        spec = spec.and(OrderSpecs.byFromDate(from));
        spec = spec.and(OrderSpecs.byToDate(to));
        spec = spec.and(OrderSpecs.byMonth(month));
        spec = spec.and(OrderSpecs.byYear(year));

        // ✅ Gọi repo với Specification
        Page<Order> orderPage = orderRepo.findAll(spec, pageable);

        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("month", month);
        model.addAttribute("year", year);
        model.addAttribute("page", "orders");

        return "manager/orders/list";
    }


    // ✅ Chi tiết đơn hàng (modal hiển thị trong list.html)
    @GetMapping("/detail/{id}")
    public String getOrderDetail(@PathVariable Integer id, Model model) {
        Order order = orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        model.addAttribute("order", order);
        model.addAttribute("items", order.getItems());
        return "manager/orders/detail";
    }

    // ✅ Mở form chỉnh sửa đơn hàng
    @GetMapping("/edit/{id}")
    public String editOrder(@PathVariable Integer id, Model model) {
        Order order = orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        model.addAttribute("order", order);
        model.addAttribute("shippers", shipperRepo.findAll());
        model.addAttribute("page", "orders");
        return "manager/orders/form";
    }

    // ✅ Cập nhật đơn hàng
    @PostMapping("/update")
    public String updateOrder(@ModelAttribute("order") Order order,
                              @RequestParam(value = "paid", required = false) Boolean paid) {
        Order existing = orderRepo.findById(order.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // 🟢 Chuyển status từ form
        if (order.getStatus() != null) {
            existing.setStatus(Order.Status.valueOf(order.getStatus().name().toUpperCase()));
        }

        existing.setShipper(order.getShipper());

        // ✅ Xử lý checkbox paid đúng cách
        existing.setPaid(paid != null && paid);

        orderRepo.save(existing);
        return "redirect:/manager/orders";
    }

    // ✅ Xóa đơn hàng (chưa thanh toán)
    @GetMapping("/delete/{id}")
    public String deleteOrder(@PathVariable Integer id) {
        Order order = orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!order.isPaid()) {
            orderRepo.delete(order);
        } else {
            throw new RuntimeException("Không thể xóa đơn hàng đã thanh toán!");
        }

        return "redirect:/manager/orders";
    }

    @GetMapping("/export/excel")
    public ResponseEntity<InputStreamResource> exportExcel() throws Exception {
        ByteArrayInputStream in = orderExportService.exportToExcel();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=orders.xlsx");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<InputStreamResource> exportPdf() throws Exception {
        ByteArrayInputStream in = orderExportService.exportToPdf();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=orders.pdf");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(in));
    }
}
