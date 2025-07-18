package com.quip.backend.problem.controller;

import com.quip.backend.common.BaseTest;
import com.quip.backend.dto.BaseResponse;
import com.quip.backend.problem.dto.request.CreateProblemChoiceRequestDto;
import com.quip.backend.problem.dto.request.CreateProblemRequestDto;
import com.quip.backend.problem.dto.request.GetProblemRequestDto;
import com.quip.backend.problem.dto.response.GetProblemListItemResponseDto;
import com.quip.backend.problem.service.ProblemService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ProblemController}.
 * 
 * This test class validates the problem controller functionality including
 * problem creation and retrieval endpoints.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProblemController Tests")
class ProblemControllerTest extends BaseTest {

    @InjectMocks
    private ProblemController problemController;

    @Mock
    private ProblemService problemService;

    @Mock
    private Validator validator;

    // Test data constants
    private static final Long VALID_CHANNEL_ID = 1L;
    private static final Long VALID_MEMBER_ID = 10L;
    private static final Long VALID_PROBLEM_CATEGORY_ID = 100L;
    private static final Long VALID_MEDIA_FILE_ID = 1000L;
    private static final String VALID_QUESTION = "What is the capital of France?";

    private CreateProblemRequestDto validCreateRequest;
    private GetProblemRequestDto validGetRequest;
    private List<GetProblemListItemResponseDto> mockProblemList;

    @BeforeEach
    void setUp() {
        reset(problemService, validator);
        setupTestData();
    }

    @Test
    @DisplayName("Should instantiate controller successfully")
    void shouldInstantiateController_Successfully() {
        // When & Then
        assertNotNull(problemController);
        assertNotNull(problemService);
        assertNotNull(validator);
    }

    /**
     * Tests for the getProblemsByCategory endpoint which retrieves problems by their category.
     * This nested class validates that the endpoint correctly processes requests,
     * validates parameters, and returns appropriate responses.
     */
    @Nested
    @DisplayName("getProblemsByCategory Tests")
    class GetProblemsByCategoryTests {

        @Test
        @DisplayName("Should return problems when valid parameters are provided")
        void shouldReturnProblems_WhenValidParametersProvided() {
            // Given
            when(validator.validate(any(GetProblemRequestDto.class))).thenReturn(Collections.emptySet());
            when(problemService.getProblemsByCategory(any(GetProblemRequestDto.class)))
                    .thenReturn(mockProblemList);

            // When
            BaseResponse<List<GetProblemListItemResponseDto>> response = problemController
                    .getProblemsByCategory(VALID_CHANNEL_ID, VALID_MEMBER_ID, VALID_PROBLEM_CATEGORY_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getData()).isEqualTo(mockProblemList);
            assertThat(response.getData()).hasSize(2);

            verify(validator).validate(any(GetProblemRequestDto.class));
            verify(problemService).getProblemsByCategory(any(GetProblemRequestDto.class));
        }

        @Test
        @DisplayName("Should throw ConstraintViolationException when validation fails")
        void shouldThrowConstraintViolationException_WhenValidationFails() {
            // Given
            ConstraintViolation<GetProblemRequestDto> violation = mock(ConstraintViolation.class);
            Set<ConstraintViolation<GetProblemRequestDto>> violations = Set.of(violation);
            when(validator.validate(any(GetProblemRequestDto.class))).thenReturn(violations);

            // When & Then
            assertThatThrownBy(() -> problemController
                    .getProblemsByCategory(VALID_CHANNEL_ID, VALID_MEMBER_ID, VALID_PROBLEM_CATEGORY_ID))
                    .isInstanceOf(ConstraintViolationException.class);

            verify(validator).validate(any(GetProblemRequestDto.class));
            verifyNoInteractions(problemService);
        }

        @Test
        @DisplayName("Should return empty list when no problems found")
        void shouldReturnEmptyList_WhenNoProblemsFound() {
            // Given
            when(validator.validate(any(GetProblemRequestDto.class))).thenReturn(Collections.emptySet());
            when(problemService.getProblemsByCategory(any(GetProblemRequestDto.class)))
                    .thenReturn(Collections.emptyList());

            // When
            BaseResponse<List<GetProblemListItemResponseDto>> response = problemController
                    .getProblemsByCategory(VALID_CHANNEL_ID, VALID_MEMBER_ID, VALID_PROBLEM_CATEGORY_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getData()).isEmpty();

            verify(validator).validate(any(GetProblemRequestDto.class));
            verify(problemService).getProblemsByCategory(any(GetProblemRequestDto.class));
        }

