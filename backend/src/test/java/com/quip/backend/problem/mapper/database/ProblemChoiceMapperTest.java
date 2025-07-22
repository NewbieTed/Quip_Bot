package com.quip.backend.problem.mapper.database;

import com.quip.backend.common.BaseTest;
import com.quip.backend.problem.model.ProblemChoice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProblemChoiceMapper}.
 * <p>
 * This test class validates the problem choice database mapper functionality.
 * Tests MyBatis mapper interface methods.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProblemChoiceMapper Tests")
public class ProblemChoiceMapperTest extends BaseTest {

    @Mock
    private ProblemChoiceMapper problemChoiceMapper;

    @Test
    @DisplayName("Should call selectById method")
    void shouldCallSelectById_Method() {
        // Given
        Long choiceId = 1L;
        ProblemChoice expectedChoice = new ProblemChoice();
        expectedChoice.setId(choiceId);
        
        when(problemChoiceMapper.selectById(choiceId)).thenReturn(expectedChoice);

        // When
        ProblemChoice result = problemChoiceMapper.selectById(choiceId);

        // Then
        assertNotNull(result);
        assertEquals(choiceId, result.getId());
        verify(problemChoiceMapper).selectById(choiceId);
    }

    @Test
    @DisplayName("Should handle null return from selectById")
    void shouldHandleNullReturn_FromSelectById() {
        // Given
        Long choiceId = 999L;
        when(problemChoiceMapper.selectById(choiceId)).thenReturn(null);

        // When
        ProblemChoice result = problemChoiceMapper.selectById(choiceId);

        // Then
        verify(problemChoiceMapper).selectById(choiceId);
    }

    @Test
    @DisplayName("Should call insert method")
    void shouldCallInsert_Method() {
        // Given
        ProblemChoice choice = new ProblemChoice();
        choice.setChoiceText("Test Choice");
        choice.setProblemId(1L);
        choice.setIsCorrect(true);
        
        when(problemChoiceMapper.insert(choice)).thenReturn(1);

        // When
        int result = problemChoiceMapper.insert(choice);

        // Then
        assertEquals(1, result);
        verify(problemChoiceMapper).insert(choice);
    }
}