package com.hhd.jewelry.controller.admin;

import com.hhd.jewelry.entity.Cart;
import com.hhd.jewelry.entity.Manager;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.repository.CartRepository;
import com.hhd.jewelry.repository.ManagerRepository;
import com.hhd.jewelry.repository.UserRepository;
import com.hhd.jewelry.service.AuditLogService;
import com.hhd.jewelry.service.CartService;
import com.hhd.jewelry.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepo;
    private final ManagerRepository managerRepository;
    private final CartRepository cartRepository;

    private final PasswordEncoder encoder;
    private final CartService cartService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    // ✅ Danh sách người dùng (phân trang + tìm kiếm + lọc role)
    @GetMapping({ "", "/" })
    public String listUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "page", defaultValue = "1") int page,
            Model model
    ) {
        int pageSize = 5;
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<User> userPage = userRepo.findAll(pageable);

        // 🔍 Lọc theo keyword
        if (keyword != null && !keyword.isBlank()) {
            String lower = keyword.toLowerCase();
            var filtered = userPage.getContent().stream()
                    .filter(u -> (u.getFullName() != null && u.getFullName().toLowerCase().contains(lower))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(lower)))
                    .toList();
            userPage = new PageImpl<>(filtered, pageable, filtered.size());
        }

        // 🔍 Lọc theo role
        if (role != null && !role.isBlank()) {
            var filtered = userPage.getContent().stream()
                    .filter(u -> u.getRole() != null && u.getRole().name().equalsIgnoreCase(role))
                    .toList();
            userPage = new PageImpl<>(filtered, pageable, filtered.size());
        }

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("roles", Arrays.stream(User.Role.values())
                .filter(r -> r != User.Role.GUEST)
                .toList());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedRole", role);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("page", "users");

        return "admin/users/list";
    }

    // ✅ Form thêm user
    @GetMapping("/form")
    public String showAddForm(Model model) {
        var user = new User();
        user.setGender("Nam");
        user.setRole(User.Role.USER);

        model.addAttribute("user", user);
        model.addAttribute("roles", Arrays.stream(User.Role.values())
                .filter(r -> r != User.Role.GUEST)
                .toList());
        model.addAttribute("isEdit", false);
        model.addAttribute("page", "users");
        return "admin/users/form";
    }

    @GetMapping("/form/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        var user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        user.setPasswordHash("");

        model.addAttribute("user", user);
        model.addAttribute("roles", Arrays.stream(User.Role.values())
                .filter(r -> r != User.Role.GUEST)
                .toList());
        model.addAttribute("isEdit", true);
        model.addAttribute("page", "users");
        return "admin/users/form";
    }

    // ✅ Lưu user (cả thêm và sửa) - GHI LOG ĐơN GIẢN
    @PostMapping("/save")
    public String saveUser(@ModelAttribute User form,
                           @RequestParam(required = false) String newPassword,
                           HttpServletRequest request) {

        User user;
        boolean isNew = (form.getId() == null);

        if (!isNew) {
            // Cập nhật user
            user = userRepo.findById(form.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        } else {
            // Thêm user mới
            user = new User();
            user.setCreatedAt(LocalDateTime.now());
        }

        // Gán dữ liệu chung
        user.setFullName(form.getFullName());
        user.setEmail(form.getEmail());
        user.setPhone(form.getPhone());
        user.setGender(form.getGender());
        user.setDateOfBirth(form.getDateOfBirth());
        user.setAddress(form.getAddress());
        user.setRole(form.getRole());

        // ✅ Xử lý password
        if (isNew) {
            if (form.getPasswordHash() == null || form.getPasswordHash().isBlank()) {
                throw new RuntimeException("Vui lòng nhập mật khẩu cho người dùng mới!");
            }
            user.setPasswordHash(encoder.encode(form.getPasswordHash()));
        } else if (newPassword != null && !newPassword.isBlank()) {
            user.setPasswordHash(encoder.encode(newPassword));
        }

        // ✅ Lưu vào DB
        userRepo.save(user);

        // ✅ GHI LOG ĐƠN GIẢN
        try {
            String action = isNew ? "CREATE_USER" : "UPDATE_USER";
            String details = (isNew ? "Tạo tài khoản mới: " : "Cập nhật tài khoản: ")
                    + user.getEmail() + " (" + user.getFullName() + ")";

            auditLogService.logWithCurrentUser(action, details, request);
        } catch (Exception e) {
            System.out.println("⚠️ Ghi log thất bại: " + e.getMessage());
        }

        // ✅ Nếu user mới → tạo giỏ hàng
        if (isNew && !cartRepository.existsByUser(user)) {
            Cart cart = new Cart();
            cart.setUser(user);
            cartRepository.save(cart);
        }

        // ✅ Đồng bộ sang bảng managers
        if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.MANAGER) {
            var existingManager = managerRepository.findAll().stream()
                    .filter(m -> m.getUser().getId().equals(user.getId()))
                    .findFirst();

            if (existingManager.isEmpty()) {
                Manager manager = new Manager();
                manager.setUser(user);
                manager.setFullName(user.getFullName());
                manager.setNote("Tạo tự động cho tài khoản có role " + user.getRole().name());
                manager.setActive(true);
                manager.setCreatedAt(LocalDateTime.now());
                managerRepository.save(manager);
            } else {
                var manager = existingManager.get();
                manager.setFullName(user.getFullName());
                manager.setActive(true);
                managerRepository.save(manager);
            }
        } else {
            // Xóa khỏi manager nếu không còn là admin/manager
            managerRepository.findAll().stream()
                    .filter(m -> m.getUser().getId().equals(user.getId()))
                    .findFirst()
                    .ifPresent(managerRepository::delete);
        }

        return "redirect:/admin/users";
    }

    // ✅ Xoá user - GHI LOG
    // ✅ Xoá user - GHI LOG (Fixed version)
    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable Integer id, HttpServletRequest request) {
        var user = userRepo.findById(id).orElse(null);
        if (user != null) {
            String userEmail = user.getEmail();
            String userName = user.getFullName();

            // ✅ XÓA MANAGER TRƯỚC (nếu có)
            managerRepository.findAll().stream()
                    .filter(m -> m.getUser() != null && m.getUser().getId().equals(user.getId()))
                    .findFirst()
                    .ifPresent(manager -> managerRepository.delete(manager));

            // ✅ XÓA CART TRƯỚC (nếu có)
            cartRepository.findAll().stream()
                    .filter(c -> c.getUser() != null && c.getUser().getId().equals(user.getId()))
                    .forEach(cart -> cartRepository.delete(cart));

            // ✅ SAU ĐÓ MỚI XÓA USER
            userRepo.delete(user);

            // Ghi log xóa
            try {
                auditLogService.logWithCurrentUser(
                        "DELETE_USER",
                        "Xóa tài khoản: " + userEmail + " (" + userName + ")",
                        request
                );
            } catch (Exception e) {
                System.out.println("⚠️ Ghi log xóa thất bại: " + e.getMessage());
            }
        }

        return "redirect:/admin/users";
    }
}