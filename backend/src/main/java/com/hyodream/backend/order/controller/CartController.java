package com.hyodream.backend.order.controller;

import com.hyodream.backend.order.domain.Cart;
import com.hyodream.backend.order.dto.OrderRequestDto;
import com.hyodream.backend.order.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Cart API", description = "장바구니 관리")
@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "장바구니 담기", description = "상품을 장바구니에 추가합니다. (로그인 필수)")
    @PostMapping
    public ResponseEntity<String> addCart(@RequestBody OrderRequestDto dto) {
        cartService.addCart(dto);
        return ResponseEntity.ok("장바구니에 담겼습니다.");
    }

    @Operation(summary = "내 장바구니 조회", description = "로그인한 사용자의 장바구니 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<Cart>> getMyCart() {
        return ResponseEntity.ok(cartService.getMyCart());
    }

    @Operation(summary = "장바구니 항목 삭제", description = "장바구니 ID로 특정 항목을 삭제합니다.")
    @DeleteMapping("/{cartId}")
    public ResponseEntity<String> deleteCart(
            @Parameter(description = "장바구니 ID") @PathVariable Long cartId) {
        cartService.deleteCart(cartId);
        return ResponseEntity.ok("삭제되었습니다.");
    }
}