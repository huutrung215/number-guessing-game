package com.game.guess.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h.frameOptions(f -> f.disable()))
            .authorizeHttpRequests(auth -> auth
                // 1. Static resources
                .requestMatchers("/", "/index.html", "/game.html", "/leaderboard.html").permitAll()
                .requestMatchers("/css/**", "/js/**", "/favicon.ico").permitAll()

                // 2. Auth endpoints
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()

                // 3. Payment callback (VNPAY gọi server-to-server, không có JWT)
                .requestMatchers("/api/payment/vnpay-return", "/api/payment/vnpay-ipn").permitAll()

                // 4. Leaderboard — public nếu muốn ai cũng xem
                .requestMatchers("/api/leaderboard").permitAll()

                // 5. H2 console (chỉ dùng cho dev)
                .requestMatchers("/h2-console/**").permitAll()

                // 6. Tất cả còn lại phải authenticated
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}