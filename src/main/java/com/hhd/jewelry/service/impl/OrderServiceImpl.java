package com.hhd.jewelry.service.impl;

import com.hhd.jewelry.dto.CheckoutDto;
import com.hhd.jewelry.entity.*;
import com.hhd.jewelry.repository.*;
import com.hhd.jewelry.service.AddressService;
import com.hhd.jewelry.service.OrderService;
import com.hhd.jewelry.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final AddressService addressService;
    private final CartRepository cartRepository;
    private final ProductService productService;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductReviewRepository reviewRepo;
    @Override
    public void save(Order order) {
        orderRepository.save(order);
    }

    @Override
    public boolean existsOrderByUser(User user) {
        return orderRepository.existsByUser(user);
    }

    @Override
    public void deleteById(Integer id) {
        Order order = orderRepository.findById(id).orElse(null);
        order.getItems().clear();
        orderRepository.save(order);
        orderRepository.deleteById(id);
    }

    @Override
    public Optional<Order> getOrderByOrderId(Integer orderId) {
        return orderRepository.findById(orderId);
    }

    @Override
    public Order createNewOrder(User user, CheckoutDto form) {
        Address selectedAddress = addressService.getAddressByIdAndUser(form.getSelectedAddressId(), user)
                .orElseThrow(() -> new IllegalStateException("Địa chỉ không hợp lệ hoặc không thuộc về người dùng."));
        Order order = new Order();
        order.setUser(user);
        order.setStatus(Order.Status.PENDING); // Trạng thái ban đầu
        order.setCreatedAt(LocalDateTime.now());
        order.setPaid(false);
        order.setMethodPay(form.getPaymentMethod());
        order.setNote(form.getNote());
        order.setConsignee(selectedAddress.getReceiverName());
        order.setShippingPhone(selectedAddress.getPhone());
        order.setShippingAddressLine(selectedAddress.getAddressLine());
        order.setShippingWard(selectedAddress.getWard());
        order.setShippingDistrict(selectedAddress.getDistrict());
        order.setShippingCity(selectedAddress.getCity());
        order.setCouponCode(form.getCouponCode());
        if(form.getShippingName().equalsIgnoreCase("Nhận tại cửa hàng")){
            order.setShippingMethod(Order.ShippingMethod.STORE_PICKUP);
        }
        else{
            order.setShippingName(form.getShippingName());
        }
        order.setShippingFee(form.getShippingFee());
        order.setDiscount(form.getDiscount());
        order.setTax(form.getTax());
        List<OrderItem> items = new ArrayList<>();

        // Trường hợp Mua ngay
        if (form.getSerialNumber() != null && !form.getSerialNumber().isEmpty()) {
            Product product = productService.getProductBySerialNumber(form.getSerialNumber());
            if (product == null) throw new IllegalStateException("Sản phẩm không tồn tại!");

            long price = product.getPrice() != null ? product.getPrice() : 0;
            int originalDiscount = product.getDiscount() != null ? product.getDiscount() : 0;
            int discountPercent = Math.min(originalDiscount, 100);
            long finalPrice = price - (price * discountPercent / 100);

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(product);
            oi.setQuantity(1);
            oi.setUnitPrice(BigDecimal.valueOf(finalPrice)); // Giá đã giảm
            oi.setTotalPrice(BigDecimal.valueOf(finalPrice)); // Tổng = giá * 1
            items.add(oi);
        }
        // Trường hợp Mua từ giỏ hàng
        else if (form.getCartId() != null) {
            Cart cart = cartRepository.findById(form.getCartId()).orElse(null);
            if (cart == null) throw new IllegalStateException("Giỏ hàng không tồn tại!");

            // Chỉ xử lý các sản phẩm được CHỌN
            List<CartItem> selectedItems = cart.getItems().stream()
                    .filter(CartItem::isSelected)
                    .collect(Collectors.toList());
            if (selectedItems.isEmpty())
                throw new IllegalStateException("Không có sản phẩm nào được chọn trong giỏ hàng!");

            for (CartItem ci : selectedItems) {
                Product product = ci.getProduct();
                int qty = Math.max(1, ci.getQuantity());

                long price = product.getPrice() != null ? product.getPrice() : 0;
                int originalDiscount = product.getDiscount() != null ? product.getDiscount() : 0;
                int discountPercent = Math.min(originalDiscount, 100);
                long finalPrice = price - (price * discountPercent / 100);

                OrderItem oi = new OrderItem();
                oi.setOrder(order);
                oi.setProduct(product);
                oi.setQuantity(qty);
                oi.setUnitPrice(BigDecimal.valueOf(finalPrice)); // Giá đã giảm
                oi.setTotalPrice(BigDecimal.valueOf(finalPrice).multiply(BigDecimal.valueOf(qty)));
                items.add(oi);
            }
        }
        if (items.isEmpty()) throw new IllegalStateException("Không có sản phẩm hợp lệ!");
        orderItemRepository.saveAll(items);
        order.setItems(items);
        order.setTotalAmount(form.getTotal());
        OrderStatusHistory initialHistory = OrderStatusHistory.builder()
                .order(order) // Liên kết với chính đơn hàng này
                .status(Order.Status.PENDING)
                .timestamp(order.getCreatedAt()) // Dùng luôn thời gian tạo đơn
                .build();
        order.getStatusHistory().add(initialHistory);
        return orderRepository.save(order);
    }

    @Override
    public Order updateOrderStatus(Integer orderId, Order.Status newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));
        order.setStatus(newStatus);
        if (newStatus == Order.Status.PAID) {
            order.setPaid(true);
        }
        OrderStatusHistory newHistoryEntry = OrderStatusHistory.builder()
                .order(order) // Liên kết với đơn hàng
                .status(newStatus) // Trạng thái mới
                .timestamp(LocalDateTime.now()) // Thời gian hiện tại
                .build();
        order.getStatusHistory().add(newHistoryEntry);
        return orderRepository.save(order);
    }

    @Override
    public Order findById(Integer id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng có ID = " + id));
    }

    @Override
    @Transactional
    public void saveAllReviews(ReviewRequest request, Map<Integer, List<MultipartFile>> filesMap) {
        // Lấy đơn hàng
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng có ID = " + request.getOrderId()));

        User user = order.getUser();

        // Lặp qua từng review
        request.getReviews().forEach(r -> {
            // Tìm sản phẩm theo itemId (product.id)
            Product product = productRepository.findById(Long.valueOf(r.getItemId()))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm có ID = " + r.getItemId()));

            // Tạo review
            ProductReview review = ProductReview.builder()
                    .order(order)
                    .user(user)
                    .product(product)
                    .rating(r.getRating())
                    .comment(r.getComment())
                    .build();

            // Xử lý upload files nếu có
            if (filesMap.containsKey(r.getItemId())) {
                List<MultipartFile> files = filesMap.get(r.getItemId());
                List<String> savedPaths = uploadFiles(files, Long.valueOf(product.getId()));
                review.setMediaPaths(savedPaths);
            }

            reviewRepo.save(review);
        });
    }

    // 🔹 Method upload files (tự implement logic upload)
    private List<String> uploadFiles(List<MultipartFile> files, Long productId) {
        List<String> paths = new ArrayList<>();

        // ✅ Đường dẫn cố định
        String uploadDir = "src/main/resources/static/images/reviews/";

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                try {
                    String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

                    // Tạo thư mục reviews/{productId}/
                    File folder = new File(uploadDir + productId);
                    if (!folder.exists()) {
                        folder.mkdirs(); // Tạo tất cả thư mục cha
                    }

                    // Lưu file
                    File destFile = new File(folder, fileName);
                    file.transferTo(destFile);

                    // Lưu web path vào DB
                    String webPath = "/images/reviews/" + productId + "/" + fileName;
                    paths.add(webPath);

                    System.out.println("✅ Saved: " + destFile.getAbsolutePath());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return paths;
    }
}