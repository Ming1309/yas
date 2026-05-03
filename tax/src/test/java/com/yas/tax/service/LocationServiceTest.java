package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.tax.config.ServiceUrlConfig;
import com.yas.tax.viewmodel.location.StateOrProvinceAndCountryGetNameVm;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

class LocationServiceTest {

    private RestClient restClient;
    private ServiceUrlConfig serviceUrlConfig;
    private LocationService locationService;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        serviceUrlConfig = mock(ServiceUrlConfig.class);
        locationService = new LocationService(restClient, serviceUrlConfig);
        responseSpec = Mockito.mock(RestClient.ResponseSpec.class);
        when(serviceUrlConfig.location()).thenReturn("http://api.yas.local/location");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getStateOrProvinceAndCountryNames_shouldReturnBodyFromRestClient() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("jwt-token");
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        URI url = UriComponentsBuilder.fromUriString(serviceUrlConfig.location())
            .path("/backoffice/state-or-provinces/state-country-names")
            .queryParam("stateOrProvinceIds", List.of(2L))
            .build()
            .toUri();

        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        List<StateOrProvinceAndCountryGetNameVm> expected = List.of(
            new StateOrProvinceAndCountryGetNameVm(2L, "HCM", "Vietnam")
        );
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(expected);

        List<StateOrProvinceAndCountryGetNameVm> actual = locationService.getStateOrProvinceAndCountryNames(List.of(2L));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void handleLocationNameListFallback_shouldRethrowOriginalThrowable() {
        RuntimeException exception = new RuntimeException("service down");

        RuntimeException thrown = assertThrows(RuntimeException.class,
            () -> locationService.handleLocationNameListFallback(exception));

        assertThat(thrown).isSameAs(exception);
    }
}
