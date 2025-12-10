package com.hyodream.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class SignupRequestDto {
    @Schema(description = "사용자 아이디", example = "hong123")
    private String username;

    @Schema(description = "비밀번호", example = "password123!")
    private String password;

    @Schema(description = "실명", example = "홍길동")
    private String name;

    @Schema(description = "휴대폰 번호", example = "010-1234-5678")
    private String phone;

    @Schema(description = "생년월일 (YYYY-MM-DD)", example = "1960-01-01")
    private LocalDate birthDate;

    // 주소 정보
    @Schema(description = "시/도", example = "서울시")
    private String city;

    @Schema(description = "도로명 주소", example = "강남대로 123")
    private String street;

    @Schema(description = "우편번호", example = "06000")
    private String zipcode;
}