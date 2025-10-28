package com.hhd.jewelry.service.impl;

import com.hhd.jewelry.entity.*;
import com.hhd.jewelry.repository.CartItemRepository;
import com.hhd.jewelry.repository.CartRepository;
import com.hhd.jewelry.repository.ProductRepository;
import com.hhd.jewelry.repository.UserRepository;
import com.hhd.jewelry.service.ProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository  userRepository;

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAllBy();
    }

    @Override
    public Product getProductByName(String productName) {
        return productRepository.findByName(productName).orElse(null);
    }

    @Override
    public Product getProductBySerialNumber(String serialNumber) {
        return productRepository.findBySerialNumber(serialNumber).orElse(null);
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    @Override
    public List<Product> getProductsById(List<Long> id) {
        return productRepository.findAllById(id);
    }

    @Override
    public List<Product> getProductsByCategoryName(String categoryName) {
        return productRepository.findAllByCategory_Name(categoryName);
    }

    @Override
    public List<Product> getProductsByCollectionName(String collectionName) {
        return productRepository.findAllByCollection_Name(collectionName);
    }

    @Override
    public List<Product> getProductsByCategoryNameAndCollectionName(String categoryName, String collectionName) {
        return productRepository.findAllByCategory_NameAndCollection_Name(categoryName, collectionName);
    }

    @Override
    public List<Product> getProductsByPriceBetween(Integer minPrice, Integer maxPrice) {
        return productRepository.findAllByPriceBetween(minPrice, maxPrice);
    }

    @Override
    public List<Product> getProductsByStockQuantityGreaterThan(int stock) {
        return productRepository.findAllByStockQuantityGreaterThan(stock);
    }

    @Override
    public List<Product> getProductsByMaterial(String material) {
        return productRepository.findAllByMaterial(material);
    }

    @Override
    public void save(Product product) {
        Optional<Product> existingProduct = productRepository.findBySerialNumber(product.getSerialNumber());

        if (existingProduct.isPresent()) {
            Product existing = existingProduct.get();

            existing.setName(product.getName());
            existing.setGemstone(product.getGemstone());
            existing.setMaterial(product.getMaterial());
            existing.setBrand(product.getBrand());
            existing.setPrice(product.getPrice());
            existing.setDiscount(product.getDiscount());
            existing.setOrder(product.getOrder());
            existing.setGender(product.getGender());
            existing.setCategory(product.getCategory());
            existing.setCollection(product.getCollection());
            existing.setStockQuantity(product.getStockQuantity());
            existing.setImageUrls(product.getImageUrls());

            productRepository.save(existing);
        }
        else {
            productRepository.save(product);
        }
    }

    @Override
    public void delete(Product product) {
        productRepository.delete(product);
    }

    @Override
    public void deleteAll() {
        productRepository.deleteAll();
    }

    @Override
    public void resetAutoIncrement() {
        productRepository.resetAutoIncrement();
    }

    @Override
    public void AddProductToCart(String email, String serialNumber) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            Cart cart = this.cartRepository.findByUser(user);
            if (cart == null) {
                Cart newCart = new Cart();
                newCart.setUser(user);
            }
            Product product = productRepository.findBySerialNumber(serialNumber).orElse(null);
            CartItem cartItem = cartItemRepository.findCartItemByCartAndProduct(cart, product).orElse(null);
            if (cartItem == null || cartItem.getCart() != cart) {
                CartItem newCartItem = new CartItem();
                newCartItem.setProduct(product);
                newCartItem.setCart(cart);
                newCartItem.setQuantity(1);
                newCartItem.setPrice(product.getPrice());
                this.cartItemRepository.save(newCartItem);
            } else {
                cartItem.setQuantity(cartItem.getQuantity() + 1);
                cartItem.setPrice(product.getPrice() * cartItem.getQuantity());
                this.cartItemRepository.save(cartItem);
            }
        }
    }
    @Transactional
    @Override
    public void RemoveProductToCart(Integer cart_id, Integer product_id) {
        cartItemRepository.deleteByCart_CartIdAndProduct_Id(cart_id, product_id);
    }
    @Override
    public void updateStockForOrder(Order order) {
        // Duyệt qua từng sản phẩm trong đơn hàng
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int orderedQuantity = item.getQuantity();

            // Lấy số lượng tồn kho hiện tại
            int currentStock = product.getStockQuantity();

            // Kiểm tra xem có đủ hàng không
            if (currentStock < orderedQuantity) {
                // Nếu không đủ, ném ra ngoại lệ để rollback transaction
                throw new IllegalStateException("Sản phẩm " + product.getName() + " không đủ số lượng tồn kho. Chỉ còn " + currentStock + " sản phẩm.");
            }

            // Tính toán số lượng mới và cập nhật
            int newStock = currentStock - orderedQuantity;
            product.setStockQuantity(newStock);
            product.setOrder(product.getOrder()+orderedQuantity);

            // Lưu lại thông tin sản phẩm đã cập nhật
            productRepository.save(product);
        }
    }

    @Override
    public Page<Product> findProducts(Specification<Product> spec, Pageable pageable) {
        return productRepository.findAll(spec, pageable);
    }

    @Override
    public List<Product> findAllProducts(Specification<Product> spec, Sort sort) {
        return productRepository.findAll(spec, sort);
    }
}
