package com.hhd.jewelry.config;


import com.hhd.jewelry.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    UserDetailsService userDetailsService(UserRepository repo) {
        return username -> repo.findByEmail(username)
                .map(u -> org.springframework.security.core.userdetails.User
                        .withUsername(u.getEmail())
                        .password(u.getPasswordHash())
                        .roles(u.getRole().name())  // USER / ADMIN...
                        .disabled(false)
                        .build())
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @Bean
    AuthenticationProvider authenticationProvider(UserDetailsService uds, PasswordEncoder encoder) {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(uds);
        p.setPasswordEncoder(encoder);
        return p;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(a -> a
                        .requestMatchers(
                                "/",
                                "/login",            // MỞ RÕ
                                "/register","/register/**", "/forgot-password",
                                "/css/**", "/js/**", "/images/**", "/cart/**",
                                "/vnpay_return"
                        ).permitAll()
                        .requestMatchers("/checkout/**", "/orders/**", "/account/**").authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(f -> f
                        .loginPage("/login")          // GET trả về login.html
                        .loginProcessingUrl("/login") // POST xử lý (khớp form)
                        .defaultSuccessUrl("/", false) // về trang chủ (đã permit)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(l -> l
                        .logoutUrl("/logout")
                        .deleteCookies("JSESSIONID", "remember-me")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );
        http.rememberMe(r -> r
                .key("pnj-remember-key")              // đặt key riêng
                .tokenValiditySeconds(1209600)        // 14 ngày
                .rememberMeParameter("remember-me")   // khớp name trong form
        );

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
