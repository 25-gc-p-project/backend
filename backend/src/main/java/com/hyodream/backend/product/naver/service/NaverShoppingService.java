package com.hyodream.backend.product.naver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyodream.backend.product.domain.Product;
import com.hyodream.backend.product.naver.dto.NaverShopItemDto;
import com.hyodream.backend.product.naver.dto.NaverShopSearchResponse;
import com.hyodream.backend.product.repository.ProductRepository;
import com.hyodream.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverShoppingService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Value("${naver.client-id}")
    private String clientId;

    @Value("${naver.client-secret}")
    private String clientSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 알러지별로 제외할 키워드들 (법적 19종 기준)
    private static final Map<String, List<String>> ALLERGEN_KEYWORDS = new HashMap<>();

    static {
        // 난류/유제품
        ALLERGEN_KEYWORDS.put("egg", List.of("계란", "달걀", "난류", "egg", "난백", "난황"));
        ALLERGEN_KEYWORDS.put("milk", List.of("우유", "milk", "유당", "버터", "치즈", "요거트", "크림", "분유", "유청"));

        // 곡류/견과류
        ALLERGEN_KEYWORDS.put("buckwheat", List.of("메밀", "buckwheat"));
        ALLERGEN_KEYWORDS.put("wheat", List.of("밀", "밀가루", "wheat", "글루텐", "소맥"));
        ALLERGEN_KEYWORDS.put("soy", List.of("대두", "콩", "soy", "두유", "간장", "된장", "청국장", "두부"));
        ALLERGEN_KEYWORDS.put("peanut", List.of("땅콩", "peanut", "피넛"));
        ALLERGEN_KEYWORDS.put("walnut", List.of("호두", "walnut"));
        ALLERGEN_KEYWORDS.put("pine_nut", List.of("잣", "pine nut"));

        // 해산물
        ALLERGEN_KEYWORDS.put("mackerel", List.of("고등어", "mackerel"));
        ALLERGEN_KEYWORDS.put("crab", List.of("게", "crab", "꽃게", "대게"));
        ALLERGEN_KEYWORDS.put("shrimp", List.of("새우", "shrimp", "대하"));
        ALLERGEN_KEYWORDS.put("squid", List.of("오징어", "squid"));
        ALLERGEN_KEYWORDS.put("shellfish", List.of("조개", "굴", "전복", "홍합", "shellfish", "바지락", "가리비"));

        // 육류
        ALLERGEN_KEYWORDS.put("pork", List.of("돼지고기", "pork", "돈육", "베이컨", "햄", "소시지"));
        ALLERGEN_KEYWORDS.put("beef", List.of("쇠고기", "소고기", "beef", "우육"));
        ALLERGEN_KEYWORDS.put("chicken", List.of("닭고기", "chicken", "계육", "치킨"));

        // 과일/채소
        ALLERGEN_KEYWORDS.put("peach", List.of("복숭아", "peach"));
        ALLERGEN_KEYWORDS.put("tomato", List.of("토마토", "tomato"));

        // 기타
        ALLERGEN_KEYWORDS.put("sulfite", List.of("아황산", "sulfite", "와인", "건조과일"));
    }

    /**
     * 네이버 쇼핑 검색 결과를 가져와서 자체 DB에 저장 (Import)
     * - 로그인 유저: 내 알러지 정보로 위험 상품 필터링 (저장 X, 반환 X)
     * - 비로그인 유저: 필터링 없이 모두 저장 및 반환
     */
    @Transactional
    public List<Product> importNaverProducts(String query) throws Exception {
        // 0. 로그인 여부 확인 및 알러지 목록 준비
        Set<String> myAllergies = new HashSet<>();
        boolean isLogin = false;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            userRepository.findByUsername(username).ifPresent(user -> {
                user.getAllergies().forEach(ua -> myAllergies.add(ua.getAllergy().getName()));
            });
            isLogin = true;
        }

        // 1. 네이버 API 호출
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://openapi.naver.com/v1/search/shop.json"
                + "?query=" + encodedQuery
                + "&display=20";

        log.info("Requesting Naver Shop API: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.error("Failed to send request to Naver API", e);
            throw e;
        }

        if (response.statusCode() != 200) {
            log.error("Naver API error: status={}, body={}", response.statusCode(), response.body());
            throw new RuntimeException("Naver API error: " + response.body());
        }

        // log.debug("Naver API response body: {}", response.body()); // 너무 길어서 생략 가능

        NaverShopSearchResponse raw;
        try {
            raw = objectMapper.readValue(response.body(), NaverShopSearchResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse Naver API response: {}", response.body(), e);
            throw e;
        }

        List<NaverShopItemDto> items = raw.getItems();

        if (items == null || items.isEmpty()) {
            log.warn("Naver API returned 0 items for query: {}", query);
            return Collections.emptyList();
        }

        log.info("Naver API returned {} items", items.size());

        // 2. DTO -> Entity 변환 및 알러지 분석 후 저장
        List<Product> savedProducts = new ArrayList<>();

        for (NaverShopItemDto item : items) {
            String name = stripHtml(item.getTitle());

            // 3. 알러지 성분 분석 (자동 태깅)
            List<String> detectedAllergens = extractAllergens(item);

            // [핵심] 로그인 유저라면, 위험한 상품은 아예 저장도 안 하고 결과에서도 뺌
            if (isLogin) {
                boolean isDangerous = detectedAllergens.stream()
                        .anyMatch(myAllergies::contains);

                if (isDangerous) {
                    log.info("Filtered out dangerous product for user: {} (Contains: {})", name, detectedAllergens);
                    continue;
                }
            }

            // 중복 저장 방지 (이름 기준)
            // 이미 존재하는 상품이라면 DB에서 가져와서 결과 리스트에 추가 (사용자에게 보여주기 위함)
            if (productRepository.existsByName(name)) {
                // 이름으로 찾아서 추가 (여기선 findByName이 없으므로 findByNameContaining 등으로 대체하거나 넘어가야 함)
                // 현재 Repository에 findByName이 없으므로, 검색 품질을 위해 일단 넘어감.
                // (만약 이미 저장된 상품을 꼭 보여주고 싶다면 findByName 추가 필요)
                continue;
            }

            Product product = new Product();
            product.setName(name);
            product.setPrice(Integer.parseInt(item.getLprice()));
            product.setImageUrl(item.getImage());

            // 상세 정보 조합
            StringBuilder desc = new StringBuilder();
            if (item.getBrand() != null && !item.getBrand().isEmpty()) desc.append("Brand: ").append(item.getBrand()).append("\n");
            if (item.getMaker() != null && !item.getMaker().isEmpty()) desc.append("Maker: ").append(item.getMaker()).append("\n");
            if (item.getCategory1() != null) desc.append("Category: ").append(item.getCategory1()).append(" > ").append(item.getCategory2()).append("\n");
            product.setDescription(desc.toString());

            product.setAllergens(detectedAllergens);

            savedProducts.add(productRepository.save(product));
        }

        log.info("Imported {} products from Naver for query '{}' (Login: {})", savedProducts.size(), query, isLogin);
        return savedProducts;
    }

    /**
     * 상품 정보 텍스트를 분석하여 포함된 알러지 유발 물질 목록 반환
     */
    private List<String> extractAllergens(NaverShopItemDto item) {
        Set<String> detected = new HashSet<>();

        // 검색 대상 텍스트 조합
        StringBuilder sb = new StringBuilder();
        if (item.getTitle() != null) sb.append(stripHtml(item.getTitle())).append(" ");
        if (item.getBrand() != null) sb.append(item.getBrand()).append(" ");
        if (item.getMaker() != null) sb.append(item.getMaker()).append(" ");
        if (item.getCategory1() != null) sb.append(item.getCategory1()).append(" ");
        // 카테고리 정보 등도 포함해서 검사

        String text = sb.toString().toLowerCase(Locale.KOREAN);

        // 모든 알러지 키워드 순회
        for (Map.Entry<String, List<String>> entry : ALLERGEN_KEYWORDS.entrySet()) {
            String allergenKey = entry.getKey(); // egg, milk ...
            List<String> keywords = entry.getValue();

            for (String keyword : keywords) {
                if (text.contains(keyword.toLowerCase(Locale.KOREAN))) {
                    // 키워드 발견 시, 사용자에게 보여줄 이름(한글 대표명)으로 변환해서 저장
                    // 여기선 편의상 맵의 키('egg') 대신 대표 키워드(리스트의 0번째, '계란')를 저장하거나
                    // DataInit과 맞춘 한글 명칭으로 변환해주면 베스트.
                    // 현재 DataInit: "난류(달걀)", "우유" ...
                    
                    String koreanName = mapKeyToKoreanName(allergenKey);
                    detected.add(koreanName);
                    break; // 해당 알러지는 확인됐으므로 다음 알러지로 넘어감
                }
            }
        }
        return new ArrayList<>(detected);
    }
    
    // API Key -> DataInit에 저장된 한글 명칭 매핑
    private String mapKeyToKoreanName(String key) {
        switch (key) {
            case "egg": return "난류(달걀)";
            case "milk": return "우유";
            case "buckwheat": return "메밀";
            case "wheat": return "밀";
            case "soy": return "대두";
            case "peanut": return "땅콩";
            case "walnut": return "호두";
            case "pine_nut": return "잣";
            case "mackerel": return "고등어";
            case "crab": return "게";
            case "shrimp": return "새우";
            case "squid": return "오징어";
            case "shellfish": return "조개류";
            case "pork": return "돼지고기";
            case "beef": return "쇠고기";
            case "chicken": return "닭고기";
            case "peach": return "복숭아";
            case "tomato": return "토마토";
            case "sulfite": return "아황산류";
            default: return key;
        }
    }

    /**
     * HTML 태그 제거
     */
    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "");
    }
}