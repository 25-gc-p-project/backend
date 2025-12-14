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

@Tag(name = "User API", description = "회원 정보 및 건강 데이터 관리 API")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "건강 정보 등록/수정", description = """
            사용자의 건강 데이터(지병, 알레르기, 건강 목표)를 업데이트합니다.
            
            **[데이터 처리 방식]**
            - **전체 교체(Replace):** 기존에 등록된 지병, 알레르기, 건강 목표 리스트를 **모두 삭제**하고, 요청받은 데이터로 **새로 등록**합니다.
            - **빈 리스트 처리:** 빈 리스트를 보내면 해당 항목은 초기화(삭제)됩니다.
            """)
    @PostMapping("/health")
    public ResponseEntity<String> updateHealthInfo(
            @RequestBody HealthInfoRequestDto dto,
            Authentication auth) {
        userService.updateHealthInfo(auth.getName(), dto);
        return ResponseEntity.ok("건강 정보가 저장되었습니다.");
    }

    @Operation(summary = "내 정보 조회", description = """
            현재 로그인한 사용자의 상세 프로필 정보를 조회합니다.
            
            **[반환 데이터]**
            - **기본 정보:** 아이디, 이름, 연락처, 생년월일, 주소(시/도, 도로명, 우편번호)
            - **건강 정보:** 등록된 지병, 알레르기, 건강 목표(기대효과)를 문자열 리스트로 반환합니다.
            """)
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponseDto> getMyProfile(Authentication auth) {
        // 유저 엔티티 가져오기
        User user = userService.getUser(auth.getName());

        // DTO로 변환해서 반환 (재귀 끊기)
        return ResponseEntity.ok(new UserProfileResponseDto(user));
    }

    @Operation(summary = "회원 정보 수정", description = """
            사용자의 기본 정보를 부분적으로 수정합니다.
            
            **[수정 가능 항목]**
            - 이름, 전화번호, 생년월일
            - 주소 (시/도, 도로명, 우편번호)
            
            **[로직]**
            - `null`로 입력된 필드는 변경되지 않고 **기존 값을 유지**합니다. (Partial Update)
            - 비밀번호 변경은 별도 API(추후 제공 예정)를 사용해야 합니다.
            """)
    @PutMapping("/profile")
    public ResponseEntity<String> updateProfile(@RequestBody SignupRequestDto dto, Authentication auth) {
        userService.updateProfile(auth.getName(), dto);
        return ResponseEntity.ok("회원 정보가 수정되었습니다.");
    }

    @Operation(summary = "회원 탈퇴", description = """
            사용자 계정을 삭제하고 관련 데이터를 정리합니다.
            
            **[Cascading 삭제 정책]**
            - 사용자 엔티티 삭제 시, 연관된 **건강 정보(지병, 알레르기, 건강목표)**는 자동으로 함께 삭제됩니다.
            - 주문 내역(`Orders`)은 보존되거나 별도 정책에 따라 처리됩니다.
            """)
    @DeleteMapping
    public ResponseEntity<String> deleteUser(Authentication auth) {
        userService.deleteUser(auth.getName());
        return ResponseEntity.ok("회원 탈퇴 처리되었습니다.");
    }
}