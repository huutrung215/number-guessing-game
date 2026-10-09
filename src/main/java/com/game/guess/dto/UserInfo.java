// UserInfo.java
package com.game.guess.dto;
import lombok.*;

@Getter @Setter @AllArgsConstructor
public class UserInfo {
    private String username;
    private int score;
    private int turns;
}