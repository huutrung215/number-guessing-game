package com.game.guess.controller;

import com.game.guess.dto.*;
import com.game.guess.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping("/guess")
    public ResponseEntity<GuessResult> guess(Authentication auth,
                                             @RequestBody Map<String, Integer> body) {
        Integer guess = body.get("number");
        if (guess == null) throw new IllegalArgumentException("Thiếu trường 'number'");
        return ResponseEntity.ok(gameService.guess(auth.getName(), guess));
    }

    @PostMapping("/buy-turns")
    public ResponseEntity<UserInfo> buyTurns(Authentication auth) {
        return ResponseEntity.ok(gameService.buyTurns(auth.getName()));
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfo> me(Authentication auth) {
        return ResponseEntity.ok(gameService.me(auth.getName()));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> leaderboard() {
        return ResponseEntity.ok(gameService.leaderboard());
    }
}