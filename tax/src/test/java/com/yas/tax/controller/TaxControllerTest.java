package com.yas.tax.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.service.TaxClassService;
import com.yas.tax.service.TaxRateService;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

class TaxControllerTest {

    @Test
    void taxRateController_shouldDelegateToServiceForCrudAndQueries() {
        TaxRateService taxRateService = mock(TaxRateService.class);
        TaxRateController controller = new TaxRateController(taxRateService);
        TaxClass taxClass = TaxClass.builder().id(1L).name("VAT").build();
        TaxRate taxRate = TaxRate.builder().id(10L).rate(0.1).zipCode("70000").countryId(84L).stateOrProvinceId(2L)
            .taxClass(taxClass).build();
        TaxRateVm taxRateVm = TaxRateVm.fromModel(taxRate);
        TaxRatePostVm postVm = new TaxRatePostVm(0.1, "70000", 1L, 2L, 84L);
        TaxRateListGetVm listVm = new TaxRateListGetVm(List.of(), 0, 10, 0, 0, true);
        when(taxRateService.getPageableTaxRates(0, 10)).thenReturn(listVm);
        when(taxRateService.findById(10L)).thenReturn(taxRateVm);
        when(taxRateService.createTaxRate(postVm)).thenReturn(taxRate);
        when(taxRateService.getTaxPercent(1L, 84L, 2L, "70000")).thenReturn(0.1);
        when(taxRateService.getBulkTaxRate(List.of(1L, 2L), 84L, 2L, "70000")).thenReturn(List.of(taxRateVm));

        assertThat(controller.getPageableTaxRates(0, 10).getBody()).isEqualTo(listVm);
        assertThat(controller.getTaxRate(10L).getBody()).isEqualTo(taxRateVm);

        ResponseEntity<TaxRateVm> created = controller.createTaxRate(postVm, UriComponentsBuilder.newInstance());
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).isEqualTo(URI.create("/tax-rates/10"));
        assertThat(created.getBody()).isEqualTo(taxRateVm);

        assertThat(controller.updateTaxRate(10L, postVm).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.deleteTaxRate(10L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.getTaxPercentByAddress(1L, 84L, 2L, "70000").getBody()).isEqualTo(0.1);
        assertThat(controller.getBatchTaxPercentsByAddress(List.of(1L, 2L), 84L, 2L, "70000").getBody())
            .containsExactly(taxRateVm);

        verify(taxRateService).updateTaxRate(postVm, 10L);
        verify(taxRateService).delete(10L);
    }

    @Test
    void taxClassController_shouldDelegateToServiceForCrudAndQueries() {
        TaxClassService taxClassService = mock(TaxClassService.class);
        TaxClassController controller = new TaxClassController(taxClassService);
        TaxClass taxClass = TaxClass.builder().id(1L).name("VAT").build();
        TaxClassVm taxClassVm = TaxClassVm.fromModel(taxClass);
        TaxClassPostVm postVm = new TaxClassPostVm("1", "VAT");
        TaxClassListGetVm listVm = new TaxClassListGetVm(List.of(taxClassVm), 0, 10, 1, 1, true);
        when(taxClassService.getPageableTaxClasses(0, 10)).thenReturn(listVm);
        when(taxClassService.findAllTaxClasses()).thenReturn(List.of(taxClassVm));
        when(taxClassService.findById(1L)).thenReturn(taxClassVm);
        when(taxClassService.create(postVm)).thenReturn(taxClass);

        assertThat(controller.getPageableTaxClasses(0, 10).getBody()).isEqualTo(listVm);
        assertThat(controller.listTaxClasses().getBody()).containsExactly(taxClassVm);
        assertThat(controller.getTaxClass(1L).getBody()).isEqualTo(taxClassVm);

        ResponseEntity<TaxClassVm> created = controller.createTaxClass(postVm, UriComponentsBuilder.newInstance());
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).isEqualTo(URI.create("/tax-classes/1"));
        assertThat(created.getBody()).isEqualTo(taxClassVm);

        assertThat(controller.updateTaxClass(1L, postVm).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.deleteTaxClass(1L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(taxClassService).update(postVm, 1L);
        verify(taxClassService).delete(1L);
    }
}
