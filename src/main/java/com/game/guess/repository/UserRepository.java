package com.game.guess.repository;

import com.game.guess.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    // Top 10 leaderboard, chỉ lấy dữ liệu cần thiết → nhanh
    @Query("SELECT u FROM User u ORDER BY u.score DESC, u.id ASC")
    List<User> findTopScores(Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.turns = u.turns - 1 WHERE u.id = :id AND u.turns > 0")
    int decrementTurn(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.score = u.score + 1 WHERE u.id = :id")
    int incrementScore(@Param("id") Long id);

    @Modifying
    @Query("UPDATE User u SET u.turns = u.turns + :amount WHERE u.id = :id")
    int addTurns(@Param("id") Long id, @Param("amount") int amount);

    @Modifying
    @Query("UPDATE User u SET u.turns = u.turns + :amount WHERE u.username = :username")
    int addTurnsByUsername(@Param("username") String username, @Param("amount") int amount);
}