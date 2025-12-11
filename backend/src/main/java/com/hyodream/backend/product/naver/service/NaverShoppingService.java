package com.hyodream.backend.product.naver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyodream.backend.product.domain.Product;
import com.hyodream.backend.product.domain.ProductStatus;
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

    // ==========================================
    // [통합] BenefitUtils 로직 시작
    // ==========================================

    // 기대효과별 매핑 키워드
    private static final Map<String, List<String>> BENEFIT_KEYWORDS = new HashMap<>();

    static {
        BENEFIT_KEYWORDS.put("면역력 강화", List.of("면역", "아연", "비타민C", "프로폴리스", "홍삼", "알로에", "상황버섯", "로얄젤리"));
        BENEFIT_KEYWORDS.put("피로 회복", List.of("피로", "비타민B", "간", "밀크씨슬", "타우린", "에너지", "활력", "헛개", "베개", "족욕기", "안마기",
                "마사지", "입욕제", "반신욕", "매트리스"));
        BENEFIT_KEYWORDS.put("관절/뼈 건강", List.of("관절", "뼈", "칼슘", "마그네슘", "MSM", "비타민D", "글루코사민", "상어연골", "초록입홍합",
                "보스웰리아", "보호대", "지팡이", "보행기", "찜질기", "파스"));
        BENEFIT_KEYWORDS.put("눈 건강",
                List.of("눈", "루테인", "지아잔틴", "오메가3", "아스타잔틴", "빌베리", "안구", "돋보기", "블루라이트", "온열안대", "눈마사지기"));
        BENEFIT_KEYWORDS.put("기억력 개선",
                List.of("기억력", "두뇌", "은행잎", "징코", "오메가3", "뇌", "스도쿠", "퍼즐", "화투", "바둑", "장기", "큐브", "보드게임"));
        BENEFIT_KEYWORDS.put("혈행 개선",
                List.of("혈행", "오메가3", "코엔자임Q10", "혈액순환", "감마리놀렌산", "혈압", "지압", "압박스타킹", "스트레칭", "혈압계"));
        BENEFIT_KEYWORDS.put("장 건강", List.of("유산균", "장", "변비", "프로바이오틱스", "프리바이오틱스", "식이섬유", "알로에", "좌욕기", "배찜질기"));
    }

    /**
     * 텍스트에서 기대효과 키워드 추출
     */
    public static List<String> extractBenefits(String text) {
        if (text == null || text.isEmpty())
            return Collections.emptyList();

        Set<String> detected = new HashSet<>();
        String lowerText = text.toLowerCase(Locale.KOREAN);

        for (Map.Entry<String, List<String>> entry : BENEFIT_KEYWORDS.entrySet()) {
            String benefitName = entry.getKey();
            for (String keyword : entry.getValue()) {
                if (lowerText.contains(keyword.toLowerCase(Locale.KOREAN))) {
                    detected.add(benefitName);
                    break;
                }
            }
        }
        return new ArrayList<>(detected);
    }

    /**
     * [EventController에서 사용]
     * 특정 단어(예: 카테고리명)가 속하는 대표 효능 하나를 반환
     * public static으로 선언하여 외부에서 유틸리티처럼 사용 가능하게 함
     */
    public static String findPrimaryBenefit(String keyword) {
        if (keyword == null || keyword.isEmpty())
            return null;
        String lowerKeyword = keyword.toLowerCase(Locale.KOREAN);

        for (Map.Entry<String, List<String>> entry : BENEFIT_KEYWORDS.entrySet()) {
            for (String key : entry.getValue()) {
                if (lowerKeyword.contains(key.toLowerCase(Locale.KOREAN)) ||
                        key.toLowerCase(Locale.KOREAN).contains(lowerKeyword)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
    // ==========================================
    // [통합] BenefitUtils 로직 끝
    // ==========================================

    // 알러지별로 제외할 키워드들
    private static final Map<String, List<String>> ALLERGEN_KEYWORDS = new HashMap<>();
    static {
        ALLERGEN_KEYWORDS.put("egg", List.of("계란", "달걀", "난류", "egg", "난백", "난황"));
        ALLERGEN_KEYWORDS.put("milk", List.of("우유", "milk", "유당", "버터", "치즈", "요거트", "크림", "분유", "유청"));
        ALLERGEN_KEYWORDS.put("buckwheat", List.of("메밀", "buckwheat"));
        ALLERGEN_KEYWORDS.put("wheat", List.of("밀", "밀가루", "wheat", "글루텐", "소맥"));
        ALLERGEN_KEYWORDS.put("soy", List.of("대두", "콩", "soy", "두유", "간장", "된장", "청국장", "두부"));
        ALLERGEN_KEYWORDS.put("peanut", List.of("땅콩", "peanut", "피넛"));
        ALLERGEN_KEYWORDS.put("walnut", List.of("호두", "walnut"));
        ALLERGEN_KEYWORDS.put("pine_nut", List.of("잣", "pine nut"));
        ALLERGEN_KEYWORDS.put("mackerel", List.of("고등어", "mackerel"));
        ALLERGEN_KEYWORDS.put("crab", List.of("게", "crab", "꽃게", "대게"));
        ALLERGEN_KEYWORDS.put("shrimp", List.of("새우", "shrimp", "대하"));
        ALLERGEN_KEYWORDS.put("squid", List.of("오징어", "squid"));
        ALLERGEN_KEYWORDS.put("shellfish", List.of("조개", "굴", "전복", "홍합", "shellfish", "바지락", "가리비"));
        ALLERGEN_KEYWORDS.put("pork", List.of("돼지고기", "pork", "돈육", "베이컨", "햄", "소시지"));
        ALLERGEN_KEYWORDS.put("beef", List.of("쇠고기", "소고기", "beef", "우육"));
        ALLERGEN_KEYWORDS.put("chicken", List.of("닭고기", "chicken", "계육", "치킨"));
        ALLERGEN_KEYWORDS.put("peach", List.of("복숭아", "peach"));
        ALLERGEN_KEYWORDS.put("tomato", List.of("토마토", "tomato"));
        ALLERGEN_KEYWORDS.put("sulfite", List.of("아황산", "sulfite", "와인", "건조과일"));
    }

    @Transactional
    public List<Product> importNaverProducts(String query) throws Exception {
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

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://openapi.naver.com/v1/search/shop.json?query=" + encodedQuery + "&display=20";

        log.info("Requesting Naver Shop API: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Naver API error: " + response.body());
        }

        NaverShopSearchResponse raw = objectMapper.readValue(response.body(), NaverShopSearchResponse.class);
        List<NaverShopItemDto> items = raw.getItems();

        if (items == null || items.isEmpty())
            return Collections.emptyList();

        List<Product> savedProducts = new ArrayList<>();

        for (NaverShopItemDto item : items) {
            if (item.getLink() == null || !item.getLink().contains("smartstore"))
                continue;

            String name = stripHtml(item.getTitle());
            String naverId = item.getProductId();

            List<String> detectedAllergens = extractAllergens(item);
            // 내부 메서드(통합된 로직) 호출
            List<String> detectedBenefits = extractBenefitsInternal(item);

            if (isLogin) {
                boolean isDangerous = detectedAllergens.stream().anyMatch(myAllergies::contains);
                if (isDangerous)
                    continue;
            }

            Product product = productRepository.findByNaverProductId(naverId).orElse(new Product());

            if (product.getId() == null) {
                product.setNaverProductId(naverId);
                product.setTotalSales(0);
                product.setRecentSales(0);
            }

            product.setName(name);
            product.setPrice(Integer.parseInt(item.getLprice()));
            product.setImageUrl(item.getImage());
            product.setItemUrl(item.getLink());
            product.setStatus(ProductStatus.ON_SALE);
            product.setBrand(item.getBrand());
            product.setMaker(item.getMaker());
            product.setCategory1(item.getCategory1());
            product.setCategory2(item.getCategory2());
            product.setCategory3(item.getCategory3());
            product.setCategory4(item.getCategory4());

            StringBuilder desc = new StringBuilder();
            if (item.getBrand() != null && !item.getBrand().isEmpty())
                desc.append("Brand: ").append(item.getBrand()).append("\n");
            if (item.getMaker() != null && !item.getMaker().isEmpty())
                desc.append("Maker: ").append(item.getMaker()).append("\n");
            if (item.getCategory1() != null)
                desc.append("Category: ").append(item.getCategory1()).append(" > ").append(item.getCategory2())
                        .append("\n");
            product.setDescription(desc.toString());

            product.setAllergens(detectedAllergens);
            product.setHealthBenefits(detectedBenefits);

            savedProducts.add(productRepository.save(product));
        }
        return savedProducts;
    }

    // DTO 기반 Benefit 추출 (내부용)
    private List<String> extractBenefitsInternal(NaverShopItemDto item) {
        StringBuilder sb = new StringBuilder();
        if (item.getTitle() != null)
            sb.append(stripHtml(item.getTitle())).append(" ");
        if (item.getCategory1() != null)
            sb.append(item.getCategory1()).append(" ");

        // 통합된 extractBenefits 호출
        return extractBenefits(sb.toString());
    }

    private List<String> extractAllergens(NaverShopItemDto item) {
        Set<String> detected = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        if (item.getTitle() != null)
            sb.append(stripHtml(item.getTitle())).append(" ");
        if (item.getBrand() != null)
            sb.append(item.getBrand()).append(" ");
        if (item.getMaker() != null)
            sb.append(item.getMaker()).append(" ");
        if (item.getCategory1() != null)
            sb.append(item.getCategory1()).append(" ");

        String text = sb.toString().toLowerCase(Locale.KOREAN);

        for (Map.Entry<String, List<String>> entry : ALLERGEN_KEYWORDS.entrySet()) {
            String allergenKey = entry.getKey();
            List<String> keywords = entry.getValue();

            for (String keyword : keywords) {
                if (text.contains(keyword.toLowerCase(Locale.KOREAN))) {
                    detected.add(mapKeyToKoreanName(allergenKey));
                    break;
                }
            }
        }
        return new ArrayList<>(detected);
    }

    private String mapKeyToKoreanName(String key) {
        switch (key) {
            case "egg":
                return "난류(달걀)";
            case "milk":
                return "우유";
            case "buckwheat":
                return "메밀";
            case "wheat":
                return "밀";
            case "soy":
                return "대두";
            case "peanut":
                return "땅콩";
            case "walnut":
                return "호두";
            case "pine_nut":
                return "잣";
            case "mackerel":
                return "고등어";
            case "crab":
                return "게";
            case "shrimp":
                return "새우";
            case "squid":
                return "오징어";
            case "shellfish":
                return "조개류";
            case "pork":
                return "돼지고기";
            case "beef":
                return "쇠고기";
            case "chicken":
                return "닭고기";
            case "peach":
                return "복숭아";
            case "tomato":
                return "토마토";
            case "sulfite":
                return "아황산류";
            default:
                return key;
        }
    }

    private String stripHtml(String html) {
        if (html == null)
            return "";
        return html.replaceAll("<[^>]*>", "");
    }
}