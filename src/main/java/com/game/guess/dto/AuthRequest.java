// AuthRequest.java
package com.game.guess.dto;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class AuthRequest {
    @NotBlank @Size(min = 3, max = 50)
    private String username;
    @NotBlank @Size(min = 6, max = 100)
    private String password;
}