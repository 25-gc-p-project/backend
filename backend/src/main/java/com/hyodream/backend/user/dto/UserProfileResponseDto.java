package com.hyodream.backend.user.dto;

import com.hyodream.backend.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class UserProfileResponseDto {
    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "사용자 아이디", example = "hong123")
    private String username;

    @Schema(description = "실명", example = "홍길동")
    private String name;

    @Schema(description = "휴대폰 번호", example = "010-1234-5678")
    private String phone;

    @Schema(description = "생년월일", example = "1960-01-01")
    private LocalDate birthDate;

    // 주소 정보 (Address가 임베디드라 풀어서 줌)
    @Schema(description = "시/도", example = "서울시")
    private String city;

    @Schema(description = "도로명 주소", example = "강남대로 123")
    private String street;

    @Schema(description = "우편번호", example = "06000")
    private String zipcode;

    // 복잡한 객체 대신 단순 문자열 리스트로 반환 (재귀 방지)
    @Schema(description = "등록된 지병 목록", example = "[\"당뇨\"]")
    private List<String> diseases;

    @Schema(description = "등록된 알레르기 목록", example = "[\"우유\"]")
    private List<String> allergies;

    @Schema(description = "등록된 건강 목표 목록", example = "[\"면역력 강화\"]")
    private List<String> healthGoals;

    public UserProfileResponseDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.name = user.getName();
        this.phone = user.getPhone();
        this.birthDate = user.getBirthDate();

        if (user.getAddress() != null) {
            this.city = user.getAddress().getCity();
            this.street = user.getAddress().getStreet();
            this.zipcode = user.getAddress().getZipcode();
        }

        // 연관된 엔티티에서 이름만 뽑아내기
        this.diseases = user.getDiseases().stream()
                .map(ud -> ud.getDisease().getName()) // UserDisease -> Disease -> name
                .collect(Collectors.toList());

        this.allergies = user.getAllergies().stream()
                .map(ua -> ua.getAllergy().getName())
                .collect(Collectors.toList());

        this.healthGoals = user.getHealthGoals().stream()
                .map(uh -> uh.getHealthGoal().getName())
                .collect(Collectors.toList());
    }
}