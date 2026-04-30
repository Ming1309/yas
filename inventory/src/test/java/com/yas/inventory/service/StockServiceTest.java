package com.yas.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.commonlibrary.exception.StockExistingException;
import com.yas.inventory.model.Stock;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.model.enumeration.FilterExistInWhSelection;
import com.yas.inventory.repository.StockRepository;
import com.yas.inventory.repository.WarehouseRepository;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.stock.StockPostVm;
import com.yas.inventory.viewmodel.stock.StockQuantityUpdateVm;
import com.yas.inventory.viewmodel.stock.StockQuantityVm;
import com.yas.inventory.viewmodel.stock.StockVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private ProductService productService;
    @Mock
    private WarehouseService warehouseService;
    @Mock
    private StockHistoryService stockHistoryService;

    @InjectMocks
    private StockService stockService;

    private StockPostVm stockPostVm;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        stockPostVm = new StockPostVm(1L, 100L);
        warehouse = new Warehouse();
        warehouse.setId(100L);
    }

    @Test
    void addProductIntoWarehouse_whenStockExists_shouldThrowException() {
        when(stockRepository.existsByWarehouseIdAndProductId(100L, 1L)).thenReturn(true);
        assertThrows(StockExistingException.class, () -> stockService.addProductIntoWarehouse(List.of(stockPostVm)));
    }

    @Test
    void addProductIntoWarehouse_whenProductNotFound_shouldThrowException() {
        when(stockRepository.existsByWarehouseIdAndProductId(100L, 1L)).thenReturn(false);
        when(productService.getProduct(1L)).thenReturn(null);
        assertThrows(NotFoundException.class, () -> stockService.addProductIntoWarehouse(List.of(stockPostVm)));
    }

    @Test
    void addProductIntoWarehouse_whenWarehouseNotFound_shouldThrowException() {
        when(stockRepository.existsByWarehouseIdAndProductId(100L, 1L)).thenReturn(false);
        when(productService.getProduct(1L)).thenReturn(new ProductInfoVm(1L, "Prod", "SKU", true));
        when(warehouseRepository.findById(100L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> stockService.addProductIntoWarehouse(List.of(stockPostVm)));
    }

    @Test
    void addProductIntoWarehouse_whenValid_shouldSaveStocks() {
        when(stockRepository.existsByWarehouseIdAndProductId(100L, 1L)).thenReturn(false);
        when(productService.getProduct(1L)).thenReturn(new ProductInfoVm(1L, "Prod", "SKU", true));
        when(warehouseRepository.findById(100L)).thenReturn(Optional.of(warehouse));

        stockService.addProductIntoWarehouse(List.of(stockPostVm));

        verify(stockRepository).saveAll(anyList());
    }

    @Test
    void getStocksByWarehouseIdAndProductNameAndSku_shouldReturnList() {
        ProductInfoVm productInfoVm = new ProductInfoVm(1L, "Prod", "SKU", true);
        when(warehouseService.getProductWarehouse(100L, "Prod", "SKU", FilterExistInWhSelection.YES))
            .thenReturn(List.of(productInfoVm));

        Stock stock = new Stock();
        stock.setId(10L);
        stock.setProductId(1L);
        stock.setQuantity(5L);
        stock.setReservedQuantity(0L);
        when(stockRepository.findByWarehouseIdAndProductIdIn(100L, List.of(1L)))
            .thenReturn(List.of(stock));

        List<StockVm> result = stockService.getStocksByWarehouseIdAndProductNameAndSku(100L, "Prod", "SKU");

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).productId());
    }

    @Test
    void updateProductQuantityInStock_whenValid_shouldUpdateAndSave() {
        StockQuantityVm sqVm = new StockQuantityVm(10L, 5L, "Note");
        StockQuantityUpdateVm requestBody = new StockQuantityUpdateVm(List.of(sqVm));

        Stock stock = new Stock();
        stock.setId(10L);
        stock.setQuantity(10L);
        stock.setProductId(1L);

        when(stockRepository.findAllById(List.of(10L))).thenReturn(List.of(stock));
        doNothing().when(stockHistoryService).createStockHistories(anyList(), anyList());
        doNothing().when(productService).updateProductQuantity(anyList());

        stockService.updateProductQuantityInStock(requestBody);

        assertEquals(15L, stock.getQuantity()); // 10 + 5
        verify(stockRepository).saveAll(anyList());
        verify(productService).updateProductQuantity(anyList());
    }

    @Test
    void updateProductQuantityInStock_whenNegativeAndExceeds_shouldThrowException() {
        StockQuantityVm sqVm = new StockQuantityVm(10L, -15L, "Note");
        StockQuantityUpdateVm requestBody = new StockQuantityUpdateVm(List.of(sqVm));

        Stock stock = new Stock();
        stock.setId(10L);
        stock.setQuantity(10L);

        when(stockRepository.findAllById(List.of(10L))).thenReturn(List.of(stock));

        assertThrows(BadRequestException.class, () -> stockService.updateProductQuantityInStock(requestBody));
    }
}
