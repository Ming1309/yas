package com.yas.order.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CheckoutItemTest {

    @Test
    void equals_shouldReturnTrueForSameInstance() {
        CheckoutItem checkoutItem = CheckoutItem.builder().id(1L).build();

        assertThat(checkoutItem).isEqualTo(checkoutItem);
    }

    @Test
    void equals_shouldReturnFalseForDifferentType() {
        CheckoutItem checkoutItem = CheckoutItem.builder().id(1L).build();

        assertThat(checkoutItem.equals("not-a-checkout-item")).isFalse();
    }

    @Test
    void equals_shouldReturnFalseWhenIdIsNull() {
        CheckoutItem first = CheckoutItem.builder().build();
        CheckoutItem second = CheckoutItem.builder().build();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void equals_shouldReturnTrueWhenIdsMatch() {
        CheckoutItem first = CheckoutItem.builder().id(9L).build();
        CheckoutItem second = CheckoutItem.builder().id(9L).build();

        assertThat(first).isEqualTo(second);
    }

    @Test
    void hashCode_shouldUseEntityClassHashCode() {
        CheckoutItem checkoutItem = CheckoutItem.builder().id(5L).build();

        assertThat(checkoutItem.hashCode()).isEqualTo(CheckoutItem.class.hashCode());
    }
}
