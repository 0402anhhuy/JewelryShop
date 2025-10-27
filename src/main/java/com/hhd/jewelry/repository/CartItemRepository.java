package com.hhd.jewelry.repository;

import com.hhd.jewelry.entity.Cart;
import com.hhd.jewelry.entity.CartItem;
import com.hhd.jewelry.entity.Product;
import com.hhd.jewelry.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem,Integer> {
    Optional<CartItem> findCartItemByCartAndProduct(Cart cart, Product product);

    List<CartItem> findAllByCart_CartId(Integer cartCartId);
    @Transactional
    void deleteByCart_CartIdAndProduct_Id(Integer cart_id,  Integer product_id);
    @Modifying
    @Transactional
    @Query("delete from CartItem ci where ci.cart.user = :user and ci.selected = true")
    int deleteSelectedByUser(User user);
}
