package com.yas.tax.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.viewmodel.location.StateOrProvinceAndCountryGetNameVm;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
import com.yas.tax.viewmodel.taxrate.TaxRateGetDetailVm;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaxViewModelTest {

    @Test
    void fromModelMethods_shouldMapDomainObjects() {
        TaxClass taxClass = TaxClass.builder().id(1L).name("VAT").build();
        TaxRate taxRate = TaxRate.builder()
            .id(10L)
            .rate(0.1)
            .zipCode("70000")
            .stateOrProvinceId(2L)
            .countryId(84L)
            .taxClass(taxClass)
            .build();

        assertThat(TaxClassVm.fromModel(taxClass)).isEqualTo(new TaxClassVm(1L, "VAT"));
        assertThat(TaxRateVm.fromModel(taxRate)).isEqualTo(new TaxRateVm(10L, 0.1, "70000", 1L, 2L, 84L));
    }

    @Test
    void postRecordsAndLists_shouldExposeAssignedValues() {
        TaxClassPostVm taxClassPostVm = new TaxClassPostVm("1", "VAT");
        TaxRatePostVm taxRatePostVm = new TaxRatePostVm(0.1, "70000", 1L, 2L, 84L);
        TaxClassListGetVm taxClassListGetVm = new TaxClassListGetVm(List.of(new TaxClassVm(1L, "VAT")), 0, 10, 1, 1, true);
        TaxRateGetDetailVm taxRateGetDetailVm = new TaxRateGetDetailVm(10L, 0.1, "70000", "VAT", "HCM", "Vietnam");
        TaxRateListGetVm taxRateListGetVm = new TaxRateListGetVm(List.of(taxRateGetDetailVm), 0, 10, 1, 1, true);
        StateOrProvinceAndCountryGetNameVm locationVm = new StateOrProvinceAndCountryGetNameVm(2L, "HCM", "Vietnam");

        assertThat(taxClassPostVm.toModel().getName()).isEqualTo("VAT");
        assertThat(taxRatePostVm.taxClassId()).isEqualTo(1L);
        assertThat(taxClassListGetVm.totalElements()).isEqualTo(1);
        assertThat(taxRateListGetVm.taxRateGetDetailContent()).containsExactly(taxRateGetDetailVm);
        assertThat(locationVm.countryName()).isEqualTo("Vietnam");
    }
}