        @Test
        @DisplayName("Should handle null channelId parameter")
        void shouldHandleNullChannelId() {
            // Given
            ConstraintViolation<GetProblemRequestDto> violation = mock(ConstraintViolation.class);
            Set<ConstraintViolation<GetProblemRequestDto>> violations = Set.of(violation);
            when(validator.validate(any(GetProblemRequestDto.class))).thenReturn(violations);

            // When & Then
            assertThatThrownBy(() -> problemController
                    .getProblemsByCategory(null, VALID_MEMBER_ID, VALID_PROBLEM_CATEGORY_ID))
                    .isInstanceOf(ConstraintViolationException.class);

            verify(validator).validate(any(GetProblemRequestDto.class));
            verifyNoInteractions(problemService);
        }

        @Test
        @DisplayName("Should handle null memberId parameter")
        void shouldHandleNullMemberId() {
            // Given
            ConstraintViolation<GetProblemRequestDto> violation = mock(ConstraintViolation.class);
            Set<ConstraintViolation<GetProblemRequestDto>> violations = Set.of(violation);
            when(validator.validate(any(GetProblemRequestDto.class))).thenReturn(violations);

            // When & Then
            assertThatThrownBy(() -> problemController
                    .getProblemsByCategory(VALID_CHANNEL_ID, null, VALID_PROBLEM_CATEGORY_ID))
                    .isInstanceOf(ConstraintViolationException.class);

            verify(validator).validate(any(GetProblemRequestDto.class));
            verifyNoInteractions(problemService);
        }

        @Test
        @DisplayName("Should handle null problemCategoryId parameter")
        void shouldHandleNullProblemCategoryId() {
            // Given
            ConstraintViolation<GetProblemRequestDto> violation = mock(ConstraintViolation.class);
            Set<ConstraintViolation<GetProblemRequestDto>> violations = Set.of(violation);
            when(validator.validate(any(GetProblemRequestDto.class))).thenReturn(violations);

            // When & Then
            assertThatThrownBy(() -> problemController
                    .getProblemsByCategory(VALID_CHANNEL_ID, VALID_MEMBER_ID, null))
                    .isInstanceOf(ConstraintViolationException.class);

            verify(validator).validate(any(GetProblemRequestDto.class));
            verifyNoInteractions(problemService);
        }

        @Test
        @DisplayName("Should handle negative channelId parameter")
        void shouldHandleNegativeChannelId() {
            // Given
            ConstraintViolation<GetProblemRequestDto> violation = mock(ConstraintViolation.class);
            Set<ConstraintViolation<GetProblemRequestDto>> violations = Set.of(violation);
            when(validator.validate(any(GetProblemRequestDto.class))).thenReturn(violations);

            // When & Then
            assertThatThrownBy(() -> problemController
                    .getProblemsByCategory(-1L, VALID_MEMBER_ID, VALID_PROBLEM_CATEGORY_ID))
                    .isInstanceOf(ConstraintViolationException.class);

            verify(validator).validate(any(GetProblemRequestDto.class));
            verifyNoInteractions(problemService);
        }
    }

    /**
     * Tests for the addProblem endpoint which creates a new problem.
     * This nested class validates that the endpoint correctly processes requests,
     * handles various input scenarios, and returns appropriate responses.
     */
    @Nested
    @DisplayName("addProblem Tests")
    class AddProblemTests {

        @Test
        @DisplayName("Should create problem when valid request is provided")
        void shouldCreateProblem_WhenValidRequestProvided() {
            // Given
            doNothing().when(problemService).addProblem(any(CreateProblemRequestDto.class));

            // When
            BaseResponse<Boolean> response = problemController.addProblem(validCreateRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getData()).isNull(); // BaseResponse.success() returns null for data

            verify(problemService).addProblem(validCreateRequest);
        }

        @Test
        @DisplayName("Should handle service exceptions gracefully")
        void shouldHandleServiceExceptions_Gracefully() {
            // Given
            doThrow(new RuntimeException("Service error")).when(problemService)
                    .addProblem(any(CreateProblemRequestDto.class));

            // When & Then
            assertThatThrownBy(() -> problemController.addProblem(validCreateRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Service error");

            verify(problemService).addProblem(validCreateRequest);
        }

        @Test
        @DisplayName("Should accept valid request with mediaFileId")
        void shouldAcceptValidRequest_WithMediaFileId() {
            // Given
            CreateProblemRequestDto requestWithMedia = CreateProblemRequestDto.builder()
                    .question(VALID_QUESTION)
                    .choices(createValidChoices())
                    .mediaFileId(VALID_MEDIA_FILE_ID)
                    .channelId(VALID_CHANNEL_ID)
                    .problemCategoryId(VALID_PROBLEM_CATEGORY_ID)
                    .memberId(VALID_MEMBER_ID)
                    .build();

            doNothing().when(problemService).addProblem(any(CreateProblemRequestDto.class));

            // When
            BaseResponse<Boolean> response = problemController.addProblem(requestWithMedia);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getData()).isNull(); // BaseResponse.success() returns null for data

            verify(problemService).addProblem(requestWithMedia);
        }

        @Test
        @DisplayName("Should accept valid request without mediaFileId")
        void shouldAcceptValidRequest_WithoutMediaFileId() {
            // Given
            CreateProblemRequestDto requestWithoutMedia = CreateProblemRequestDto.builder()
                    .question(VALID_QUESTION)
                    .choices(createValidChoices())
                    .mediaFileId(null)
                    .channelId(VALID_CHANNEL_ID)
                    .problemCategoryId(VALID_PROBLEM_CATEGORY_ID)
                    .memberId(VALID_MEMBER_ID)
                    .build();

            doNothing().when(problemService).addProblem(any(CreateProblemRequestDto.class));

            // When
            BaseResponse<Boolean> response = problemController.addProblem(requestWithoutMedia);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getData()).isNull(); // BaseResponse.success() returns null for data

            verify(problemService).addProblem(requestWithoutMedia);
        }

