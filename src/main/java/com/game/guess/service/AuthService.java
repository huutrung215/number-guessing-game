package com.game.guess.service;

import com.game.guess.dto.*;
import com.game.guess.entity.User;
import com.game.guess.repository.UserRepository;
import com.game.guess.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    @Value("${app.game.start-turns}")
    private int startTurns;

    @Transactional
    public AuthResponse register(AuthRequest req) {
        if (userRepo.findByUsername(req.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }
        User u = User.builder()
                .username(req.getUsername())
                .password(encoder.encode(req.getPassword()))
                .score(0)
                .turns(startTurns)
                .build();
        userRepo.save(u);
        String token = jwtService.generateToken(u.getUsername());
        return new AuthResponse(token, u.getUsername());
    }

    public AuthResponse login(AuthRequest req) {
        User u = userRepo.findByUsername(req.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Sai username/password"));
        if (!encoder.matches(req.getPassword(), u.getPassword())) {
            throw new IllegalArgumentException("Sai username/password");
        }
        String token = jwtService.generateToken(u.getUsername());
        return new AuthResponse(token, u.getUsername());
    }
}