package com.yas.cart.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yas.cart.model.CartItem;
import com.yas.cart.viewmodel.CartItemGetVm;
import com.yas.cart.viewmodel.CartItemPostVm;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartItemMapperTest {

    private CartItemMapper cartItemMapper;

    @BeforeEach
    void setUp() {
        cartItemMapper = new CartItemMapper();
    }

    @Test
    void toGetVm_whenValidCartItem_shouldReturnCartItemGetVm() {
        CartItem cartItem = CartItem.builder()
            .customerId("user-1")
            .productId(101L)
            .quantity(2)
            .build();

        CartItemGetVm result = cartItemMapper.toGetVm(cartItem);

        assertEquals("user-1", result.customerId());
        assertEquals(101L, result.productId());
        assertEquals(2, result.quantity());
    }

    @Test
    void toCartItem_whenValidCartItemPostVm_shouldReturnCartItem() {
        CartItemPostVm postVm = new CartItemPostVm(102L, 3);
        
        CartItem result = cartItemMapper.toCartItem(postVm, "user-2");

        assertEquals("user-2", result.getCustomerId());
        assertEquals(102L, result.getProductId());
        assertEquals(3, result.getQuantity());
    }

    @Test
    void toCartItem_whenValidInputs_shouldReturnCartItem() {
        CartItem result = cartItemMapper.toCartItem("user-3", 103L, 4);

        assertEquals("user-3", result.getCustomerId());
        assertEquals(103L, result.getProductId());
        assertEquals(4, result.getQuantity());
    }

    @Test
    void toGetVms_whenValidCartItems_shouldReturnCartItemGetVms() {
        CartItem cartItem1 = CartItem.builder()
            .customerId("user-4")
            .productId(104L)
            .quantity(1)
            .build();
        CartItem cartItem2 = CartItem.builder()
            .customerId("user-4")
            .productId(105L)
            .quantity(2)
            .build();

        List<CartItemGetVm> results = cartItemMapper.toGetVms(List.of(cartItem1, cartItem2));

        assertEquals(2, results.size());
        assertEquals(104L, results.get(0).productId());
        assertEquals(105L, results.get(1).productId());
    }
}
