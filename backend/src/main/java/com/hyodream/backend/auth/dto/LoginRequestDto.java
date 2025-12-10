package com.hyodream.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDto {
    @Schema(description = "사용자 아이디", example = "hong123")
    private String username;

    @Schema(description = "비밀번호", example = "password123!")
    private String password;
}