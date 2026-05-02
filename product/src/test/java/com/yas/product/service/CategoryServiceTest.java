package com.yas.product.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Category;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.category.CategoryGetDetailVm;
import com.yas.product.viewmodel.category.CategoryGetVm;
import com.yas.product.viewmodel.category.CategoryListGetVm;
import com.yas.product.viewmodel.category.CategoryPostVm;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MediaService mediaService;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;
    private NoFileMediaVm noFileMediaVm;
    private CategoryPostVm categoryPostVm;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("name");
        category.setSlug("slug");
        category.setDescription("description");
        category.setMetaKeyword("metaKeyword");
        category.setMetaDescription("metaDescription");
        category.setDisplayOrder((short) 1);
        category.setIsPublished(true);
        category.setImageId(1L);

        noFileMediaVm = new NoFileMediaVm(1L, "caption", "fileName", "mediaType", "url");
    }

    @Test
    void getCategoryById_whenFound_returnsDetailVm() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(mediaService.getMedia(1L)).thenReturn(noFileMediaVm);
        
        CategoryGetDetailVm categoryGetDetailVm = categoryService.getCategoryById(1L);
        
        assertNotNull(categoryGetDetailVm);
        assertEquals("name", categoryGetDetailVm.name());
        assertEquals("url", categoryGetDetailVm.categoryImage().url());
    }

    @Test
    void getCategoryById_whenNotFound_throwsNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> categoryService.getCategoryById(99L));
    }

    @Test
    void getCategories_Success() {
        when(categoryRepository.findByNameContainingIgnoreCase("name")).thenReturn(List.of(category));
        when(mediaService.getMedia(1L)).thenReturn(noFileMediaVm);
        
        List<CategoryGetVm> result = categoryService.getCategories("name");
        
        assertEquals(1, result.size());
        assertEquals("name", result.get(0).name());
    }

    @Test
    void getPageableCategories_Success() {
        Page<Category> page = new PageImpl<>(List.of(category), PageRequest.of(0, 10), 1);
        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(page);
        
        CategoryListGetVm result = categoryService.getPageableCategories(0, 10);
        
        assertEquals(1, result.categoryContent().size());
        assertEquals("name", result.categoryContent().get(0).name());
    }

    @Test
    void create_whenDuplicateName_throwsDuplicatedException() {
        categoryPostVm = new CategoryPostVm("name", "slug", "desc", 1L, "metaK", "metaD", (short) 1, true, 1L);
        when(categoryRepository.findExistedName("name", null)).thenReturn(category);
        
        assertThrows(DuplicatedException.class, () -> categoryService.create(categoryPostVm));
    }

    @Test
    void create_whenParentNotFound_throwsBadRequestException() {
        categoryPostVm = new CategoryPostVm("name", "slug", "desc", 99L, "metaK", "metaD", (short) 1, true, 1L);
        when(categoryRepository.findExistedName("name", null)).thenReturn(null);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        
        assertThrows(BadRequestException.class, () -> categoryService.create(categoryPostVm));
    }

    @Test
    void create_whenParentExists_setsParentAndSaves() {
        categoryPostVm = new CategoryPostVm("name", "slug", "desc", 2L, "metaK", "metaD", (short) 1, true, 1L);
        when(categoryRepository.findExistedName("name", null)).thenReturn(null);
        
        Category parent = new Category();
        parent.setId(2L);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        
        Category result = categoryService.create(categoryPostVm);
        
        assertNotNull(result);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void create_whenNoParent_savesCategory() {
        categoryPostVm = new CategoryPostVm("name", "slug", "desc", null, "metaK", "metaD", (short) 1, true, 1L);
        when(categoryRepository.findExistedName("name", null)).thenReturn(null);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        
        Category result = categoryService.create(categoryPostVm);
        
        assertNotNull(result);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void update_whenDuplicateName_throwsDuplicatedException() {
        categoryPostVm = new CategoryPostVm("name", "slug", "desc", null, "metaK", "metaD", (short) 1, true, 1L);
        when(categoryRepository.findExistedName("name", 1L)).thenReturn(new Category());
        
        assertThrows(DuplicatedException.class, () -> categoryService.update(categoryPostVm, 1L));
    }

    @Test
    void update_whenCategoryNotFound_throwsNotFoundException() {
        categoryPostVm = new CategoryPostVm("name", "slug", "desc", null, "metaK", "metaD", (short) 1, true, 1L);
        when(categoryRepository.findExistedName("name", 1L)).thenReturn(null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(NotFoundException.class, () -> categoryService.update(categoryPostVm, 1L));
    }

    @Test
    void update_whenParentIsItself_throwsBadRequestException() {
        categoryPostVm = new CategoryPostVm("name", "slug", "desc", 1L, "metaK", "metaD", (short) 1, true, 1L);
        when(categoryRepository.findExistedName("name", 1L)).thenReturn(null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        
        assertThrows(BadRequestException.class, () -> categoryService.update(categoryPostVm, 1L));
    }

    @Test
    void update_whenParentIsValid_updatesParent() {
        categoryPostVm = new CategoryPostVm("name", "slug", "desc", 2L, "metaK", "metaD", (short) 1, true, 1L);
        when(categoryRepository.findExistedName("name", 1L)).thenReturn(null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        
        Category parent = new Category();
        parent.setId(2L);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(parent));
        
        categoryService.update(categoryPostVm, 1L);
        
        assertEquals(parent, category.getParent());
    }

    @Test
    void update_whenParentNull_clearsParent() {
        categoryPostVm = new CategoryPostVm("name", "slug", "desc", null, "metaK", "metaD", (short) 1, true, 1L);
        when(categoryRepository.findExistedName("name", 1L)).thenReturn(null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        
        categoryService.update(categoryPostVm, 1L);
        
        assertNull(category.getParent());
    }

    @Test
    void getCategoryByIds_returnsFilteredCategories() {
        when(categoryRepository.findAllById(anyList())).thenReturn(List.of(category));
        List<CategoryGetVm> result = categoryService.getCategoryByIds(List.of(1L));
        assertEquals(1, result.size());
    }

    @Test
    void getTopNthCategories_returnsLimitedList() {
        when(categoryRepository.findCategoriesOrderedByProductCount(any(Pageable.class))).thenReturn(List.of("name"));
        List<String> result = categoryService.getTopNthCategories(1);
        assertEquals(1, result.size());
        assertEquals("name", result.get(0));
    }
}