package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.constants.MessageCode;
import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.repository.TaxRateRepository;
import com.yas.tax.viewmodel.location.StateOrProvinceAndCountryGetNameVm;
import com.yas.tax.viewmodel.taxrate.TaxRateGetDetailVm;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class TaxServiceTest {

    @Mock
    private TaxRateRepository taxRateRepository;

    @Mock
    private LocationService locationService;

    @Mock
    private TaxClassRepository taxClassRepository;

    @InjectMocks
    private TaxRateService taxRateService;

    private TaxClass taxClass;
    private TaxRate taxRate;

    @BeforeEach
    void setUp() {
        taxClass = TaxClass.builder().id(1L).name("VAT").build();
        taxRate = TaxRate.builder()
            .id(10L)
            .rate(0.1)
            .zipCode("70000")
            .stateOrProvinceId(2L)
            .countryId(84L)
            .taxClass(taxClass)
            .build();
    }

    @Test
    void findAll_shouldReturnAllTaxRates() {
        when(taxRateRepository.findAll()).thenReturn(List.of(taxRate));

        List<TaxRateVm> result = taxRateService.findAll();

        assertThat(result).containsExactly(TaxRateVm.fromModel(taxRate));
    }

    @Test
    void createTaxRate_whenTaxClassMissing_shouldThrowNotFoundException() {
        TaxRatePostVm postVm = new TaxRatePostVm(0.1, "70000", 1L, 2L, 84L);
        when(taxClassRepository.existsById(1L)).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> taxRateService.createTaxRate(postVm));

        assertThat(exception.getMessage()).isEqualTo(MessageCode.TAX_CLASS_NOT_FOUND);
    }

    @Test
    void createTaxRate_whenTaxClassExists_shouldPersistTaxRate() {
        TaxRatePostVm postVm = new TaxRatePostVm(0.1, "70000", 1L, 2L, 84L);
        when(taxClassRepository.existsById(1L)).thenReturn(true);
        when(taxClassRepository.getReferenceById(1L)).thenReturn(taxClass);
        when(taxRateRepository.save(any(TaxRate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaxRate result = taxRateService.createTaxRate(postVm);

        assertThat(result.getRate()).isEqualTo(0.1);
        assertThat(result.getTaxClass()).isEqualTo(taxClass);
    }

    @Test
    void updateTaxRate_whenRateMissing_shouldThrowNotFoundException() {
        TaxRatePostVm postVm = new TaxRatePostVm(0.2, "70001", 1L, 2L, 84L);
        when(taxRateRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> taxRateService.updateTaxRate(postVm, 99L));

        assertThat(exception.getMessage()).isEqualTo(MessageCode.TAX_RATE_NOT_FOUND);
    }

    @Test
    void updateTaxRate_whenTaxClassMissing_shouldThrowNotFoundException() {
        TaxRatePostVm postVm = new TaxRatePostVm(0.2, "70001", 5L, 2L, 84L);
        when(taxRateRepository.findById(10L)).thenReturn(Optional.of(taxRate));
        when(taxClassRepository.existsById(5L)).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> taxRateService.updateTaxRate(postVm, 10L));

        assertThat(exception.getMessage()).isEqualTo(MessageCode.TAX_CLASS_NOT_FOUND);
    }

    @Test
    void updateTaxRate_whenInputValid_shouldUpdateAndSave() {
        TaxClass newTaxClass = TaxClass.builder().id(5L).name("GST").build();
        TaxRatePostVm postVm = new TaxRatePostVm(0.2, "70001", 5L, 3L, 1L);
        when(taxRateRepository.findById(10L)).thenReturn(Optional.of(taxRate));
        when(taxClassRepository.existsById(5L)).thenReturn(true);
        when(taxClassRepository.getReferenceById(5L)).thenReturn(newTaxClass);

        taxRateService.updateTaxRate(postVm, 10L);

        assertThat(taxRate.getRate()).isEqualTo(0.2);
        assertThat(taxRate.getZipCode()).isEqualTo("70001");
        assertThat(taxRate.getStateOrProvinceId()).isEqualTo(3L);
        assertThat(taxRate.getCountryId()).isEqualTo(1L);
        assertThat(taxRate.getTaxClass()).isEqualTo(newTaxClass);
        verify(taxRateRepository).save(taxRate);
    }

    @Test
    void delete_whenTaxRateMissing_shouldThrowNotFoundException() {
        when(taxRateRepository.existsById(99L)).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> taxRateService.delete(99L));

        assertThat(exception.getMessage()).isEqualTo(MessageCode.TAX_RATE_NOT_FOUND);
    }

    @Test
    void delete_whenTaxRateExists_shouldDeleteById() {
        when(taxRateRepository.existsById(10L)).thenReturn(true);

        taxRateService.delete(10L);

        verify(taxRateRepository).deleteById(10L);
    }

    @Test
    void findById_whenTaxRateExists_shouldMapVm() {
        when(taxRateRepository.findById(10L)).thenReturn(Optional.of(taxRate));

        TaxRateVm result = taxRateService.findById(10L);

        assertThat(result).isEqualTo(TaxRateVm.fromModel(taxRate));
    }

    @Test
    void findById_whenTaxRateMissing_shouldThrowNotFoundException() {
        when(taxRateRepository.findById(10L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> taxRateService.findById(10L));

        assertThat(exception.getMessage()).isEqualTo(MessageCode.TAX_RATE_NOT_FOUND);
    }

    @Test
    void getPageableTaxRates_whenStateListNotEmpty_shouldMapDetailRows() {
        when(taxRateRepository.findAll(PageRequest.of(0, 10)))
            .thenReturn(new PageImpl<>(List.of(taxRate), PageRequest.of(0, 10), 1));
        when(locationService.getStateOrProvinceAndCountryNames(List.of(2L)))
            .thenReturn(List.of(new StateOrProvinceAndCountryGetNameVm(2L, "HCM", "Vietnam")));

        TaxRateListGetVm result = taxRateService.getPageableTaxRates(0, 10);

        assertThat(result.taxRateGetDetailContent()).containsExactly(
            new TaxRateGetDetailVm(10L, 0.1, "70000", "VAT", "HCM", "Vietnam")
        );
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void getPageableTaxRates_whenStateListEmpty_shouldReturnEmptyDetailRows() {
        TaxRate taxRateWithoutState = TaxRate.builder()
            .id(11L)
            .rate(0.05)
            .zipCode(null)
            .stateOrProvinceId(null)
            .countryId(84L)
            .taxClass(taxClass)
            .build();
        when(taxRateRepository.findAll(PageRequest.of(0, 10)))
            .thenReturn(new PageImpl<>(List.of(taxRateWithoutState), PageRequest.of(0, 10), 1));

        TaxRateListGetVm result = taxRateService.getPageableTaxRates(0, 10);

        assertThat(result.taxRateGetDetailContent()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void getTaxPercent_shouldReturnRepositoryValueOrZero() {
        when(taxRateRepository.getTaxPercent(84L, 2L, "70000", 1L)).thenReturn(0.1);
        when(taxRateRepository.getTaxPercent(84L, 3L, "99999", 1L)).thenReturn(null);

        assertThat(taxRateService.getTaxPercent(1L, 84L, 2L, "70000")).isEqualTo(0.1);
        assertThat(taxRateService.getTaxPercent(1L, 84L, 3L, "99999")).isZero();
    }

    @Test
    void getBulkTaxRate_shouldMapRepositoryResult() {
        when(taxRateRepository.getBatchTaxRates(84L, 2L, "70000", Set.of(1L, 2L)))
            .thenReturn(List.of(taxRate));

        List<TaxRateVm> result = taxRateService.getBulkTaxRate(List.of(1L, 2L), 84L, 2L, "70000");

        assertThat(result).containsExactly(TaxRateVm.fromModel(taxRate));
    }
}
