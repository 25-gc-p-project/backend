package com.hyodream.backend.order.controller;

import com.hyodream.backend.order.domain.Cart;
import com.hyodream.backend.order.dto.OrderRequestDto;
import com.hyodream.backend.order.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // 장바구니 담기
    @PostMapping
    public ResponseEntity<String> addCart(@RequestBody OrderRequestDto dto) {
        cartService.addCart(dto);
        return ResponseEntity.ok("장바구니에 담겼습니다.");
    }

    // 내 장바구니 조회
    @GetMapping
    public ResponseEntity<List<Cart>> getMyCart() {
        return ResponseEntity.ok(cartService.getMyCart());
    }

    // 장바구니 삭제
    @DeleteMapping("/{cartId}")
    public ResponseEntity<String> deleteCart(@PathVariable Long cartId) {
        cartService.deleteCart(cartId);
        return ResponseEntity.ok("삭제되었습니다.");
    }
}