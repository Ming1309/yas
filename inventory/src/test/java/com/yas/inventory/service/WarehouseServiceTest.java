package com.yas.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.model.enumeration.FilterExistInWhSelection;
import com.yas.inventory.repository.StockRepository;
import com.yas.inventory.repository.WarehouseRepository;
import com.yas.inventory.viewmodel.address.AddressDetailVm;
import com.yas.inventory.viewmodel.address.AddressVm;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.warehouse.WarehouseDetailVm;
import com.yas.inventory.viewmodel.warehouse.WarehouseGetVm;
import com.yas.inventory.viewmodel.warehouse.WarehouseListGetVm;
import com.yas.inventory.viewmodel.warehouse.WarehousePostVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;
    
    @Mock
    private StockRepository stockRepository;
    
    @Mock
    private ProductService productService;
    
    @Mock
    private LocationService locationService;

    @InjectMocks
    private WarehouseService warehouseService;

    private Warehouse warehouse;
    private WarehousePostVm warehousePostVm;
    private AddressDetailVm addressDetailVm;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setName("Warehouse 1");
        warehouse.setAddressId(10L);

        warehousePostVm = new WarehousePostVm(
            "Warehouse 1", "John", "123", "Line 1", "Line 2", "City", "Zip", 1L, 1L, 1L);

        addressDetailVm = new AddressDetailVm(
            10L, "John", "123", "Line 1", "Line 2", "City", "Zip", 1L, 1L, 1L);
    }

    @Test
    void findAllWarehouses_shouldReturnList() {
        when(warehouseRepository.findAll()).thenReturn(List.of(warehouse));
        List<WarehouseGetVm> result = warehouseService.findAllWarehouses();
        assertEquals(1, result.size());
        assertEquals("Warehouse 1", result.get(0).name());
    }

    @Test
    void getProductWarehouse_whenExistStatus_shouldReturnProducts() {
        List<Long> productIds = List.of(100L);
        when(stockRepository.getProductIdsInWarehouse(1L)).thenReturn(productIds);
        
        ProductInfoVm productInfoVm = new ProductInfoVm(100L, "Prod", "SKU", false);
        when(productService.filterProducts("Prod", "SKU", productIds, FilterExistInWhSelection.YES))
            .thenReturn(List.of(productInfoVm));

        List<ProductInfoVm> result = warehouseService.getProductWarehouse(1L, "Prod", "SKU", FilterExistInWhSelection.YES);
        assertEquals(1, result.size());
        assertEquals(true, result.get(0).isAllowedToOrder()); // because 100L is in productIds
    }

    @Test
    void findById_whenExists_shouldReturnDetail() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(locationService.getAddressById(10L)).thenReturn(addressDetailVm);

        WarehouseDetailVm result = warehouseService.findById(1L);
        assertEquals("Warehouse 1", result.name());
        assertEquals("John", result.contactName());
    }

    @Test
    void findById_whenNotFound_shouldThrowException() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> warehouseService.findById(1L));
    }

    @Test
    void create_whenNameExists_shouldThrowException() {
        when(warehouseRepository.existsByName("Warehouse 1")).thenReturn(true);
        assertThrows(DuplicatedException.class, () -> warehouseService.create(warehousePostVm));
    }

    @Test
    void create_whenValid_shouldCreateWarehouse() {
        when(warehouseRepository.existsByName("Warehouse 1")).thenReturn(false);
        AddressVm addressVm = new AddressVm(10L, "John", "123", "Line 1", "Line 2", "City", "Zip", 1L, 1L, 1L);
        when(locationService.createAddress(any())).thenReturn(addressVm);
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(warehouse);

        Warehouse result = warehouseService.create(warehousePostVm);
        assertNotNull(result);
        assertEquals("Warehouse 1", result.getName());
        assertEquals(10L, result.getAddressId());
    }

    @Test
    void update_whenNotFound_shouldThrowException() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> warehouseService.update(warehousePostVm, 1L));
    }

    @Test
    void update_whenNameExistsWithDifferentId_shouldThrowException() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.existsByNameWithDifferentId("Warehouse 1", 1L)).thenReturn(true);
        assertThrows(DuplicatedException.class, () -> warehouseService.update(warehousePostVm, 1L));
    }

    @Test
    void update_whenValid_shouldUpdateWarehouse() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.existsByNameWithDifferentId("Warehouse 1", 1L)).thenReturn(false);
        doNothing().when(locationService).updateAddress(any(), any());
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(warehouse);

        warehouseService.update(warehousePostVm, 1L);
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    void delete_whenValid_shouldDeleteWarehouse() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        doNothing().when(warehouseRepository).deleteById(1L);
        doNothing().when(locationService).deleteAddress(10L);

        warehouseService.delete(1L);
        verify(warehouseRepository).deleteById(1L);
        verify(locationService).deleteAddress(10L);
    }

    @Test
    void getPageableWarehouses_shouldReturnPage() {
        Page<Warehouse> page = new PageImpl<>(List.of(warehouse), PageRequest.of(0, 10), 1);
        when(warehouseRepository.findAll(any(Pageable.class))).thenReturn(page);

        WarehouseListGetVm result = warehouseService.getPageableWarehouses(0, 10);
        assertEquals(1, result.warehouseContent().size());
        assertEquals(1, result.totalElements());
    }
}
