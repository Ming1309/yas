package com.yas.product.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductImage;
import com.yas.product.model.ProductOption;
import com.yas.product.model.ProductOptionCombination;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.product.ProductDetailInfoVm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ProductDetailServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private MediaService mediaService;
    @Mock
    private ProductOptionCombinationRepository productOptionCombinationRepository;

    @InjectMocks
    private ProductDetailService productDetailService;

    private Product mainProduct;

    @BeforeEach
    void setUp() {
        mainProduct = Product.builder()
                .id(1L)
                .name("Main Product")
                .isPublished(true)
                .hasOptions(false)
                .productCategories(new ArrayList<>())
                .attributeValues(new ArrayList<>())
                .productImages(new ArrayList<>())
                .products(new ArrayList<>())
                .build();
    }

    @Test
    void getProductDetailById_whenProductNotFound_throwsNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> productDetailService.getProductDetailById(99L));
    }

    @Test
    void getProductDetailById_whenProductNotPublished_throwsNotFoundException() {
        mainProduct.setPublished(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(mainProduct));
        assertThrows(NotFoundException.class, () -> productDetailService.getProductDetailById(1L));
    }

    @Test
    void getProductDetailById_withoutVariations_returnsDetailWithEmptyVariations() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mainProduct));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertNotNull(result);
        assertTrue(result.getVariations().isEmpty());
        assertEquals("Main Product", result.getName());
    }

    @Test
    void getProductDetailById_withVariations_returnsVariationList() {
        mainProduct.setHasOptions(true);
        Product variant = new Product();
        variant.setId(2L);
        variant.setPublished(true);
        mainProduct.getProducts().add(variant);

        when(productRepository.findById(1L)).thenReturn(Optional.of(mainProduct));
        when(productOptionCombinationRepository.findAllByProduct(variant)).thenReturn(List.of());

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertNotNull(result);
        assertEquals(1, result.getVariations().size());
        assertEquals(2L, result.getVariations().get(0).id());
    }

    @Test
    void getProductDetailById_withThumbnail_includesThumbnailVm() {
        mainProduct.setThumbnailMediaId(10L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(mainProduct));
        when(mediaService.getMedia(10L)).thenReturn(new NoFileMediaVm(10L, "", "", "", "thumbnail_url"));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertNotNull(result);
        assertEquals("thumbnail_url", result.getThumbnail().url());
    }

    @Test
    void getProductDetailById_withProductImages_includesImageList() {
        ProductImage image = new ProductImage();
        image.setImageId(20L);
        mainProduct.getProductImages().add(image);

        when(productRepository.findById(1L)).thenReturn(Optional.of(mainProduct));
        when(mediaService.getMedia(20L)).thenReturn(new NoFileMediaVm(20L, "", "", "", "image_url"));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertNotNull(result);
        assertEquals(1, result.getProductImages().size());
        assertEquals("image_url", result.getProductImages().get(0).url());
    }

    @Test
    void getProductDetailById_withBrand_includesBrandInfo() {
        Brand brand = new Brand();
        brand.setId(5L);
        brand.setName("MyBrand");
        mainProduct.setBrand(brand);

        when(productRepository.findById(1L)).thenReturn(Optional.of(mainProduct));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertNotNull(result);
        assertEquals(5L, result.getBrandId());
        assertEquals("MyBrand", result.getBrandName());
    }

    @Test
    void getProductDetailById_withNullCategories_handlesGracefully() {
        mainProduct.setProductCategories(null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(mainProduct));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(1L);

        assertNotNull(result);
        assertTrue(result.getCategories().isEmpty());
    }
}
