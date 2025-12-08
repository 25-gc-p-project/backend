package com.hyodream.backend.order.service;

import com.hyodream.backend.order.domain.Cart;
import com.hyodream.backend.order.dto.OrderRequestDto;
import com.hyodream.backend.order.repository.CartRepository;
import com.hyodream.backend.product.domain.Product;
import com.hyodream.backend.product.repository.ProductRepository;
import com.hyodream.backend.user.domain.User;
import com.hyodream.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final UserService userService;
    private final ProductRepository productRepository;

    // 장바구니 담기
    @Transactional
    public void addCart(OrderRequestDto dto) {
        User user = userService.getCurrentUser();

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("상품 없음"));

        Cart cart = cartRepository.findByUserIdAndProductId(user.getId(), product.getId())
                .orElse(null);

        if (cart == null) {
            cart = Cart.createCart(user.getId(), product.getId(), dto.getCount());
            cartRepository.save(cart);
        } else {
            cart.setCount(cart.getCount() + dto.getCount());
        }
    }

    // 내 장바구니 조회
    @Transactional(readOnly = true)
    public List<Cart> getMyCart() {
        User user = userService.getCurrentUser();
        return cartRepository.findByUserId(user.getId());
    }

    // 장바구니 삭제
    @Transactional
    public void deleteCart(Long cartId) {
        cartRepository.deleteById(cartId);
    }
}