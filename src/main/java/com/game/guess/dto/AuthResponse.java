// AuthResponse.java
package com.game.guess.dto;
import lombok.*;

@Getter @Setter @AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
}