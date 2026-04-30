package com.yas.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.order.model.enumeration.DeliveryStatus;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.viewmodel.order.OrderBriefVm;
import com.yas.order.viewmodel.orderaddress.OrderAddressVm;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class OrderMapperTest {

    private final OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);

    @Test
    void toCsv_shouldMapBillingPhoneAndOrderFields() {
        OrderBriefVm orderBriefVm = OrderBriefVm.builder()
            .id(1L)
            .email("nhu@example.com")
            .billingAddressVm(OrderAddressVm.builder().phone("0123").build())
            .totalPrice(new BigDecimal("100.00"))
            .orderStatus(OrderStatus.PENDING)
            .deliveryStatus(DeliveryStatus.PREPARING)
            .paymentStatus(PaymentStatus.PENDING)
            .createdOn(ZonedDateTime.now())
            .build();

        var csv = orderMapper.toCsv(orderBriefVm);

        assertThat(csv.getId()).isEqualTo(1L);
        assertThat(csv.getEmail()).isEqualTo("nhu@example.com");
        assertThat(csv.getPhone()).isEqualTo("0123");
        assertThat(csv.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void toCsv_shouldReturnNullWhenSourceIsNull() {
        assertThat(orderMapper.toCsv(null)).isNull();
    }

    @Test
    void toCsv_shouldAllowMissingBillingAddress() {
        OrderBriefVm orderBriefVm = OrderBriefVm.builder()
            .id(2L)
            .email("nhu@example.com")
            .billingAddressVm(null)
            .totalPrice(new BigDecimal("200.00"))
            .orderStatus(OrderStatus.COMPLETED)
            .deliveryStatus(DeliveryStatus.DELIVERED)
            .paymentStatus(PaymentStatus.COMPLETED)
            .createdOn(ZonedDateTime.now())
            .build();

        var csv = orderMapper.toCsv(orderBriefVm);

        assertThat(csv.getId()).isEqualTo(2L);
        assertThat(csv.getPhone()).isNull();
        assertThat(csv.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
    }
}
