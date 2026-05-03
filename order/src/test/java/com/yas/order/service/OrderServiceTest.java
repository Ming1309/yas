package com.yas.order.service;

import static com.yas.order.utils.SecurityContextUtils.setSubjectUpSecurityContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.mapper.OrderMapper;
import com.yas.order.model.Order;
import com.yas.order.model.OrderAddress;
import com.yas.order.model.OrderItem;
import com.yas.order.model.csv.OrderItemCsv;
import com.yas.order.model.enumeration.DeliveryMethod;
import com.yas.order.model.enumeration.DeliveryStatus;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentMethod;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.model.request.OrderRequest;
import com.yas.order.repository.OrderItemRepository;
import com.yas.order.repository.OrderRepository;
import com.yas.order.viewmodel.order.OrderBriefVm;
import com.yas.order.viewmodel.order.OrderExistsByProductAndUserGetVm;
import com.yas.order.viewmodel.order.OrderGetVm;
import com.yas.order.viewmodel.order.OrderItemPostVm;
import com.yas.order.viewmodel.order.OrderListVm;
import com.yas.order.viewmodel.order.OrderPostVm;
import com.yas.order.viewmodel.order.OrderVm;
import com.yas.order.viewmodel.order.PaymentOrderStatusVm;
import com.yas.order.viewmodel.orderaddress.OrderAddressPostVm;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductService productService;

    @Mock
    private CartService cartService;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private PromotionService promotionService;

    @InjectMocks
    private OrderService orderService;

    private Order order;
    private OrderItem orderItem;
    private OrderPostVm orderPostVm;

    @BeforeEach
    void setUp() {
        order = buildOrder(1L, "checkout-1");
        orderItem = buildOrderItem(11L, order.getId(), 101L, "Keyboard");
        orderPostVm = buildOrderPostVm();
    }

    @Test
    void createOrder_whenRequestIsValid_shouldCreateOrderAndTriggerSideEffects() {
        AtomicReference<Order> savedOrderRef = new AtomicReference<>();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(1L);
            savedOrderRef.set(saved);
            return saved;
        });
        when(orderRepository.findById(1L)).thenAnswer(invocation -> Optional.of(savedOrderRef.get()));
        when(orderItemRepository.saveAll(anyCollection())).thenAnswer(
            invocation -> new ArrayList<>((java.util.Collection<OrderItem>) invocation.getArgument(0))
        );

        OrderVm result = orderService.createOrder(orderPostVm);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.orderItemVms()).hasSize(2);
        verify(productService).subtractProductStockQuantity(any(OrderVm.class));
        verify(cartService).deleteCartItems(any(OrderVm.class));
        verify(promotionService).updateUsagePromotion(anyList());
        assertThat(savedOrderRef.get().getOrderStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test
    void getOrderWithItemsById_whenOrderExists_shouldReturnVm() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(1L)).thenReturn(List.of(orderItem));

        OrderVm result = orderService.getOrderWithItemsById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.orderItemVms()).hasSize(1);
    }

    @Test
    void getOrderWithItemsById_whenOrderMissing_shouldThrowNotFoundException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.getOrderWithItemsById(1L));
    }

    @Test
    void getAllOrder_whenPageEmpty_shouldReturnEmptyMetadata() {
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        OrderListVm result = orderService.getAllOrder(
            Pair.of(ZonedDateTime.now().minusDays(1), ZonedDateTime.now()),
            "keyboard",
            List.of(),
            Pair.of("VN", "0123"),
            "nhu@example.com",
            Pair.of(0, 10)
        );

        assertThat(result.orderList()).isNull();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void getAllOrder_whenPageHasData_shouldMapToBriefVmList() {
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1));

        OrderListVm result = orderService.getAllOrder(
            Pair.of(ZonedDateTime.now().minusDays(1), ZonedDateTime.now()),
            "keyboard",
            List.of(OrderStatus.PENDING),
            Pair.of("VN", "0123"),
            "nhu@example.com",
            Pair.of(0, 10)
        );

        assertThat(result.orderList()).hasSize(1);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void getLatestOrders_shouldHandleCountAndEmptyCases() {
        assertThat(orderService.getLatestOrders(0)).isEmpty();

        when(orderRepository.getLatestOrders(PageRequest.of(0, 2))).thenReturn(List.of());
        assertThat(orderService.getLatestOrders(2)).isEmpty();

        when(orderRepository.getLatestOrders(PageRequest.of(0, 2))).thenReturn(List.of(order));
        assertThat(orderService.getLatestOrders(2)).hasSize(1);
    }

    @Test
    void isOrderCompletedWithUserIdAndProductId_whenVariationListEmpty_shouldCheckOriginalProductId() {
        setSubjectUpSecurityContext("nhu-user");
        when(productService.getProductVariations(101L)).thenReturn(List.of());
        when(orderRepository.findOne(any(Specification.class))).thenReturn(Optional.of(order));

        OrderExistsByProductAndUserGetVm result = orderService.isOrderCompletedWithUserIdAndProductId(101L);

        assertThat(result.isPresent()).isTrue();
    }

    @Test
    void isOrderCompletedWithUserIdAndProductId_whenVariationsExist_shouldUseVariationIds() {
        setSubjectUpSecurityContext("nhu-user");
        when(productService.getProductVariations(101L)).thenReturn(List.of(
            new com.yas.order.viewmodel.product.ProductVariationVm(201L, "Blue", "BLUE"),
            new com.yas.order.viewmodel.product.ProductVariationVm(202L, "Red", "RED")
        ));
        when(orderRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        OrderExistsByProductAndUserGetVm result = orderService.isOrderCompletedWithUserIdAndProductId(101L);

        assertThat(result.isPresent()).isFalse();
    }

    @Test
    void getMyOrders_shouldMapRepositoryResult() {
        setSubjectUpSecurityContext("nhu-user");
        when(orderRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(order));

        List<OrderGetVm> result = orderService.getMyOrders("keyboard", OrderStatus.PENDING);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(1L);
    }

    @Test
    void findOrderByCheckoutIdAndView_shouldReturnMappedOrder() {
        when(orderRepository.findByCheckoutId("checkout-1")).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(1L)).thenReturn(List.of(orderItem));

        assertThat(orderService.findOrderByCheckoutId("checkout-1")).isEqualTo(order);
        assertThat(orderService.findOrderVmByCheckoutId("checkout-1").id()).isEqualTo(1L);
    }

    @Test
    void findOrderByCheckoutId_whenMissing_shouldThrowNotFoundException() {
        when(orderRepository.findByCheckoutId(anyString())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.findOrderByCheckoutId("missing"));
    }

    @Test
    void updateOrderPaymentStatus_shouldHandleCompletedAndPendingStates() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentOrderStatusVm completed = orderService.updateOrderPaymentStatus(
            new PaymentOrderStatusVm(1L, null, 99L, PaymentStatus.COMPLETED.name())
        );
        assertThat(completed.orderStatus()).isEqualTo(OrderStatus.PAID.getName());

        order.setOrderStatus(OrderStatus.PENDING);
        PaymentOrderStatusVm pending = orderService.updateOrderPaymentStatus(
            new PaymentOrderStatusVm(1L, null, 100L, PaymentStatus.PENDING.name())
        );
        assertThat(pending.orderStatus()).isEqualTo(OrderStatus.PENDING.getName());
    }

    @Test
    void updateOrderPaymentStatus_whenOrderMissing_shouldThrowNotFoundException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.updateOrderPaymentStatus(
            new PaymentOrderStatusVm(1L, null, 99L, PaymentStatus.COMPLETED.name())
        ));
    }

    @Test
    void rejectAndAcceptOrder_shouldUpdateStatus() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.rejectOrder(1L, "invalid address");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.REJECT);
        assertThat(order.getRejectReason()).isEqualTo("invalid address");

        orderService.acceptOrder(1L);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test
    void exportCsv_shouldHandleEmptyAndNonEmptyOrderLists() throws IOException {
        OrderRequest request = OrderRequest.builder()
            .createdFrom(ZonedDateTime.now().minusDays(1))
            .createdTo(ZonedDateTime.now())
            .productName("keyboard")
            .orderStatus(List.of(OrderStatus.PENDING))
            .billingPhoneNumber("0123")
            .email("nhu@example.com")
            .billingCountry("VN")
            .pageNo(0)
            .pageSize(10)
            .build();

        OrderItemCsv csvRow = OrderItemCsv.builder()
            .id(1L)
            .email("nhu@example.com")
            .phone("0123")
            .orderStatus(OrderStatus.PENDING)
            .paymentStatus(PaymentStatus.PENDING)
            .deliveryStatus(DeliveryStatus.PREPARING)
            .totalPrice(new BigDecimal("100.00"))
            .createdOn(ZonedDateTime.now())
            .build();

        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        byte[] emptyResult = orderService.exportCsv(request);
        assertThat(emptyResult).isNotEmpty();

        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1));
        when(orderMapper.toCsv(any(OrderBriefVm.class))).thenReturn(csvRow);

        byte[] populatedResult = orderService.exportCsv(request);

        assertThat(populatedResult).isNotEmpty();
        verify(orderMapper).toCsv(any(OrderBriefVm.class));
    }

    private OrderPostVm buildOrderPostVm() {
        OrderAddressPostVm address = OrderAddressPostVm.builder()
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

        return OrderPostVm.builder()
            .checkoutId("checkout-1")
            .email("nhu@example.com")
            .shippingAddressPostVm(address)
            .billingAddressPostVm(address)
            .note("handle with care")
            .tax(2.5f)
            .discount(1.0f)
            .numberItem(2)
            .totalPrice(new BigDecimal("100.00"))
            .deliveryFee(new BigDecimal("5.00"))
            .couponCode("PROMO")
            .deliveryMethod(DeliveryMethod.GRAB_EXPRESS)
            .paymentMethod(PaymentMethod.COD)
            .paymentStatus(PaymentStatus.PENDING)
            .orderItemPostVms(List.of(
                OrderItemPostVm.builder().productId(101L).productName("Keyboard").quantity(1)
                    .productPrice(new BigDecimal("60.00")).note("silent").build(),
                OrderItemPostVm.builder().productId(102L).productName("Mouse").quantity(1)
                    .productPrice(new BigDecimal("40.00")).note("wireless").build()
            ))
            .build();
    }

    private Order buildOrder(Long id, String checkoutId) {
        OrderAddress address = OrderAddress.builder()
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

        Order result = Order.builder()
            .id(id)
            .email("nhu@example.com")
            .shippingAddressId(address)
            .billingAddressId(address)
            .note("handle with care")
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
            .checkoutId(checkoutId)
            .build();
        result.setCreatedOn(ZonedDateTime.now());
        return result;
    }

    private OrderItem buildOrderItem(Long id, Long orderId, Long productId, String productName) {
        return OrderItem.builder()
            .id(id)
            .orderId(orderId)
            .productId(productId)
            .productName(productName)
            .quantity(1)
            .productPrice(new BigDecimal("60.00"))
            .note("note")
            .discountAmount(new BigDecimal("1.00"))
            .taxAmount(new BigDecimal("2.00"))
            .taxPercent(new BigDecimal("3.00"))
            .build();
    }
}
