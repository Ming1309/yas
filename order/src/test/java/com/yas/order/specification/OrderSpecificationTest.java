package com.yas.order.specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.order.model.Order;
import com.yas.order.model.OrderItem;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.utils.Constants;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderSpecificationTest {

    private final CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    private final Predicate conjunction = mock(Predicate.class);

    @Test
    void hasCreatedBy_shouldCreateEqualPredicate() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Path<Object> createdByPath = mockPath();
        Predicate expected = mock(Predicate.class);

        when(root.get(Constants.Column.CREATE_BY_COLUMN)).thenReturn(createdByPath);
        when(criteriaBuilder.equal(createdByPath, "user123")).thenReturn(expected);

        Predicate result = OrderSpecification.hasCreatedBy("user123")
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void hasOrderStatus_shouldReturnEqualPredicateWhenStatusPresent() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Path<Object> orderStatusPath = mockPath();
        Predicate expected = mock(Predicate.class);

        when(root.get(Constants.Column.ORDER_ORDER_STATUS_COLUMN)).thenReturn(orderStatusPath);
        when(criteriaBuilder.equal(orderStatusPath, OrderStatus.COMPLETED)).thenReturn(expected);

        Predicate result = OrderSpecification.hasOrderStatus(OrderStatus.COMPLETED)
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void hasOrderStatus_shouldReturnConjunctionWhenStatusMissing() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);

        when(criteriaBuilder.conjunction()).thenReturn(conjunction);

        Predicate result = OrderSpecification.hasOrderStatus(null)
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(conjunction);
    }

    @Test
    void hasProductInOrderItems_shouldReturnConjunctionWhenQueryMissing() {
        Root<Order> root = mock(Root.class);

        when(criteriaBuilder.conjunction()).thenReturn(conjunction);

        Predicate result = OrderSpecification.hasProductInOrderItems(List.of(1L))
            .toPredicate(root, null, criteriaBuilder);

        assertThat(result).isSameAs(conjunction);
    }

    @Test
    void hasProductInOrderItems_shouldUseEmptyListWhenProductIdsNull() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        @SuppressWarnings("unchecked")
        Subquery<OrderItem> subquery = mock(Subquery.class);
        Root<OrderItem> orderItemRoot = mock(Root.class);
        Path<Long> rootIdPath = mockPath();
        Path<Long> orderIdPath = mockPath();
        Path<Object> productIdPath = mockPath();
        Predicate equalPredicate = mock(Predicate.class);
        Predicate inPredicate = mock(Predicate.class);
        Predicate andPredicate = mock(Predicate.class);
        Predicate existsPredicate = mock(Predicate.class);

        when(query.subquery(OrderItem.class)).thenReturn(subquery);
        when(subquery.from(OrderItem.class)).thenReturn(orderItemRoot);
        when(root.<Long>get(Constants.Column.ID_COLUMN)).thenReturn(rootIdPath);
        when(orderItemRoot.<Long>get(Constants.Column.ORDER_ORDER_ID_COLUMN)).thenReturn(orderIdPath);
        when(orderItemRoot.get(Constants.Column.ORDER_ITEM_PRODUCT_ID_COLUMN)).thenReturn(productIdPath);
        when(criteriaBuilder.equal(orderIdPath, rootIdPath)).thenReturn(equalPredicate);
        when(productIdPath.in(List.of())).thenReturn(inPredicate);
        when(criteriaBuilder.and(equalPredicate, inPredicate)).thenReturn(andPredicate);
        when(subquery.select(orderItemRoot)).thenReturn(subquery);
        when(subquery.where(andPredicate)).thenReturn(subquery);
        when(criteriaBuilder.exists(subquery)).thenReturn(existsPredicate);

        Predicate result = OrderSpecification.hasProductInOrderItems(null)
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(existsPredicate);
    }

    @Test
    void hasProductNameInOrderItems_shouldReturnConjunctionWhenQueryMissing() {
        Root<Order> root = mock(Root.class);

        when(criteriaBuilder.conjunction()).thenReturn(conjunction);

        Predicate result = OrderSpecification.hasProductNameInOrderItems("book")
            .toPredicate(root, null, criteriaBuilder);

        assertThat(result).isSameAs(conjunction);
    }

    @Test
    void hasProductNameInOrderItems_shouldSkipLikeWhenProductNameEmpty() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        @SuppressWarnings("unchecked")
        Subquery<Long> subquery = mock(Subquery.class);
        Root<OrderItem> orderItemRoot = mock(Root.class);
        Path<Long> orderIdPath = mockPath();
        Path<Long> rootIdPath = mockPath();
        @SuppressWarnings("unchecked")
        CriteriaBuilder.In<Long> inPredicate = mock(CriteriaBuilder.In.class);

        when(criteriaBuilder.conjunction()).thenReturn(conjunction);
        when(query.subquery(Long.class)).thenReturn(subquery);
        when(subquery.from(OrderItem.class)).thenReturn(orderItemRoot);
        when(orderItemRoot.<Long>get(Constants.Column.ORDER_ORDER_ID_COLUMN)).thenReturn(orderIdPath);
        when(subquery.select(orderIdPath)).thenReturn(subquery);
        when(subquery.where(conjunction)).thenReturn(subquery);
        when(root.<Long>get(Constants.Column.ID_COLUMN)).thenReturn(rootIdPath);
        when(criteriaBuilder.in(rootIdPath)).thenReturn(inPredicate);
        when(inPredicate.value(subquery)).thenReturn(inPredicate);

        Predicate result = OrderSpecification.hasProductNameInOrderItems("")
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(inPredicate);
        verify(criteriaBuilder, never()).like(any(), anyString());
    }

    @Test
    void withEmail_shouldReturnLikePredicateWhenEmailPresent() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Path<Object> emailPath = mockPath();
        Predicate likePredicate = mock(Predicate.class);

        when(root.get(Constants.Column.ORDER_EMAIL_COLUMN)).thenReturn(emailPath);
        when(criteriaBuilder.like(any(), anyString())).thenReturn(likePredicate);

        Predicate result = OrderSpecification.withEmail("test@example.com")
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(likePredicate);
    }

    @Test
    void withEmail_shouldReturnConjunctionWhenEmailEmpty() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);

        when(criteriaBuilder.conjunction()).thenReturn(conjunction);

        Predicate result = OrderSpecification.withEmail("")
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(conjunction);
    }

    @Test
    void withOrderStatus_shouldReturnInPredicateWhenListNotEmpty() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Path<Object> orderStatusPath = mockPath();
        Predicate inPredicate = mock(Predicate.class);

        when(root.get(Constants.Column.ORDER_ORDER_STATUS_COLUMN)).thenReturn(orderStatusPath);
        when(orderStatusPath.in(any(Collection.class))).thenReturn(inPredicate);

        Predicate result = OrderSpecification.withOrderStatus(List.of(OrderStatus.COMPLETED))
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(inPredicate);
    }

    @Test
    void withOrderStatus_shouldReturnConjunctionWhenListEmpty() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);

        when(criteriaBuilder.conjunction()).thenReturn(conjunction);

        Predicate result = OrderSpecification.withOrderStatus(List.of())
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(conjunction);
    }

    @Test
    void withProductName_shouldReturnConjunctionWhenQueryMissing() {
        Root<Order> root = mock(Root.class);

        when(criteriaBuilder.conjunction()).thenReturn(conjunction);

        Predicate result = OrderSpecification.withProductName("tea")
            .toPredicate(root, null, criteriaBuilder);

        assertThat(result).isSameAs(conjunction);
    }

    @Test
    void withProductName_shouldReturnConjunctionWhenProductNameEmpty() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);

        when(criteriaBuilder.conjunction()).thenReturn(conjunction);

        Predicate result = OrderSpecification.withProductName("")
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(conjunction);
    }

    @Test
    void withProductName_shouldBuildExistsSubqueryWhenProductNamePresent() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        @SuppressWarnings("unchecked")
        Subquery<Long> subquery = mock(Subquery.class);
        Root<OrderItem> orderItemRoot = mock(Root.class);
        Path<Long> rootIdPath = mockPath();
        Path<Long> orderIdPath = mockPath();
        Path<Object> productNamePath = mockPath();
        Predicate equalPredicate = mock(Predicate.class);
        Predicate likePredicate = mock(Predicate.class);
        Predicate andPredicate = mock(Predicate.class);
        Predicate existsPredicate = mock(Predicate.class);

        when(query.subquery(Long.class)).thenReturn(subquery);
        when(subquery.from(OrderItem.class)).thenReturn(orderItemRoot);
        when(orderItemRoot.<Long>get(Constants.Column.ORDER_ORDER_ID_COLUMN)).thenReturn(orderIdPath);
        when(orderItemRoot.get(Constants.Column.ORDER_ITEM_PRODUCT_NAME_COLUMN)).thenReturn(productNamePath);
        when(root.<Long>get(Constants.Column.ID_COLUMN)).thenReturn(rootIdPath);
        when(subquery.select(orderIdPath)).thenReturn(subquery);
        when(criteriaBuilder.equal(orderIdPath, rootIdPath)).thenReturn(equalPredicate);
        when(criteriaBuilder.like(any(), anyString())).thenReturn(likePredicate);
        when(criteriaBuilder.and(equalPredicate, likePredicate)).thenReturn(andPredicate);
        when(subquery.where(andPredicate)).thenReturn(subquery);
        when(criteriaBuilder.exists(subquery)).thenReturn(existsPredicate);

        Predicate result = OrderSpecification.withProductName("Tea")
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(existsPredicate);
    }

    @Test
    void withBillingPhoneNumber_shouldReturnLikePredicateWhenPhonePresent() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Path<Object> billingAddressPath = mockPath();
        Path<Object> phonePath = mockPath();
        Predicate likePredicate = mock(Predicate.class);

        when(root.get(Constants.Column.ORDER_BILLING_ADDRESS_ID_COLUMN)).thenReturn(billingAddressPath);
        when(billingAddressPath.get(Constants.Column.ORDER_PHONE_COLUMN)).thenReturn(phonePath);
        when(criteriaBuilder.like(any(), anyString())).thenReturn(likePredicate);

        Predicate result = OrderSpecification.withBillingPhoneNumber("1234567890")
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(likePredicate);
    }

    @Test
    void withBillingPhoneNumber_shouldReturnConjunctionWhenPhoneEmpty() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);

        when(criteriaBuilder.conjunction()).thenReturn(conjunction);

        Predicate result = OrderSpecification.withBillingPhoneNumber("")
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(conjunction);
    }

    @Test
    void withCountryName_shouldReturnLikePredicateWhenCountryPresent() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Path<Object> billingAddressPath = mockPath();
        Path<Object> countryPath = mockPath();
        Predicate likePredicate = mock(Predicate.class);

        when(root.get(Constants.Column.ORDER_BILLING_ADDRESS_ID_COLUMN)).thenReturn(billingAddressPath);
        when(billingAddressPath.get(Constants.Column.ORDER_COUNTRY_NAME_COLUMN)).thenReturn(countryPath);
        when(criteriaBuilder.like(any(), anyString())).thenReturn(likePredicate);

        Predicate result = OrderSpecification.withCountryName("Viet Nam")
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(likePredicate);
    }

    @Test
    void withCountryName_shouldReturnConjunctionWhenCountryMissing() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);

        when(criteriaBuilder.conjunction()).thenReturn(conjunction);

        Predicate result = OrderSpecification.withCountryName(null)
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(conjunction);
    }

    @Test
    void withDateRange_shouldReturnBetweenPredicateWhenBothDatesPresent() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Path<ZonedDateTime> createdOnPath = mockPath();
        ZonedDateTime createdFrom = ZonedDateTime.now().minusDays(7);
        ZonedDateTime createdTo = ZonedDateTime.now();
        Predicate betweenPredicate = mock(Predicate.class);

        when(root.<ZonedDateTime>get(Constants.Column.CREATE_ON_COLUMN)).thenReturn(createdOnPath);
        when(criteriaBuilder.between(createdOnPath, createdFrom, createdTo)).thenReturn(betweenPredicate);

        Predicate result = OrderSpecification.withDateRange(createdFrom, createdTo)
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(betweenPredicate);
    }

    @Test
    void withDateRange_shouldReturnConjunctionWhenOneDateMissing() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);

        when(criteriaBuilder.conjunction()).thenReturn(conjunction);

        Predicate result = OrderSpecification.withDateRange(ZonedDateTime.now(), null)
            .toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(conjunction);
    }

    @Test
    void findOrderByWithMulCriteria_shouldFetchAddressesForEntityQuery() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Predicate combined = mock(Predicate.class);

        when(query.getResultType()).thenReturn((Class) Order.class);
        when(criteriaBuilder.conjunction()).thenReturn(conjunction);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(combined);

        Predicate result = OrderSpecification.findOrderByWithMulCriteria(
            List.of(),
            null,
            "",
            "",
            null,
            null,
            null
        ).toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(combined);
        verify(root).fetch(Constants.Column.ORDER_SHIPPING_ADDRESS_ID_COLUMN, jakarta.persistence.criteria.JoinType.LEFT);
        verify(root).fetch(Constants.Column.ORDER_BILLING_ADDRESS_ID_COLUMN, jakarta.persistence.criteria.JoinType.LEFT);
    }

    @Test
    void findOrderByWithMulCriteria_shouldSkipFetchForCountQuery() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Predicate combined = mock(Predicate.class);

        when(query.getResultType()).thenReturn((Class) Long.class);
        when(criteriaBuilder.conjunction()).thenReturn(conjunction);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(combined);

        Predicate result = OrderSpecification.findOrderByWithMulCriteria(
            List.of(),
            null,
            null,
            null,
            "",
            null,
            null
        ).toPredicate(root, query, criteriaBuilder);

        assertThat(result).isSameAs(combined);
        verify(root, never()).fetch(anyString(), any());
    }

    @SuppressWarnings("unchecked")
    private static <T> Path<T> mockPath() {
        return mock(Path.class);
    }
}
