package com.hyodream.backend.auth.controller;

import com.hyodream.backend.auth.dto.LoginRequestDto;
import com.hyodream.backend.auth.dto.SignupRequestDto;
import com.hyodream.backend.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Auth API", description = "회원가입, 로그인, 로그아웃 인증 관리")
@RestController
@RequestMapping("/api/auth") // 공통 주소
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다. (기본 ROLE: USER)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "이미 존재하는 아이디")
    })
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequestDto dto) {
        authService.signup(dto);
        return ResponseEntity.ok("회원가입 성공!");
    }

    @Operation(summary = "로그인", description = "아이디/비밀번호로 로그인하고 Access Token(JWT)을 발급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공 (토큰 반환)"),
            @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호 불일치")
    })
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequestDto dto) {
        // 서비스에게 로그인 시키고 Access Token 받아옴
        String accessToken = authService.login(dto.getUsername(), dto.getPassword());

        // JSON 형태로 응답
        // { "accessToken": "eyJh..." }
        Map<String, String> response = new HashMap<>();
        response.put("accessToken", accessToken);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "로그아웃", description = "현재 사용 중인 Access Token을 블랙리스트에 추가하여 무효화합니다.")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        // "Bearer " 제거하고 순수 토큰만 추출
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            authService.logout(accessToken);
            return ResponseEntity.ok("로그아웃 되었습니다.");
        } else {
            throw new RuntimeException("토큰이 없습니다.");
        }
    }
}