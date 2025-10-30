package com.hhd.jewelry.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    private String gemstone;

    @Column(nullable = false)
    private String material;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false, unique = true)
    private String serialNumber;

    @Column(nullable = false)
    private Integer price;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer discount;

    @Column(name = "`order`", columnDefinition = "INT DEFAULT 0")
    private Integer order;

    @Column(nullable = false)
    private String gender;

    @ManyToOne
    @JoinColumn(name = "category_id")
    @ToString.Exclude
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    @ToString.Exclude
    private Supplier supplier;

    @ManyToOne
    @JoinColumn(name = "collection_id")
    @ToString.Exclude
    private Collection collection;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    private List<String> imageUrls = new ArrayList<>();

    // 🔹 Đơn hàng liên quan
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<OrderItem> orderItems = new ArrayList<>();


    // 🔹 Danh sách đánh giá (bắt buộc thêm dòng này)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<ProductReview> reviews = new ArrayList<>();

    // 🔹 Danh sách wishlist nếu có
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Wishlist> wishlists = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        if (discount == null) discount = 0;
        if (order == null) order = 0;
    }

    public String getDisplayName() {
        return name + " " + (gemstone != null ? gemstone : "") + " " + material + " " + brand + " " + serialNumber;
    }
}
