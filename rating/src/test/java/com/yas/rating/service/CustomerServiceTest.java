package com.yas.rating.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.rating.config.ServiceUrlConfig;
import com.yas.rating.viewmodel.CustomerVm;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

class CustomerServiceTest {

    private static final String CUSTOMER_URL = "http://api.yas.local/customer";

    private RestClient restClient;
    private ServiceUrlConfig serviceUrlConfig;
    private CustomerService customerService;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        serviceUrlConfig = mock(ServiceUrlConfig.class);
        customerService = new CustomerService(restClient, serviceUrlConfig);
        responseSpec = Mockito.mock(RestClient.ResponseSpec.class);
        when(serviceUrlConfig.customer()).thenReturn(CUSTOMER_URL);
    }

    @Test
    void getCustomer_whenProfileExists_shouldReturnCustomerVm() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("jwt-token");
        SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt)
        );

        URI url = UriComponentsBuilder
            .fromUriString(serviceUrlConfig.customer())
            .path("/storefront/customer/profile")
            .buildAndExpand()
            .toUri();

        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        CustomerVm customer = new CustomerVm("john_doe", "john.doe@example.com", "John", "Doe");
        when(responseSpec.body(CustomerVm.class)).thenReturn(customer);

        CustomerVm result = customerService.getCustomer();

        assertThat(result.username()).isEqualTo("john_doe");
        assertThat(result.email()).isEqualTo("john.doe@example.com");
        assertThat(result.firstName()).isEqualTo("John");
        assertThat(result.lastName()).isEqualTo("Doe");
    }

    @Test
    void handleFallback_shouldReturnNull() throws Throwable {
        assertThat(customerService.handleFallback(new RuntimeException("downstream failed"))).isNull();
    }

    @Test
    void handleBodilessFallback_shouldRethrowOriginalThrowable() {
        RuntimeException exception = new RuntimeException("downstream failed");

        RuntimeException thrown = assertThrows(RuntimeException.class,
            () -> customerService.handleBodilessFallback(exception));

        assertThat(thrown).isSameAs(exception);
    }
}
