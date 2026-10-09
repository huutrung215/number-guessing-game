// LeaderboardEntry.java
package com.game.guess.dto;
import lombok.*;

@Getter @Setter @AllArgsConstructor
public class LeaderboardEntry {
    private int rank;
    private String username;
    private int score;
}