package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxClass;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class TaxClassServiceTest {

    @Mock
    private TaxClassRepository taxClassRepository;

    @InjectMocks
    private TaxClassService taxClassService;

    private TaxClass taxClass;

    @BeforeEach
    void setUp() {
        taxClass = TaxClass.builder().id(1L).name("VAT").build();
    }

    @Test
    void findAllTaxClasses_shouldReturnSortedMappedList() {
        when(taxClassRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))).thenReturn(List.of(taxClass));

        List<TaxClassVm> result = taxClassService.findAllTaxClasses();

        assertThat(result).containsExactly(TaxClassVm.fromModel(taxClass));
    }

    @Test
    void findById_whenTaxClassExists_shouldReturnVm() {
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));

        TaxClassVm result = taxClassService.findById(1L);

        assertThat(result).isEqualTo(TaxClassVm.fromModel(taxClass));
    }

    @Test
    void findById_whenTaxClassMissing_shouldThrowNotFoundException() {
        when(taxClassRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> taxClassService.findById(1L));

        assertThat(exception.getMessage()).isEqualTo(com.yas.tax.constants.MessageCode.TAX_CLASS_NOT_FOUND);
    }

    @Test
    void create_whenNameDuplicated_shouldThrowDuplicatedException() {
        TaxClassPostVm postVm = new TaxClassPostVm("1", "VAT");
        when(taxClassRepository.existsByName("VAT")).thenReturn(true);

        DuplicatedException exception = assertThrows(DuplicatedException.class, () -> taxClassService.create(postVm));

        assertThat(exception.getMessage()).isEqualTo(com.yas.tax.constants.MessageCode.NAME_ALREADY_EXITED);
    }

    @Test
    void create_whenNameUnique_shouldSaveEntity() {
        TaxClassPostVm postVm = new TaxClassPostVm("1", "VAT");
        when(taxClassRepository.existsByName("VAT")).thenReturn(false);
        when(taxClassRepository.save(any(TaxClass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaxClass result = taxClassService.create(postVm);

        assertThat(result.getName()).isEqualTo("VAT");
    }

    @Test
    void update_whenTaxClassMissing_shouldThrowNotFoundException() {
        TaxClassPostVm postVm = new TaxClassPostVm("1", "GST");
        when(taxClassRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> taxClassService.update(postVm, 1L));

        assertThat(exception.getMessage()).isEqualTo(com.yas.tax.constants.MessageCode.TAX_CLASS_NOT_FOUND);
    }

    @Test
    void update_whenNameDuplicated_shouldThrowDuplicatedException() {
        TaxClassPostVm postVm = new TaxClassPostVm("1", "GST");
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));
        when(taxClassRepository.existsByNameNotUpdatingTaxClass("GST", 1L)).thenReturn(true);

        DuplicatedException exception = assertThrows(DuplicatedException.class,
            () -> taxClassService.update(postVm, 1L));

        assertThat(exception.getMessage()).isEqualTo(com.yas.tax.constants.MessageCode.NAME_ALREADY_EXITED);
    }

    @Test
    void update_whenInputValid_shouldSaveUpdatedEntity() {
        TaxClassPostVm postVm = new TaxClassPostVm("1", "GST");
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));
        when(taxClassRepository.existsByNameNotUpdatingTaxClass("GST", 1L)).thenReturn(false);

        taxClassService.update(postVm, 1L);

        assertThat(taxClass.getName()).isEqualTo("GST");
        verify(taxClassRepository).save(taxClass);
    }

    @Test
    void delete_whenTaxClassMissing_shouldThrowNotFoundException() {
        when(taxClassRepository.existsById(1L)).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class, () -> taxClassService.delete(1L));

        assertThat(exception.getMessage()).isEqualTo(com.yas.tax.constants.MessageCode.TAX_CLASS_NOT_FOUND);
    }

    @Test
    void delete_whenTaxClassExists_shouldDeleteById() {
        when(taxClassRepository.existsById(1L)).thenReturn(true);

        taxClassService.delete(1L);

        verify(taxClassRepository).deleteById(1L);
    }

    @Test
    void getPageableTaxClasses_shouldReturnMappedPage() {
        when(taxClassRepository.findAll(PageRequest.of(0, 10)))
            .thenReturn(new PageImpl<>(List.of(taxClass), PageRequest.of(0, 10), 1));

        TaxClassListGetVm result = taxClassService.getPageableTaxClasses(0, 10);

        assertThat(result.taxClassContent()).containsExactly(TaxClassVm.fromModel(taxClass));
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.isLast()).isTrue();
    }
}
