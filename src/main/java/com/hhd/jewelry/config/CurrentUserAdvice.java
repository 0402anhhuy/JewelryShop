package com.hhd.jewelry.config;

import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentUserAdvice {
    private final UserRepository repo;
    public CurrentUserAdvice(UserRepository repo) { this.repo = repo; }

    @ModelAttribute("currentUser")
    public User currentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return repo.findByEmail(auth.getName()).orElse(null);
    }
}
