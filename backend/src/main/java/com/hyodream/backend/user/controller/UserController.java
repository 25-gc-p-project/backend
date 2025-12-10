package com.hyodream.backend.user.controller;

import com.hyodream.backend.auth.dto.SignupRequestDto;
import com.hyodream.backend.user.domain.User;
import com.hyodream.backend.user.dto.HealthInfoRequestDto;
import com.hyodream.backend.user.dto.UserProfileResponseDto;
import com.hyodream.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User API", description = "회원 정보 조회 및 건강 데이터 관리")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "건강 정보 등록/수정", description = "사용자의 지병, 알레르기, 건강 목표(기대효과) 정보를 업데이트합니다.")
    @PostMapping("/health")
    public ResponseEntity<String> updateHealthInfo(
            @RequestBody HealthInfoRequestDto dto,
            Authentication auth) {
        userService.updateHealthInfo(auth.getName(), dto);
        return ResponseEntity.ok("건강 정보가 저장되었습니다.");
    }

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 프로필(기본정보 + 건강정보)을 조회합니다.")
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponseDto> getMyProfile(Authentication auth) {
        // 유저 엔티티 가져오기
        User user = userService.getUser(auth.getName());

        // DTO로 변환해서 반환 (재귀 끊기)
        return ResponseEntity.ok(new UserProfileResponseDto(user));
    }

    @Operation(summary = "회원 정보 수정", description = "사용자의 기본 정보(이름, 전화번호, 주소 등)를 수정합니다. (비밀번호 제외)")
    @PutMapping("/profile")
    public ResponseEntity<String> updateProfile(@RequestBody SignupRequestDto dto, Authentication auth) {
        userService.updateProfile(auth.getName(), dto);
        return ResponseEntity.ok("회원 정보가 수정되었습니다.");
    }

    @Operation(summary = "회원 탈퇴", description = "사용자 계정과 관련된 모든 데이터를 삭제합니다.")
    @DeleteMapping
    public ResponseEntity<String> deleteUser(Authentication auth) {
        userService.deleteUser(auth.getName());
        return ResponseEntity.ok("회원 탈퇴 처리되었습니다.");
    }
}