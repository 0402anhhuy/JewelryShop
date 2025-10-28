package com.hhd.jewelry.service.impl;

import com.hhd.jewelry.entity.*;
import com.hhd.jewelry.repository.*;
import com.hhd.jewelry.service.CartService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    @Override
    public Cart getCartByUser(User user) {
        return cartRepository.findByUser(user);
    }

    @Override
    public void save(Cart cart) {
        cartRepository.save(cart);
    }

    @Override
    public void delete(Cart cart) {
        cartRepository.delete(cart);
    }

    @Override
    public boolean addItemsFromOrderToCartAndSelect(Integer orderId, User user) {
        // 1. Tìm đơn hàng cũ
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        // 2. Lấy giỏ hàng hiện tại của người dùng (hoặc tạo mới nếu chưa có)
        Cart cart = cartRepository.findByUser(user);

        // 3. Lặp qua các item trong đơn hàng cũ
        for (OrderItem orderItem : order.getItems()) {
            Product product = orderItem.getProduct();
            int quantity = orderItem.getQuantity();

            // 4. KIỂM TRA SẢN PHẨM CÒN TỒN TẠI VÀ ĐỦ HÀNG KHÔNG?
            // (Quan trọng: bạn cần kiểm tra logic tồn kho của mình ở đây)
            if (product == null || product.getStockQuantity() < quantity) {
                throw new IllegalStateException("Sản phẩm '" + (product != null ? product.getName() : "Không xác định") + "' không đủ hàng hoặc không tồn tại.");
                // Hoặc bạn có thể chọn bỏ qua sản phẩm này và tiếp tục
                // continue;
            }

            // 5. Tìm xem sản phẩm đã có trong giỏ hàng chưa
            CartItem existingCartItem = cart.getItems().stream()
                    .filter(ci -> ci.getProduct().getId().equals(product.getId()))
                    .findFirst()
                    .orElse(null);

            if (existingCartItem != null) {
                // Nếu đã có -> cộng dồn số lượng và ĐÁNH DẤU LÀ SELECTED
                existingCartItem.setQuantity(existingCartItem.getQuantity() + quantity);
                existingCartItem.setSelected(true); // Đánh dấu chọn
                cartItemRepository.save(existingCartItem); // Lưu lại thay đổi
            } else {
                // Nếu chưa có -> tạo CartItem mới và ĐÁNH DẤU LÀ SELECTED
                CartItem newCartItem = new CartItem();
                newCartItem.setCart(cart);
                newCartItem.setPrice(product.getPrice());
                newCartItem.setProduct(product);
                newCartItem.setQuantity(quantity);
                newCartItem.setSelected(true); // Đánh dấu chọn
                cart.getItems().add(newCartItem); // Thêm vào danh sách của giỏ hàng
                // Không cần save riêng newCartItem nếu Cart có CascadeType.PERSIST hoặc ALL
            }
        }

        // 6. Lưu lại giỏ hàng (bao gồm các CartItem mới nếu có Cascade)
        cartRepository.save(cart);
        return true;
    }
}