        @Test
        @DisplayName("Should handle null request gracefully")
        void shouldHandleNullRequest_Gracefully() {
            // When & Then - Spring validation will handle null request at framework level
            // This test ensures our controller method can handle null input
            assertDoesNotThrow(() -> {
                BaseResponse<Boolean> response = problemController.addProblem(null);
                assertThat(response).isNotNull();
                assertThat(response.isStatus()).isTrue();
                assertThat(response.getData()).isNull(); // BaseResponse.success() returns null for data
            });

            verify(problemService).addProblem(null);
        }

        @Test
        @DisplayName("Should handle request with empty choices")
        void shouldHandleRequestWithEmptyChoices() {
            // Given
            CreateProblemRequestDto requestWithEmptyChoices = CreateProblemRequestDto.builder()
                    .question(VALID_QUESTION)
                    .choices(Collections.emptyList())
                    .channelId(VALID_CHANNEL_ID)
                    .problemCategoryId(VALID_PROBLEM_CATEGORY_ID)
                    .memberId(VALID_MEMBER_ID)
                    .build();

            doNothing().when(problemService).addProblem(any(CreateProblemRequestDto.class));

            // When
            BaseResponse<Boolean> response = problemController.addProblem(requestWithEmptyChoices);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getData()).isNull(); // BaseResponse.success() returns null for data

            verify(problemService).addProblem(requestWithEmptyChoices);
        }

        @Test
        @DisplayName("Should handle request with null question")
        void shouldHandleRequestWithNullQuestion() {
            // Given
            CreateProblemRequestDto requestWithNullQuestion = CreateProblemRequestDto.builder()
                    .question(null)
                    .choices(createValidChoices())
                    .channelId(VALID_CHANNEL_ID)
                    .problemCategoryId(VALID_PROBLEM_CATEGORY_ID)
                    .memberId(VALID_MEMBER_ID)
                    .build();

            doNothing().when(problemService).addProblem(any(CreateProblemRequestDto.class));

            // When
            BaseResponse<Boolean> response = problemController.addProblem(requestWithNullQuestion);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getData()).isNull(); // BaseResponse.success() returns null for data

            verify(problemService).addProblem(requestWithNullQuestion);
        }
    }

    /**
     * Helper methods for test data setup and creation
     */
    
    /**
     * Sets up test data for problem controller tests.
     * This method initializes valid request DTOs and mock response data.
     */
    private void setupTestData() {
        validCreateRequest = CreateProblemRequestDto.builder()
                .question(VALID_QUESTION)
                .choices(createValidChoices())
                .mediaFileId(VALID_MEDIA_FILE_ID)
                .channelId(VALID_CHANNEL_ID)
                .problemCategoryId(VALID_PROBLEM_CATEGORY_ID)
                .memberId(VALID_MEMBER_ID)
                .build();

        validGetRequest = GetProblemRequestDto.builder()
                .channelId(VALID_CHANNEL_ID)
                .memberId(VALID_MEMBER_ID)
                .problemCategoryId(VALID_PROBLEM_CATEGORY_ID)
                .build();

        mockProblemList = Arrays.asList(
                GetProblemListItemResponseDto.builder()
                        .question("Question 1")
                        .build(),
                GetProblemListItemResponseDto.builder()
                        .question("Question 2")
                        .build()
        );
    }

    /**
     * Creates a list of valid problem choices for testing.
     * 
     * @return A list of CreateProblemChoiceRequestDto objects with valid test data
     */
    private List<CreateProblemChoiceRequestDto> createValidChoices() {
        return Arrays.asList(
                CreateProblemChoiceRequestDto.builder()
                        .choiceText("Paris")
                        .isCorrect(true)
                        .build(),
                CreateProblemChoiceRequestDto.builder()
                        .choiceText("London")
                        .isCorrect(false)
                        .build(),
                CreateProblemChoiceRequestDto.builder()
                        .choiceText("Berlin")
                        .isCorrect(false)
                        .build()
        );
    }
}
