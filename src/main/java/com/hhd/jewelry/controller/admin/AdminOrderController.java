package com.hhd.jewelry.controller.admin;

import com.hhd.jewelry.entity.Order;
import com.hhd.jewelry.entity.Shipper;
import com.hhd.jewelry.repository.OrderRepository;
import com.hhd.jewelry.repository.ShipperRepository;
import com.hhd.jewelry.service.OrderExportService;
import com.hhd.jewelry.service.OrderService;
import com.hhd.jewelry.service.ShipperService;
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
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderRepository orderRepo;
    private final ShipperRepository shipperRepo;
    private final OrderExportService orderExportService;
    private final ShipperService shipperService;
    private final OrderService orderService;

    @GetMapping({""})
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

        return "admin/orders/list";
    }


    // ✅ Chi tiết đơn hàng (modal hiển thị trong list.html)
    @GetMapping("/detail/{id}")
    public String getOrderDetail(@PathVariable Integer id, Model model) {
        Order order = orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        model.addAttribute("order", order);
        model.addAttribute("items", order.getItems());
        return "admin/orders/detail";
    }

    // ✅ Mở form chỉnh sửa đơn hàng
    @GetMapping("/edit/{id}")
    public String editOrder(@PathVariable Integer id, Model model) {
        Order order = orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        model.addAttribute("order", order);
        model.addAttribute("shippers", shipperRepo.findAll());
        model.addAttribute("page", "orders");
        return "admin/orders/form";
    }

    // ✅ Cập nhật đơn hàng
    @PostMapping("/update")
    public String updateOrder(
            // Tham số khớp với 'name' trong form
            @RequestParam("orderId") Integer orderId,
            @RequestParam(value = "status", required = false) Order.Status newStatus,
            @RequestParam(value = "shipperId", required = false) String shipperIdStr,
            @RequestParam(value = "paid", required = false) Boolean paidStatus // Sẽ là TRUE (khi "true") hoặc NULL (khi rỗng)
    ) {

        // 1. Lấy đơn hàng gốc (persistent)
        Order existing = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        boolean needsSave = false;

        // ⭐️ Xử lý 'paid' trước
        // Nếu paidStatus là null (không tick), coi là false.
        // Nếu paidStatus là true (đã tick), coi là true.
        boolean newPaidValue = (paidStatus != null && paidStatus == true);

        if (newPaidValue != existing.isPaid()) {
            existing.setPaid(newPaidValue);
            needsSave = true;
        }

        // 2. Xử lý STATUS (Qua Service)
        // Ưu tiên trạng thái gửi lên từ form
        if (newStatus != null && newStatus != existing.getStatus()) {
            String updatedBy = "admin";
            String notes = "Admin cập nhật trạng thái sang " + newStatus.name();

            // Service sẽ tự động lưu và trả về đối tượng đã được cập nhật
            existing = orderService.updateOrderStatus(existing.getId(), newStatus);
            needsSave = false; // Service đã lưu

        }
        // Nếu status không đổi, nhưng 'paid' có đổi (logic đồng bộ)
        else if (needsSave && newPaidValue == true && existing.getStatus() == Order.Status.PENDING) {
            // User tick 'paid' nhưng status là PENDING -> Tự động chuyển
            existing = orderService.updateOrderStatus(existing.getId(), Order.Status.PAID);
            needsSave = false;
        }
        else if (needsSave && newPaidValue == false && existing.getStatus() == Order.Status.PAID) {
            // User bỏ tick 'paid' -> Tự động chuyển về PENDING
            existing = orderService.updateOrderStatus(existing.getId(), Order.Status.PENDING);
            needsSave = false;
        }


        // 3. Xử lý SHIPPER
        if (shipperIdStr != null) {
            try {
                Integer shipperId = Integer.parseInt(shipperIdStr);

                // Nếu value="0" (chưa gán)
                if (shipperId == 0) {
                    if (existing.getShipper() != null) {
                        existing.setShipper(null);
                        needsSave = true;
                    }
                } else {
                    // Tải đối tượng Shipper THẬT (persistent) từ CSDL
                    Shipper shipper = shipperService.findById(shipperId);

                    if (shipper != null) {
                        existing.setShipper(shipper);
                        needsSave = true;
                    }
                }
            } catch (NumberFormatException e) {
                System.err.println("Giá trị shipperId không hợp lệ, bỏ qua: " + shipperIdStr);
            }
        }

        // 4. Chỉ lưu khi cần thiết
        if (needsSave) {
            orderRepo.save(existing);
        }

        return "redirect:/admin/orders";
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

        return "redirect:/admin/orders";
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
