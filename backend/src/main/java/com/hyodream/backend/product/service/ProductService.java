package com.hyodream.backend.product.service;

import com.hyodream.backend.product.domain.Product;
import com.hyodream.backend.product.dto.ProductRequestDto;
import com.hyodream.backend.product.dto.ProductResponseDto;
import com.hyodream.backend.product.repository.ProductRepository;
import com.hyodream.backend.global.client.AiClient;

import com.hyodream.backend.user.domain.User;
import com.hyodream.backend.user.repository.UserRepository;
import com.hyodream.backend.user.dto.HealthInfoRequestDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final AiClient aiClient;

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    // 상품 등록 (관리자용 - 나중에 쿠팡 API로 대체될 부분)
    @Transactional
    public void createProduct(ProductRequestDto dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setDescription(dto.getDescription());
        product.setImageUrl(dto.getImageUrl());
        product.setVolume(dto.getVolume());
        product.setSizeInfo(dto.getSizeInfo());

        // 효능 태그 저장
        if (dto.getHealthBenefits() != null) {
            for (String benefit : dto.getHealthBenefits()) {
                product.addBenefit(benefit);
            }
        }

        // 알레르기 정보 저장 로직
        if (dto.getAllergens() != null) {
            for (String allergen : dto.getAllergens()) {
                product.addAllergen(allergen); // 엔티티에 만들어둔 메서드 호출
            }
        }

        productRepository.save(product);
    }

    // 전체 상품 목록 조회 (실시간 관심사 반영 정렬)
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getAllProducts(int page, int size, String identifier) {
        Pageable pageable = PageRequest.of(page, size); // 정렬 조건은 쿼리에서 직접 하므로 여기선 뺌

        // A. 변수 초기화
        boolean isLogin = false;
        List<String> userAllergies = new ArrayList<>(); // 알레르기 목록
        String interestCategory = ""; // 관심사 (없으면 빈 문자열)

        // B. 유저 정보 및 관심사 추출
        if (identifier != null && !identifier.equals("unknown")) {
            // 1. 로그인 유저 확인 (identifier가 username인 경우)
            // (컨트롤러에서 명확히 구분해서 넘겨주면 좋지만, 일단 DB 조회 시도)
            userRepository.findByUsername(identifier).ifPresent(user -> {
                // 알레르기 목록 채우기
                user.getAllergies().forEach(ua -> userAllergies.add(ua.getAllergy().getName()));
            });
            if (!userAllergies.isEmpty())
                isLogin = true;

            // 2. Redis 실시간 관심사 확인
            String redisKey = "interest:user:" + identifier;
            Set<String> topInterests = redisTemplate.opsForZSet().reverseRange(redisKey, 0, 0);
            if (topInterests != null && !topInterests.isEmpty()) {
                interestCategory = topInterests.iterator().next();
            }
        }

        // C. 쿼리 실행 (DB가 알아서 정렬하고 필터링해서 Page로 줌)
        // (알레르기가 없으면 빈 리스트를 넘겨야 에러 안 남)
        if (userAllergies.isEmpty())
            userAllergies.add("NONE");

        return productRepository.findAllWithPersonalization(isLogin, userAllergies, interestCategory, pageable)
                .map(ProductResponseDto::new);
    }

    // 상품 상세 조회 (ID로 찾기)
    @Transactional(readOnly = true)
    public ProductResponseDto getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품이 없습니다."));
        return new ProductResponseDto(product);
    }

    // AI + 실시간 하이브리드 추천 (로그인 유저 전용)
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getRecommendedProducts(String username) {
        List<Product> finalProducts = new ArrayList<>();

        // [A] 실시간 행동 기반 추천 (Short-term Interest)
        // 로그인 유저니까 식별자로 username을 사용
        String redisKey = "interest:user:" + username;
        Set<String> topInterests = redisTemplate.opsForZSet().reverseRange(redisKey, 0, 0);

        if (topInterests != null && !topInterests.isEmpty()) {
            String hotCategory = topInterests.iterator().next();
            // 해당 태그 상품 3개 가져오기
            List<Product> realTimePicks = productRepository.findByHealthBenefitsContaining(hotCategory);
            if (realTimePicks.size() > 3)
                realTimePicks = realTimePicks.subList(0, 3);
            finalProducts.addAll(realTimePicks);
        }

        // 2. [AI] 기존 AI 추천 로직
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자 없음"));

            HealthInfoRequestDto requestDto = new HealthInfoRequestDto();
            requestDto.setDiseaseNames(user.getDiseases().stream().map(ud -> ud.getDisease().getName()).toList());
            requestDto.setAllergyNames(user.getAllergies().stream().map(ua -> ua.getAllergy().getName()).toList());
            requestDto.setHealthGoalNames(
                    user.getHealthGoals().stream().map(uh -> uh.getHealthGoal().getName()).toList());

            List<Long> aiProductIds = aiClient.getRecommendations(requestDto);
            List<Product> aiProducts = productRepository.findAllById(aiProductIds);

            finalProducts.addAll(aiProducts); // AI 결과 뒤에 붙이기
        } catch (Exception e) {
            // Fallback: AI 서버가 죽어도 여기서 멈추지 않고, 위에서 담은 [실시간 추천]만이라도 리턴함
            System.out.println("AI 서버 연결 실패 (Fallback 작동): " + e.getMessage());
        }

        // 3. 중복 제거 (혹시 AI랑 실시간이랑 겹칠 수 있으니)
        return finalProducts.stream()
                .distinct()
                .map(ProductResponseDto::new)
                .collect(Collectors.toList());
    }

    // 상품 검색 기능 (이름으로)
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> searchProducts(String keyword, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Page.empty(); // 빈 리스트 대신 빈 페이지 반환
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        return productRepository.findByNameContaining(keyword, pageable)
                .map(ProductResponseDto::new);
    }
}