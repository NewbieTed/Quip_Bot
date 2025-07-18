package com.quip.backend.problem.mapper.database;

import com.quip.backend.common.BaseTest;
import com.quip.backend.problem.model.Problem;
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
 * Unit tests for {@link ProblemMapper}.
 * <p>
 * This test class validates the problem database mapper functionality.
 * Tests MyBatis mapper interface methods.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProblemMapper Tests")
public class ProblemMapperTest extends BaseTest {

    @Mock
    private ProblemMapper problemMapper;

    @Test
    @DisplayName("Should call selectById method")
    void shouldCallSelectById_Method() {
        // Given
        Long problemId = 1L;
        Problem expectedProblem = new Problem();
        expectedProblem.setId(problemId);
        
        when(problemMapper.selectById(problemId)).thenReturn(expectedProblem);

        // When
        Problem result = problemMapper.selectById(problemId);

        // Then
        assertNotNull(result);
        assertEquals(problemId, result.getId());
        verify(problemMapper).selectById(problemId);
    }

    @Test
    @DisplayName("Should handle null return from selectById")
    void shouldHandleNullReturn_FromSelectById() {
        // Given
        Long problemId = 999L;
        when(problemMapper.selectById(problemId)).thenReturn(null);

        // When
        Problem result = problemMapper.selectById(problemId);

        // Then
        verify(problemMapper).selectById(problemId);
    }

    @Test
    @DisplayName("Should call selectByProblemCategoryId method")
    void shouldCallSelectByProblemCategoryId_Method() {
        // Given
        Long categoryId = 1L;
        List<Problem> expectedProblems = new ArrayList<>();
        Problem problem1 = new Problem();
        problem1.setId(1L);
        problem1.setProblemCategoryId(categoryId);
        expectedProblems.add(problem1);
        
        when(problemMapper.selectByProblemCategoryId(categoryId)).thenReturn(expectedProblems);

        // When
        List<Problem> results = problemMapper.selectByProblemCategoryId(categoryId);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(categoryId, results.get(0).getProblemCategoryId());
        verify(problemMapper).selectByProblemCategoryId(categoryId);
    }

    @Test
    @DisplayName("Should handle empty list from selectByProblemCategoryId")
    void shouldHandleEmptyList_FromSelectByProblemCategoryId() {
        // Given
        Long categoryId = 999L;
        when(problemMapper.selectByProblemCategoryId(categoryId)).thenReturn(new ArrayList<>());

        // When
        List<Problem> results = problemMapper.selectByProblemCategoryId(categoryId);

        // Then
        assertNotNull(results);
        assertEquals(0, results.size());
        verify(problemMapper).selectByProblemCategoryId(categoryId);
    }

    @Test
    @DisplayName("Should call insert method")
    void shouldCallInsert_Method() {
        // Given
        Problem problem = new Problem();
        problem.setQuestion("Test Question");
        problem.setProblemCategoryId(1L);
        
        when(problemMapper.insert(problem)).thenReturn(1);

        // When
        int result = problemMapper.insert(problem);

        // Then
        assertEquals(1, result);
        verify(problemMapper).insert(problem);
    }
}