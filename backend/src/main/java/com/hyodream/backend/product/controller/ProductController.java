package com.hyodream.backend.product.controller;

import com.hyodream.backend.product.dto.ProductRequestDto;
import com.hyodream.backend.product.dto.ProductResponseDto;
import com.hyodream.backend.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;

import java.util.List;

@Tag(name = "Product API", description = "상품 검색, 조회 및 추천 관련 API")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "상품 수동 등록 (관리자용)", description = "관리자가 상품을 직접 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 입력 값")
    })
    @PostMapping
    public ResponseEntity<String> createProduct(@RequestBody ProductRequestDto dto) {
        productService.createProduct(dto);
        return ResponseEntity.ok("상품 등록 완료!");
    }

    @Operation(summary = "전체 상품 목록 조회", description = "DB에 저장된 모든 상품을 페이징하여 조회합니다. 인기순/최신순 정렬이 가능합니다.\n" +
            "첫 페이지(page=0) 조회 시, 사용자(또는 세션)의 실시간 관심사를 반영한 추천 상품 3개가 최상단에 자동 주입됩니다.")
    @GetMapping
    public ResponseEntity<PagedModel<ProductResponseDto>> getAllProducts(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준 ('latest': 최신순, 'popular': 인기순)") @RequestParam(defaultValue = "latest") String sort,
            @Parameter(description = "비로그인 유저 세션 ID (개인화 추천을 위한 식별자)") @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            Authentication auth) {
        // 식별자 결정: 로그인했으면 ID, 아니면 세션ID
        String identifier = (auth != null && auth.isAuthenticated()) ? auth.getName() : sessionId;
        if (identifier == null)
            identifier = "unknown";

        Page<ProductResponseDto> result = productService.getAllProducts(page, size, sort, identifier);
        return ResponseEntity.ok(new PagedModel<>(result));
    }

    @Operation(summary = "상품 상세 조회 (크롤링 & AI 감성 분석)", description = """
            상품 ID로 상세 정보를 조회합니다. 이 API는 호출 시점에 데이터 최신성을 확인하고 필요한 경우 AI 크롤러와 감성 분석 엔진을 트리거합니다.

            **[상세 정보 갱신 로직 (On-Demand Processing)]**
            1. **데이터 최신성 확인:** 해당 상품의 상세 정보(`ProductDetail`)가 없거나, 마지막 갱신일로부터 **3일**이 지났는지 확인합니다.
            2. **AI 크롤러 호출:** 갱신이 필요하다면 AI 서버에 크롤링을 요청하여 최신 상세 정보(원가, 할인율)와 리뷰 데이터를 수집합니다.
            3. **AI 리뷰 감성 분석 (New):**
               - 수집된 리뷰 텍스트를 AI 모델에 전송하여 **긍정/부정 비율**을 분석합니다.
               - 분석 결과(`positiveRatio`, `negativeRatio`)는 DB에 저장되며, 소비자에게 직관적인 구매 지표로 제공됩니다.
            4. **데이터 동기화:** 크롤링된 정보와 분석 결과를 `ProductDetail` 및 `Review` 테이블에 저장합니다.
            5. **응답 반환:** 최신화된 상세 정보와 감성 분석 결과를 포함한 `ProductResponseDto`를 반환합니다.
            """)
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @Operation(summary = "개인화 맞춤 상품 추천 (섹션별 그룹화)", description = """
            사용자의 상태에 따라 4가지 섹션으로 그룹화된 추천 결과를 반환합니다.
            각 섹션은 `title`(추천 사유)과 `products`(상품 목록)으로 구성됩니다.

            **[응답 구조]**
            - **realTime:** [실시간] 최근 관심사(카테고리/효능) 기반 (최대 4개)
            - **healthGoals:** [건강목표] 사용자가 설정한 목표별 리스트 (목표당 2개)
            - **diseases:** [지병] 같은 지병을 가진 환우들의 선택 (지병당 2개)
            - **ai:** [AI] 종합 분석 결과 (3개 고정)
            """)
    @GetMapping("/recommend")
    public ResponseEntity<com.hyodream.backend.product.dto.RecommendationResponseDto> getRecommendedProducts(
            @Parameter(description = "비로그인 유저 세션 ID") @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            Authentication auth) {
        String identifier;
        boolean isLogin = false;

        // 식별자 결정 (로그인 우선 -> 없으면 세션ID)
        if (auth != null && auth.isAuthenticated()) {
            identifier = auth.getName();
            isLogin = true;
        } else if (sessionId != null) {
            identifier = sessionId;
            isLogin = false;
        } else {
            // 둘 다 없으면 빈 객체 반환
            return ResponseEntity.ok(new com.hyodream.backend.product.dto.RecommendationResponseDto());
        }

        return ResponseEntity.ok(productService.getRecommendedProducts(identifier, isLogin));
    }

    @Operation(summary = "상품 키워드 검색 (통합 검색)", description = """
            키워드로 상품을 검색하고, 네이버 쇼핑 API 결과를 실시간으로 캐싱합니다. (Cache-Aside 패턴 적용)

            **[검색 및 데이터 동기화 로직]**
            1. **Cache-Aside 전략:**
               - 해당 키워드로 검색된 기록(`SearchLog`)이 없거나, 마지막 API 호출(`lastApiCallAt`)로부터 **24시간**이 지났다면 네이버 쇼핑 API를 호출하여 데이터를 최신화합니다.
               - 그 외의 경우(단순 인기 검색어 등)에는 DB에 저장된 데이터를 바로 반환하여 응답 속도를 높입니다.
            2. **데이터 적재 (Light Info):**
               - 검색 목록 노출에 필요한 **기본 정보**(상품명, 가격, 이미지, 브랜드, 카테고리)만 `Product` 테이블에 저장합니다.
               - 무거운 상세 정보(원가, 리뷰 상세 등)는 저장하지 않습니다. (상세 조회 시점에 처리)
            3. **알러지 필터링:** 로그인 회원의 경우, 본인의 알러지 유발 성분이 포함된 상품은 검색 결과 및 DB 저장 과정에서 **자동 제외**됩니다.

            **[자동 갱신 스케줄링]**
            - **검색어 갱신:** 매일 새벽 3시, 3일 이상 API 호출이 없었던 검색어에 대해 자동으로 API를 호출하여 상품 정보를 최신화합니다.
            """)
    @GetMapping("/search")
    public ResponseEntity<PagedModel<ProductResponseDto>> searchProducts(
            @Parameter(description = "검색어 (예: 관절, 루테인)") @RequestParam("keyword") String keyword,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준 ('latest': 최신순, 'popular': 인기순)") @RequestParam(defaultValue = "latest") String sort) {
        Page<ProductResponseDto> result = productService.searchProducts(keyword, page, size, sort);
        return ResponseEntity.ok(new PagedModel<>(result));
    }

    @Operation(summary = "연관 상품 추천", description = "해당 상품을 본 사용자들이 함께 많이 구매한 상품(Collaborative Filtering)을 추천합니다.")
    @GetMapping("/{id}/related")
    public ResponseEntity<List<ProductResponseDto>> getRelatedProducts(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getRelatedProducts(id));
    }
}
