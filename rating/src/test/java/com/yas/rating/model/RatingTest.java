package com.yas.rating.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RatingTest {

    @Test
    void equals_shouldReturnTrueForSameInstance() {
        Rating rating = Rating.builder().id(1L).build();

        assertThat(rating).isEqualTo(rating);
    }

    @Test
    void equals_shouldReturnFalseForDifferentType() {
        Rating rating = Rating.builder().id(1L).build();

        assertThat(rating.equals("not-a-rating")).isFalse();
    }

    @Test
    void equals_shouldReturnFalseWhenIdIsNull() {
        Rating first = Rating.builder().build();
        Rating second = Rating.builder().build();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void equals_shouldReturnTrueWhenIdsMatch() {
        Rating first = Rating.builder().id(9L).build();
        Rating second = Rating.builder().id(9L).build();

        assertThat(first).isEqualTo(second);
    }

    @Test
    void hashCode_shouldUseEntityClassHashCode() {
        Rating rating = Rating.builder().id(5L).build();

        assertThat(rating.hashCode()).isEqualTo(Rating.class.hashCode());
    }
}
