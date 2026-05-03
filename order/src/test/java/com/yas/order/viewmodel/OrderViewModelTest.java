package com.yas.order.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.order.model.Order;
import com.yas.order.model.OrderAddress;
import com.yas.order.model.OrderItem;
import com.yas.order.model.enumeration.DeliveryMethod;
import com.yas.order.model.enumeration.DeliveryStatus;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.model.request.OrderRequest;
import com.yas.order.viewmodel.checkout.CheckoutPaymentMethodPutVm;
import com.yas.order.viewmodel.checkout.CheckoutStatusPutVm;
import com.yas.order.viewmodel.customer.CustomerVm;
import com.yas.order.viewmodel.order.OrderBriefVm;
import com.yas.order.viewmodel.order.OrderExistsByProductAndUserGetVm;
import com.yas.order.viewmodel.order.OrderGetVm;
import com.yas.order.viewmodel.order.OrderItemGetVm;
import com.yas.order.viewmodel.order.OrderItemVm;
import com.yas.order.viewmodel.order.OrderListVm;
import com.yas.order.viewmodel.order.OrderVm;
import com.yas.order.viewmodel.order.PaymentOrderStatusVm;
import com.yas.order.viewmodel.orderaddress.OrderAddressVm;
import com.yas.order.viewmodel.product.ProductGetCheckoutListVm;
import com.yas.order.viewmodel.product.ProductQuantityItem;
import com.yas.order.viewmodel.product.ProductVariationVm;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OrderViewModelTest {

    @Test
    void fromModelMethods_shouldMapDomainObjects() {
        OrderAddress address = buildAddress();
        Order order = buildOrder(address);
        OrderItem item = buildItem(order.getId());

        OrderAddressVm addressVm = OrderAddressVm.fromModel(address);
        OrderItemVm itemVm = OrderItemVm.fromModel(item);
        OrderVm orderVm = OrderVm.fromModel(order, Set.of(item));
        OrderGetVm orderGetVm = OrderGetVm.fromModel(order, Set.of(item));
        OrderBriefVm orderBriefVm = OrderBriefVm.fromModel(order);

        assertThat(addressVm.contactName()).isEqualTo("Nhu");
        assertThat(itemVm.productName()).isEqualTo("Keyboard");
        assertThat(orderVm.orderItemVms()).hasSize(1);
        assertThat(orderGetVm.orderItems()).hasSize(1);
        assertThat(orderBriefVm.email()).isEqualTo("nhu@example.com");
    }

    @Test
    void helperFactories_shouldHandleNullOrEmptyCollections() {
        OrderGetVm orderGetVm = OrderGetVm.fromModel(buildOrder(buildAddress()), null);
        OrderVm orderVm = OrderVm.fromModel(buildOrder(buildAddress()), null);

        assertThat(OrderItemGetVm.fromModels(null)).isEmpty();
        assertThat(orderGetVm.orderItems()).isEmpty();
        assertThat(orderVm.orderItemVms()).isNull();
    }

    @Test
    void recordsAndBuilders_shouldExposeAssignedValues() {
        ErrorVm errorVm = new ErrorVm("400", "Bad Request", "detail", List.of("field"));
        ResponeStatusVm responseStatusVm = new ResponeStatusVm("title", "message", "200");
        ProductVariationVm productVariationVm = new ProductVariationVm(1L, "Blue", "BLUE");
        ProductQuantityItem productQuantityItem = new ProductQuantityItem(10L, 2L);
        OrderExistsByProductAndUserGetVm existsVm = new OrderExistsByProductAndUserGetVm(true);
        PaymentOrderStatusVm paymentOrderStatusVm = new PaymentOrderStatusVm(1L, "PAID", 9L, "COMPLETED");
        OrderListVm orderListVm = new OrderListVm(List.of(), 0, 0);
        ProductGetCheckoutListVm checkoutListVm = new ProductGetCheckoutListVm(List.of(), 0, 0, 0, 0, true);
        CheckoutStatusPutVm checkoutStatusPutVm = new CheckoutStatusPutVm("checkout-1", "PENDING");
        CheckoutPaymentMethodPutVm paymentMethodPutVm = new CheckoutPaymentMethodPutVm("COD");
        CustomerVm customerVm = new CustomerVm("nhu", "nhu@example.com", "Nhu", "Pham");
        OrderRequest orderRequest = OrderRequest.builder().productName("Keyboard").pageNo(0).pageSize(10).build();

        assertThat(errorVm.detail()).isEqualTo("detail");
        assertThat(responseStatusVm.statusCode()).isEqualTo("200");
        assertThat(productVariationVm.sku()).isEqualTo("BLUE");
        assertThat(productQuantityItem.quantity()).isEqualTo(2L);
        assertThat(existsVm.isPresent()).isTrue();
        assertThat(paymentOrderStatusVm.paymentStatus()).isEqualTo("COMPLETED");
        assertThat(orderListVm.totalPages()).isZero();
        assertThat(checkoutListVm.isLast()).isTrue();
        assertThat(checkoutStatusPutVm.checkoutStatus()).isEqualTo("PENDING");
        assertThat(paymentMethodPutVm.paymentMethodId()).isEqualTo("COD");
        assertThat(customerVm.firstName()).isEqualTo("Nhu");
        assertThat(orderRequest.getProductName()).isEqualTo("Keyboard");
    }

    private Order buildOrder(OrderAddress address) {
        Order order = Order.builder()
            .id(1L)
            .email("nhu@example.com")
            .shippingAddressId(address)
            .billingAddressId(address)
            .note("note")
            .tax(2.5f)
            .discount(1.0f)
            .numberItem(1)
            .couponCode("PROMO")
            .totalPrice(new BigDecimal("100.00"))
            .deliveryFee(new BigDecimal("5.00"))
            .orderStatus(OrderStatus.PENDING)
            .deliveryMethod(DeliveryMethod.GRAB_EXPRESS)
            .deliveryStatus(DeliveryStatus.PREPARING)
            .paymentStatus(PaymentStatus.PENDING)
            .checkoutId("checkout-1")
            .build();
        order.setCreatedOn(ZonedDateTime.now());
        return order;
    }

    private OrderAddress buildAddress() {
        return OrderAddress.builder()
            .id(50L)
            .contactName("Nhu")
            .phone("0123")
            .addressLine1("123 Street")
            .addressLine2("Ward 1")
            .city("HCM")
            .zipCode("70000")
            .districtId(1L)
            .districtName("District 1")
            .stateOrProvinceId(20L)
            .stateOrProvinceName("HCM")
            .countryId(30L)
            .countryName("Vietnam")
            .build();
    }

    private OrderItem buildItem(Long orderId) {
        return OrderItem.builder()
            .id(11L)
            .orderId(orderId)
            .productId(101L)
            .productName("Keyboard")
            .quantity(1)
            .productPrice(new BigDecimal("60.00"))
            .note("silent")
            .discountAmount(new BigDecimal("1.00"))
            .taxAmount(new BigDecimal("2.00"))
            .taxPercent(new BigDecimal("3.00"))
            .build();
    }
}
