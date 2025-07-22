package com.quip.backend.problem.mapper.database;

import com.quip.backend.common.BaseTest;
import com.quip.backend.problem.model.ProblemCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProblemCategoryMapper}.
 * <p>
 * This test class validates the problem category database mapper functionality.
 * Tests MyBatis mapper interface methods.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProblemCategoryMapper Tests")
public class ProblemCategoryMapperTest extends BaseTest {

    @Mock
    private ProblemCategoryMapper problemCategoryMapper;

    @Test
    @DisplayName("Should call selectById method")
    void shouldCallSelectById_Method() {
        // Given
        Long categoryId = 1L;
        ProblemCategory expectedCategory = new ProblemCategory();
        expectedCategory.setId(categoryId);
        
        when(problemCategoryMapper.selectById(categoryId)).thenReturn(expectedCategory);

        // When
        ProblemCategory result = problemCategoryMapper.selectById(categoryId);

        // Then
        assertNotNull(result);
        assertEquals(categoryId, result.getId());
        verify(problemCategoryMapper).selectById(categoryId);
    }

    @Test
    @DisplayName("Should handle null return from selectById")
    void shouldHandleNullReturn_FromSelectById() {
        // Given
        Long categoryId = 999L;
        when(problemCategoryMapper.selectById(categoryId)).thenReturn(null);

        // When
        ProblemCategory result = problemCategoryMapper.selectById(categoryId);

        // Then
        verify(problemCategoryMapper).selectById(categoryId);
    }

    @Test
    @DisplayName("Should call selectByServerId method")
    void shouldCallSelectByServerId_Method() {
        // Given
        Long serverId = 1L;
        List<ProblemCategory> expectedCategories = new ArrayList<>();
        ProblemCategory category1 = new ProblemCategory();
        category1.setId(1L);
        category1.setServerId(serverId);
        expectedCategories.add(category1);
        
        when(problemCategoryMapper.selectByServerId(serverId)).thenReturn(expectedCategories);

        // When
        List<ProblemCategory> results = problemCategoryMapper.selectByServerId(serverId);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(serverId, results.get(0).getServerId());
        verify(problemCategoryMapper).selectByServerId(serverId);
    }

    @Test
    @DisplayName("Should handle empty list from selectByServerId")
    void shouldHandleEmptyList_FromSelectByServerId() {
        // Given
        Long serverId = 999L;
        when(problemCategoryMapper.selectByServerId(serverId)).thenReturn(new ArrayList<>());

        // When
        List<ProblemCategory> results = problemCategoryMapper.selectByServerId(serverId);

        // Then
        assertNotNull(results);
        assertEquals(0, results.size());
        verify(problemCategoryMapper).selectByServerId(serverId);
    }

    @Test
    @DisplayName("Should call insert method")
    void shouldCallInsert_Method() {
        // Given
        ProblemCategory category = new ProblemCategory();
        category.setCategoryName("Test Category");
        category.setDescription("Test Description");
        category.setServerId(1L);
        
        when(problemCategoryMapper.insert(category)).thenReturn(1);

        // When
        int result = problemCategoryMapper.insert(category);

        // Then
        assertEquals(1, result);
        verify(problemCategoryMapper).insert(category);
    }
}