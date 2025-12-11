package com.hyodream.backend.user.service;

import com.hyodream.backend.auth.dto.SignupRequestDto;
import com.hyodream.backend.user.domain.Address;
import com.hyodream.backend.user.domain.Allergy;
import com.hyodream.backend.user.domain.Disease;
import com.hyodream.backend.user.domain.HealthGoal;
import com.hyodream.backend.user.domain.User;
import com.hyodream.backend.user.domain.UserAllergy;
import com.hyodream.backend.user.domain.UserDisease;
import com.hyodream.backend.user.domain.UserHealthGoal;
import com.hyodream.backend.user.dto.HealthInfoRequestDto;
import com.hyodream.backend.user.repository.AllergyRepository;
import com.hyodream.backend.user.repository.DiseaseRepository;
import com.hyodream.backend.user.repository.HealthGoalRepository;
import com.hyodream.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DiseaseRepository diseaseRepository;
    private final AllergyRepository allergyRepository;
    private final HealthGoalRepository healthGoalRepository;

    /**
     * 현재 로그인한 사용자 정보를 반환 (SecurityContextHolder 이용)
     */
    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("로그인된 사용자를 찾을 수 없습니다."));
    }

    // 건강 정보 저장/수정
    @Transactional
    public void updateHealthInfo(String username, HealthInfoRequestDto dto) {
        log.info("🏥 Updating health info for user: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 지병 처리
        user.getDiseases().clear();
        if (dto.getDiseaseNames() != null) {
            log.info("🏥 Processing diseases: {}", dto.getDiseaseNames());
            for (String name : dto.getDiseaseNames()) {
                Disease d = diseaseRepository.findByName(name)
                        .orElseThrow(() -> new RuntimeException("지병을 찾을 수 없음: " + name));
                user.addDisease(UserDisease.createUserDisease(d));
            }
        }

        // 알레르기 처리
        user.getAllergies().clear();
        if (dto.getAllergyNames() != null) {
            log.info("🥕 Processing allergies: {}", dto.getAllergyNames());
            for (String name : dto.getAllergyNames()) {
                Allergy a = allergyRepository.findByName(name)
                        .orElseThrow(() -> new RuntimeException("알레르기를 찾을 수 없음: " + name));
                log.info("   -> Found allergy entity: {}", a.getName());
                
                UserAllergy ua = UserAllergy.createUserAllergy(a);
                user.addAllergy(ua);
            }
        }

        // 기대효과 처리
        user.getHealthGoals().clear();
        if (dto.getHealthGoalNames() != null) {
            log.info("🎯 Processing health goals: {}", dto.getHealthGoalNames());
            for (String name : dto.getHealthGoalNames()) {
                HealthGoal h = healthGoalRepository.findByName(name)
                        .orElseThrow(() -> new RuntimeException("기대효과를 찾을 수 없음: " + name));
                user.addHealthGoal(UserHealthGoal.createUserHealthGoal(h));
            }
        }
        
        User savedUser = userRepository.saveAndFlush(user); // 변경 사항 즉시 DB 반영
        log.info("✅ Health info updated. Allergies: {}, Diseases: {}, Goals: {}", 
                savedUser.getAllergies().size(), savedUser.getDiseases().size(), savedUser.getHealthGoals().size());
    }

    // 내 정보 조회 (컨트롤러에서 필요해서 추가)
    @Transactional(readOnly = true)
    public User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));
    }

    // 회원 탈퇴
    @Transactional
    public void deleteUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));
        userRepository.delete(user);
    }

    // 프로필 수정 (수정됨: 모든 필드 업데이트)
    @Transactional
    public void updateProfile(String username, SignupRequestDto dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        // 기본 정보 수정 (값이 있을 때만 변경)
        if (dto.getName() != null)
            user.setName(dto.getName());
        if (dto.getPhone() != null)
            user.setPhone(dto.getPhone());
        if (dto.getBirthDate() != null)
            user.setBirthDate(dto.getBirthDate());

        // 주소 정보 수정 (하나라도 들어오면 업데이트)
        if (dto.getCity() != null || dto.getStreet() != null || dto.getZipcode() != null) {
            // 기존 주소 가져오기 (null 방지)
            Address currentAddress = user.getAddress();
            String city = (currentAddress != null) ? currentAddress.getCity() : "";
            String street = (currentAddress != null) ? currentAddress.getStreet() : "";
            String zipcode = (currentAddress != null) ? currentAddress.getZipcode() : "";

            // 들어온 값만 덮어쓰기
            if (dto.getCity() != null)
                city = dto.getCity();
            if (dto.getStreet() != null)
                street = dto.getStreet();
            if (dto.getZipcode() != null)
                zipcode = dto.getZipcode();

            // 새 주소 객체로 교체
            user.setAddress(new Address(city, street, zipcode));
        }
    }
}