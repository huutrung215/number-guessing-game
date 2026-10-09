// GuessResult.java
package com.game.guess.dto;
import lombok.*;

@Getter @Setter @AllArgsConstructor
public class GuessResult {
    private int serverNumber;
    private int yourGuess;
    private boolean correct;
    private int score;
    private int turnsLeft;
    private String message;
}