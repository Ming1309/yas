package com.yas.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.inventory.model.Stock;
import com.yas.inventory.model.StockHistory;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.repository.StockHistoryRepository;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.stock.StockQuantityVm;
import com.yas.inventory.viewmodel.stockhistory.StockHistoryListVm;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockHistoryServiceTest {

    @Mock
    private StockHistoryRepository stockHistoryRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private StockHistoryService stockHistoryService;

    private Stock stock;
    private StockQuantityVm stockQuantityVm;
    private Warehouse warehouse;
    private StockHistory stockHistory;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setId(10L);

        stock = new Stock();
        stock.setId(1L);
        stock.setProductId(100L);
        stock.setWarehouse(warehouse);

        stockQuantityVm = new StockQuantityVm(1L, 10L, "Note 1");

        stockHistory = StockHistory.builder()
            .productId(100L)
            .note("Note 1")
            .adjustedQuantity(10L)
            .warehouse(warehouse)
            .build();
    }

    @Test
    void createStockHistories_shouldSaveHistories() {
        when(stockHistoryRepository.saveAll(anyList())).thenReturn(List.of(stockHistory));

        stockHistoryService.createStockHistories(List.of(stock), List.of(stockQuantityVm));

        verify(stockHistoryRepository).saveAll(anyList());
    }

    @Test
    void getStockHistories_shouldReturnList() {
        when(stockHistoryRepository.findByProductIdAndWarehouseIdOrderByCreatedOnDesc(100L, 10L))
            .thenReturn(List.of(stockHistory));
            
        ProductInfoVm productInfoVm = new ProductInfoVm(100L, "Prod", "SKU", true);
        when(productService.getProduct(100L)).thenReturn(productInfoVm);

        StockHistoryListVm result = stockHistoryService.getStockHistories(100L, 10L);

        assertEquals(1, result.data().size());
        assertEquals("Note 1", result.data().get(0).note());
    }
}
