package com.hyodream.backend.user.dto;

import com.hyodream.backend.user.domain.User;
import lombok.Getter;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class UserProfileResponseDto {
    private Long id;
    private String username;
    private String name;
    private String phone;
    private LocalDate birthDate;

    // 주소 정보 (Address가 임베디드라 풀어서 줌)
    private String city;
    private String street;
    private String zipcode;

    // 복잡한 객체 대신 단순 문자열 리스트로 반환 (재귀 방지)
    private List<String> diseases;
    private List<String> allergies;
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