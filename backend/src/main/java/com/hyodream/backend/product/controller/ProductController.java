package com.hyodream.backend.product.controller;

import com.hyodream.backend.product.dto.ProductRequestDto;
import com.hyodream.backend.product.dto.ProductResponseDto;
import com.hyodream.backend.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 상품 등록 (관리자용 - 일단 누구나 쓸 수 있게 열어둠 or 토큰 필요)
    @PostMapping
    public ResponseEntity<String> createProduct(@RequestBody ProductRequestDto dto) {
        productService.createProduct(dto);
        return ResponseEntity.ok("상품 등록 완료!");
    }

    // 전체 목록 조회
    // 사용법: GET /api/products?page=0&size=10
    // 헤더: X-Session-Id (비로그인 유저 식별용)
    @GetMapping
    public ResponseEntity<Page<ProductResponseDto>> getAllProducts(
            @RequestParam(defaultValue = "0") int page, // 안 보내면 0페이지(처음)
            @RequestParam(defaultValue = "10") int size, // 안 보내면 10개씩
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            Authentication auth // 로그인 여부 확인용
    ) {
        // 식별자 결정: 로그인했으면 ID, 아니면 세션ID
        String identifier = (auth != null && auth.isAuthenticated()) ? auth.getName() : sessionId;
        if (identifier == null)
            identifier = "unknown";

        return ResponseEntity.ok(productService.getAllProducts(page, size, identifier));
    }

    // 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    // AI 맞춤 추천 상품 조회
    // GET http://localhost:8080/api/products/recommend
    // 헤더: Authorization: Bearer {토큰}
    // 4. AI 추천 (로그인 필수)
    @GetMapping("/recommend")
    public ResponseEntity<List<ProductResponseDto>> getRecommendedProducts(Authentication authentication) {
        // 로그인 유저는 username을 식별자로 사용
        return ResponseEntity.ok(productService.getRecommendedProducts(authentication.getName()));
    }

    // 사용법: GET /api/products/search?keyword=관절&page=0&size=10
    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponseDto>> searchProducts(
            @RequestParam("keyword") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productService.searchProducts(keyword, page, size));
    }
}