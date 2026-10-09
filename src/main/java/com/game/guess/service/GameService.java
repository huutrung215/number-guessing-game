package com.game.guess.service;

import com.game.guess.dto.*;
import com.game.guess.entity.User;
import com.game.guess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class GameService {

    private final UserRepository userRepo;

    @Value("${app.game.win-rate}")
    private double winRate;

    @Value("${app.game.buy-turns-amount}")
    private int buyTurnsAmount;

    /**
     * Xử lý đoán số - CHỐNG RACE CONDITION:
     * - Sử dụng atomic UPDATE ... WHERE turns > 0 ở tầng DB.
     * - Chỉ khi UPDATE trả về 1 (nghĩa là user còn lượt) mới thực hiện đoán.
     * → 2 request đồng thời không thể cùng tiêu thụ 1 lượt.
     */
    @Transactional
    public GuessResult guess(String username, int guess) {
        if (guess < 1 || guess > 5) {
            throw new IllegalArgumentException("Số đoán phải từ 1-5");
        }

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));

        // Trừ lượt bằng atomic UPDATE (có WHERE turns > 0)
        int affected = userRepo.decrementTurn(user.getId());
        if (affected == 0) {
            throw new IllegalStateException("Bạn đã hết lượt chơi. Hãy mua thêm.");
        }

        int serverNumber = ThreadLocalRandom.current().nextInt(1, 6); // 1..5
        // Điều chỉnh xác suất: winRate% người thắng
        boolean correct = ThreadLocalRandom.current().nextDouble() < winRate;

        // Đảm bảo tính nhất quán: nếu correct → ép serverNumber = guess
        if (correct) {
            serverNumber = guess;
            userRepo.incrementScore(user.getId());
        }

        // Load lại thông tin mới nhất
        User updated = userRepo.findById(user.getId()).orElseThrow();
        return new GuessResult(
                serverNumber,
                guess,
                correct,
                updated.getScore(),
                updated.getTurns(),
                correct ? "🎉 Đoán đúng! +1 điểm" : "❌ Sai rồi, thử lại nhé!"
        );
    }

    @Transactional
    public UserInfo buyTurns(String username) {
        User u = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));
        userRepo.addTurns(u.getId(), buyTurnsAmount);
        User updated = userRepo.findById(u.getId()).orElseThrow();
        return new UserInfo(updated.getUsername(), updated.getScore(), updated.getTurns());
    }

    @Transactional(readOnly = true)
    public UserInfo me(String username) {
        User u = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));
        return new UserInfo(u.getUsername(), u.getScore(), u.getTurns());
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> leaderboard() {
        // Chỉ query top 10 - rất nhanh, có index trên score
        List<User> top = userRepo.findTopScores(PageRequest.of(0, 10));
        return IntStream.range(0, top.size())
                .mapToObj(i -> new LeaderboardEntry(i + 1, top.get(i).getUsername(), top.get(i).getScore()))
                .toList();
    }
}