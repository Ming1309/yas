package com.yas.order.service;

import static com.yas.order.utils.SecurityContextUtils.setUpSecurityContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.config.ServiceUrlConfig;
import com.yas.order.model.enumeration.DeliveryMethod;
import com.yas.order.model.enumeration.DeliveryStatus;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.viewmodel.order.OrderItemVm;
import com.yas.order.viewmodel.order.OrderVm;
import com.yas.order.viewmodel.product.ProductCheckoutListVm;
import com.yas.order.viewmodel.product.ProductGetCheckoutListVm;
import com.yas.order.viewmodel.product.ProductVariationVm;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

class ProductServiceTest {

    private RestClient restClient;
    private ServiceUrlConfig serviceUrlConfig;
    private ProductService productService;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        serviceUrlConfig = mock(ServiceUrlConfig.class);
        productService = new ProductService(restClient, serviceUrlConfig);
        responseSpec = Mockito.mock(RestClient.ResponseSpec.class);
        setUpSecurityContext("jwt-token");
        when(serviceUrlConfig.product()).thenReturn("http://api.yas.local/product");
    }

    @Test
    void getProductVariations_shouldReturnBodyFromRestClient() {
        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        List<ProductVariationVm> expected = List.of(new ProductVariationVm(1L, "Blue", "BLUE"));
        when(responseSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(ResponseEntity.ok(expected));

        List<ProductVariationVm> actual = productService.getProductVariations(10L);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void subtractProductStockQuantity_shouldSendRequestWithoutThrowing() {
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        when(restClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(java.net.URI.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        assertDoesNotThrow(() -> productService.subtractProductStockQuantity(buildOrderVm()));
    }

    @Test
    void getProductInfomation_whenResponseContainsProducts_shouldReturnMappedResult() {
        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        ProductCheckoutListVm product = ProductCheckoutListVm.builder()
            .id(101L)
            .name("Keyboard")
            .price(60.0)
            .taxClassId(1L)
            .build();
        ProductGetCheckoutListVm response = new ProductGetCheckoutListVm(List.of(product), 0, 1, 1, 1, true);
        when(responseSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(ResponseEntity.ok(response));

        Map<Long, ProductCheckoutListVm> actual = productService.getProductInfomation(Set.of(101L), 0, 10);

        assertThat(actual).containsEntry(101L, product);
    }

    @Test
    void getProductInfomation_whenBodyMissing_shouldThrowNotFoundException() {
        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(new ProductGetCheckoutListVm(null, 0, 0, 0, 0, true)));

        assertThrows(NotFoundException.class, () -> productService.getProductInfomation(Set.of(101L), 0, 10));
    }

    @Test
    void fallbackMethods_shouldRethrowOriginalThrowable() {
        RuntimeException exception = new RuntimeException("downstream failed");

        assertThrows(RuntimeException.class, () -> productService.handleProductVariationListFallback(exception));
        assertThrows(RuntimeException.class, () -> productService.handleProductInfomationFallback(exception));
    }

    private OrderVm buildOrderVm() {
        Set<OrderItemVm> items = Set.of(
            new OrderItemVm(1L, 101L, "Keyboard", 1, new BigDecimal("60.00"), "silent",
                new BigDecimal("1.00"), new BigDecimal("2.00"), new BigDecimal("3.00"), 1L),
            new OrderItemVm(2L, 102L, "Mouse", 1, new BigDecimal("40.00"), "wireless",
                new BigDecimal("1.00"), new BigDecimal("2.00"), new BigDecimal("3.00"), 1L)
        );
        return new OrderVm(
            1L,
            "nhu@example.com",
            null,
            null,
            "handle with care",
            2.5f,
            1.0f,
            2,
            new BigDecimal("100.00"),
            new BigDecimal("5.00"),
            "PROMO",
            OrderStatus.PENDING,
            DeliveryMethod.GRAB_EXPRESS,
            DeliveryStatus.PREPARING,
            PaymentStatus.PENDING,
            items,
            UUID.randomUUID().toString()
        );
    }
}
