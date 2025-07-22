package com.quip.backend.problem.service;

import com.quip.backend.common.BaseTest;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.problem.dto.request.CreateProblemChoiceRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProblemChoiceService}.
 * <p>
 * This test class validates the problem choice service functionality,
 * particularly focusing on the validation of problem choices.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProblemChoiceService Tests")
public class ProblemChoiceServiceTest extends BaseTest {

    @InjectMocks
    private ProblemChoiceService problemChoiceService;

    private static final String VALID_OPERATION = "testOperation";

    private static final String INVALID_CHOICE_MSG =
        "Validation failed in [Problem Choice Creation]: Field 'choiceText/mediaFileId' must have non-empty text or a valid media file ID for each choice.";

    /**
     * Tests for the validateProblemChoices method which ensures problem choices are valid.
     * This nested class validates that proper validation is performed on problem choices,
     * checking for required fields and valid combinations of text and media.
     */
    @Nested
    @DisplayName("validateProblemChoices() Tests")
    class ValidateProblemChoicesTests {

        @Test
        @DisplayName("Should not throw when all choices are valid (text or media present)")
        void shouldNotThrow_WhenChoicesAreValid() {
            // Given
            List<CreateProblemChoiceRequestDto> choices = new ArrayList<>();
            choices.add(new CreateProblemChoiceRequestDto("Choice 1", null, true));
            choices.add(new CreateProblemChoiceRequestDto(null, 1L, false));
            choices.add(new CreateProblemChoiceRequestDto("Choice 3", 2L, false));

            // When & Then
            assertDoesNotThrow(() -> {
                problemChoiceService.validateProblemChoices(choices, VALID_OPERATION);
            });
        }

        @Test
        @DisplayName("Should throw ValidationException when a choice has neither text nor media")
        void shouldThrowValidationException_WhenChoiceHasNoTextOrMedia() {
            // Given
            List<CreateProblemChoiceRequestDto> choices = new ArrayList<>();
            choices.add(new CreateProblemChoiceRequestDto(null, null, false));

            // When & Then
            assertThatThrownBy(() -> problemChoiceService.validateProblemChoices(choices, VALID_OPERATION))
                .isInstanceOf(ValidationException.class)
                .hasMessage(INVALID_CHOICE_MSG);
        }

        @Test
        @DisplayName("Should not throw when choices list is null or empty")
        void shouldNotThrow_WhenChoicesListIsNullOrEmpty() {
            // Given / When & Then
            assertDoesNotThrow(() -> {
                problemChoiceService.validateProblemChoices(null, VALID_OPERATION);
            });

            assertDoesNotThrow(() -> {
                problemChoiceService.validateProblemChoices(new ArrayList<>(), VALID_OPERATION);
            });
        }
    }
}
