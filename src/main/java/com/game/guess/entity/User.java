package com.game.guess.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_score", columnList = "score DESC")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Integer score = 0;

    @Column(nullable = false)
    private Integer turns = 0;

    // Optimistic Locking - chống race condition khi đoán nhiều lần cùng lúc
    @Version
    private Long version;
}