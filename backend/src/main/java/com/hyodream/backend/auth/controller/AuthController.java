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

@Tag(name = "Auth API", description = "회원 인증 및 토큰 관리 API")
@RestController
@RequestMapping("/api/auth") // 공통 주소
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = """
            새로운 사용자를 등록합니다. (기본 권한: ROLE_USER)
            
            **[로직 설명]**
            1. **아이디 중복 체크:** 요청된 `username`이 이미 존재하는지 확인합니다.
            2. **비밀번호 암호화:** BCrypt 알고리즘을 사용하여 비밀번호를 해싱 후 저장합니다.
            3. **주소 정보 저장:** 주소(`Address`) 객체를 생성하여 사용자 정보와 함께 저장합니다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "이미 존재하는 아이디 또는 잘못된 입력값")
    })
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequestDto dto) {
        authService.signup(dto);
        return ResponseEntity.ok("회원가입 성공!");
    }

    @Operation(summary = "로그인", description = """
            아이디/비밀번호를 검증하고 JWT 토큰(Access Token)을 발급합니다.
            
            **[보안 및 세션 처리]**
            1. **검증:** 아이디 존재 여부 및 비밀번호 일치 여부를 확인합니다.
            2. **토큰 발급:**
               - **Access Token:** 유효기간 30분. API 접근 권한 인증용.
               - **Refresh Token:** 유효기간 7일. Redis에 저장되어 Access Token 재발급 시 사용.
            3. **응답:** 생성된 Access Token을 JSON 형태로 반환합니다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공 (Access Token 반환)"),
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

    @Operation(summary = "로그아웃", description = """
            현재 사용자의 로그인 세션을 종료합니다.
            
            **[블랙리스트 처리 로직]**
            1. **Access Token 무효화:** 요청 헤더의 Access Token을 Redis 블랙리스트에 등록합니다. (남은 유효시간만큼 저장)
            2. **Refresh Token 삭제:** Redis에 저장된 해당 유저의 Refresh Token을 영구 삭제하여 재발급을 막습니다.
            """)
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