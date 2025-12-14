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

    @Operation(summary = "상품 수동 등록 (관리자용)", description = """
            관리자가 상품 정보를 직접 DB에 등록합니다.
            - 주로 크롤링되지 않는 상품이나 기획 상품을 등록할 때 사용합니다.
            - 효능 태그(`healthBenefits`)와 알러지 정보(`allergens`)를 수동으로 입력할 수 있습니다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 입력 값")
    })
    @PostMapping
    public ResponseEntity<String> createProduct(@RequestBody ProductRequestDto dto) {
        productService.createProduct(dto);
        return ResponseEntity.ok("상품 등록 완료!");
    }

    @Operation(summary = "전체 상품 목록 조회", description = """
            DB에 저장된 모든 상품을 페이징하여 조회합니다.
            
            **[개인화 로직: 실시간 관심사 주입]**
            - **Page 0 요청 시:** 사용자(또는 세션)의 Redis 실시간 관심사(`interest:user:{id}`)를 분석하여, 
              관심 카테고리/효능에 해당하는 상품 **3개**를 목록 최상단에 자동으로 주입합니다.
            
            **[정렬 옵션 및 통계 정책]**
            - `latest`: 최신 등록순 (기본값)
            - `popular`: 인기순 (최근 30일 판매량 `recentSales` 기준 내림차순)
               - **실시간 반영:** 주문/취소 발생 시 판매량이 **즉시 집계**되어 인기 순위에 바로 반영됩니다.
               - **정합성 보정 (Scheduler):** 매일 자정(00:00)에 스케줄러가 실행되어, 정확히 '오늘 기준 최근 30일' 간의 판매량을 재계산(Calibration)하여 데이터 정확도를 유지합니다.
            """)
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

    @Operation(summary = "상품 상세 조회 (비동기 AI 리뷰 분석)", description = """
            상품 ID로 상세 정보를 조회합니다. 대기 시간을 최소화하기 위해 **비동기 처리(Async Processing)** 방식을 사용합니다.

            **[상세 정보 갱신 및 AI 분석 프로세스]**
            1. **즉시 응답:** 요청 시 DB에 저장된 상품 정보를 **즉시 반환**합니다. (User Waiting Zero)
            2. **DB 리뷰 기반 AI 분석:** 
               - 크롤링을 수행하지 않고, **현재 DB에 저장된 리뷰 데이터**를 사용하여 AI 감성 분석을 수행합니다.
               - 데이터가 없거나 분석이 필요한 경우(`NONE` or `FAILED`), 백그라운드에서 AI 분석을 트리거하고 상태를 `PROGRESS`로 변경합니다.
            3. **상태 확인 (`analysisStatus`):**
               - `PROGRESS`: AI가 현재 DB의 리뷰를 분석 중입니다.
               - `COMPLETED`: 분석이 완료되어 최신 긍정/부정 비율이 반영되었습니다.
            4. **클라이언트 가이드:** `analysisStatus`가 `PROGRESS`인 경우, 잠시 후(예: 3~5초) 다시 조회(Polling)하면 업데이트된 분석 결과를 확인할 수 있습니다.
            """)
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @Operation(summary = "개인화 맞춤 상품 추천 (Hybrid RAG)", description = """
            사용자의 로그, 건강 상태, 구매 이력을 종합하여 **4가지 섹션**으로 구성된 개인화 추천 결과를 반환합니다.
            **비로그인 사용자**도 세션 ID(`X-Session-Id`)를 기반으로 실시간 추천을 받을 수 있습니다.

            **[추천 섹션 구성 및 특징]**
            1. **realTime (실시간 관심사):**
               - **대상:** 로그인 유저 & **비로그인 유저(세션)**
               - **로직:** Redis에 저장된 최근 클릭/장바구니 이력을 분석하여 관심 카테고리 상품을 추천합니다.
               - **개수:** 최대 **4개**
               - **메시지:** "최근 보신 '{카테고리}' 관련 상품"
            2. **healthGoals (건강 목표):**
               - **대상:** 로그인 유저
               - **로직:** 설정한 건강 목표(예: '면역력 강화') 태그를 가진 상품을 추천합니다.
               - **개수:** 각 목표당 **2개**
               - **메시지:** "고객님의 '{목표}' 관리를 위한 추천"
            3. **diseases (지병별 맞춤 - 협업 필터링):**
               - **대상:** 로그인 유저
               - **로직:** 나와 같은 지병을 가진 다른 사용자들이 많이 구매한 상품을 추천합니다.
               - **개수:** 각 지병당 **2개**
               - **메시지:** "'{지병}' 환우들이 많이 선택한 상품"
            4. **ai (AI 종합 분석):**
               - **대상:** 로그인 유저
               - **로직:** 후보군(인기+신규) 중 건강 정보를 고려하여 AI(GPT)가 선정한 상품.
               - **개수:** **3개** 고정
               - **메시지:** "AI 종합 분석"
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
            네이버 쇼핑 API와 연동하여 상품을 검색하고 관리합니다.

            **[데이터 수집 및 관리 정책 (Scheduler)]**
            1. **Cache-Aside:** 검색 시 DB에 데이터가 없거나 24시간이 지났으면 네이버 API를 호출하여 저장합니다.
            2. **장기 미판매 상품 정리 (Daily):** 마지막 업데이트 후 **30일**이 지난 상품을 정리합니다.
               - **판매 이력 있음:** `STOP_SELLING` (판매 종료) 상태로 변경하여 구매 내역은 보존합니다.
               - **판매 이력 없음:** DB에서 **영구 삭제**하여 데이터 용량을 최적화합니다.
            
            **[검색 로직]**
            - **Light Info 적재:** 목록 노출에 필요한 기본 정보만 빠르게 저장합니다.
            - **알러지 필터링:** 로그인 유저는 본인의 알러지 성분이 포함된 상품이 결과에서 자동 제외됩니다.
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

    @Operation(summary = "연관 상품 추천 (협업 필터링)", description = """
            해당 상품을 주문한 사용자들이 함께 많이 구매한 상품 5개를 추천합니다.
            
            - **데이터 출처:** 주문 상세 내역 (`OrderItems`) + 주문 정보 (`Orders`)
            - **필터링 조건:** 결제 완료 후 **취소되지 않은 정상 주문(`status='ORDER'`)** 만 집계에 포함합니다.
            - **알고리즘:** Item-based Collaborative Filtering (이 상품을 산 사람이 가장 많이 산 다른 상품)
            """)
    @GetMapping("/{id}/related")
    public ResponseEntity<List<ProductResponseDto>> getRelatedProducts(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getRelatedProducts(id));
    }
}