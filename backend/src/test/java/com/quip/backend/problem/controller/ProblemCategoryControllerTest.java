package com.quip.backend.problem.controller;

import com.quip.backend.common.BaseTest;
import com.quip.backend.dto.BaseResponse;
import com.quip.backend.problem.dto.request.CreateProblemCategoryRequestDto;
import com.quip.backend.problem.dto.request.GetProblemCategoryRequestDto;
import com.quip.backend.problem.dto.response.GetProblemCategoryResponseDto;
import com.quip.backend.problem.service.ProblemCategoryService;
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
 * Unit tests for {@link ProblemCategoryController}.
 * 
 * This test class validates the problem category controller functionality including
 * problem category creation and retrieval endpoints.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProblemCategoryController Tests")
class ProblemCategoryControllerTest extends BaseTest {

    @InjectMocks
    private ProblemCategoryController problemCategoryController;

    @Mock
    private ProblemCategoryService problemCategoryService;

    @Mock
    private Validator validator;

    // Test data constants
    private static final Long VALID_CHANNEL_ID = 1L;
    private static final Long VALID_MEMBER_ID = 10L;
    private static final String VALID_CATEGORY_NAME = "Math Problems";
    private static final String VALID_CATEGORY_DESCRIPTION = "Mathematical problem solving questions";

    private CreateProblemCategoryRequestDto validCreateRequest;
    private GetProblemCategoryRequestDto validGetRequest;
    private List<GetProblemCategoryResponseDto> mockCategoryList;

    @BeforeEach
    void setUp() {
        reset(problemCategoryService, validator);
        setupTestData();
    }

    @Test
    @DisplayName("Should instantiate controller successfully")
    void shouldInstantiateController_Successfully() {
        // When & Then
        assertNotNull(problemCategoryController);
        assertNotNull(problemCategoryService);
        assertNotNull(validator);
    }

    @Nested
    @DisplayName("getByChannelId Tests")
    class GetByChannelIdTests {

        @Test
        @DisplayName("Should return problem categories when valid parameters are provided")
        void shouldReturnProblemCategories_WhenValidParametersProvided() {
            // Given
            when(validator.validate(any(GetProblemCategoryRequestDto.class))).thenReturn(Collections.emptySet());
            when(problemCategoryService.getServerProblemCategories(any(GetProblemCategoryRequestDto.class)))
                    .thenReturn(mockCategoryList);

            // When
            BaseResponse<List<GetProblemCategoryResponseDto>> response = problemCategoryController
                    .getByChannelId(VALID_CHANNEL_ID, VALID_MEMBER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getData()).isEqualTo(mockCategoryList);
            assertThat(response.getData()).hasSize(2);

            verify(validator).validate(any(GetProblemCategoryRequestDto.class));
            verify(problemCategoryService).getServerProblemCategories(any(GetProblemCategoryRequestDto.class));
        }

        @Test
        @DisplayName("Should throw ConstraintViolationException when validation fails")
        void shouldThrowConstraintViolationException_WhenValidationFails() {
            // Given
            ConstraintViolation<GetProblemCategoryRequestDto> violation = mock(ConstraintViolation.class);
            Set<ConstraintViolation<GetProblemCategoryRequestDto>> violations = Set.of(violation);
            when(validator.validate(any(GetProblemCategoryRequestDto.class))).thenReturn(violations);

            // When & Then
            assertThatThrownBy(() -> problemCategoryController
                    .getByChannelId(VALID_CHANNEL_ID, VALID_MEMBER_ID))
                    .isInstanceOf(ConstraintViolationException.class);

            verify(validator).validate(any(GetProblemCategoryRequestDto.class));
            verifyNoInteractions(problemCategoryService);
        }

        @Test
        @DisplayName("Should return empty list when no categories found")
        void shouldReturnEmptyList_WhenNoCategoriesFound() {
            // Given
            when(validator.validate(any(GetProblemCategoryRequestDto.class))).thenReturn(Collections.emptySet());
            when(problemCategoryService.getServerProblemCategories(any(GetProblemCategoryRequestDto.class)))
                    .thenReturn(Collections.emptyList());

            // When
            BaseResponse<List<GetProblemCategoryResponseDto>> response = problemCategoryController
                    .getByChannelId(VALID_CHANNEL_ID, VALID_MEMBER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getData()).isEmpty();

            verify(validator).validate(any(GetProblemCategoryRequestDto.class));
            verify(problemCategoryService).getServerProblemCategories(any(GetProblemCategoryRequestDto.class));
        }

        @Test
        @DisplayName("Should handle null channelId parameter")
        void shouldHandleNullChannelId() {
            // Given
            ConstraintViolation<GetProblemCategoryRequestDto> violation = mock(ConstraintViolation.class);
            Set<ConstraintViolation<GetProblemCategoryRequestDto>> violations = Set.of(violation);
            when(validator.validate(any(GetProblemCategoryRequestDto.class))).thenReturn(violations);

            // When & Then
            assertThatThrownBy(() -> problemCategoryController
                    .getByChannelId(null, VALID_MEMBER_ID))
                    .isInstanceOf(ConstraintViolationException.class);

            verify(validator).validate(any(GetProblemCategoryRequestDto.class));
            verifyNoInteractions(problemCategoryService);
        }

        @Test
        @DisplayName("Should handle null memberId parameter")
        void shouldHandleNullMemberId() {
            // Given
            ConstraintViolation<GetProblemCategoryRequestDto> violation = mock(ConstraintViolation.class);
            Set<ConstraintViolation<GetProblemCategoryRequestDto>> violations = Set.of(violation);
            when(validator.validate(any(GetProblemCategoryRequestDto.class))).thenReturn(violations);

            // When & Then
            assertThatThrownBy(() -> problemCategoryController
                    .getByChannelId(VALID_CHANNEL_ID, null))
                    .isInstanceOf(ConstraintViolationException.class);

            verify(validator).validate(any(GetProblemCategoryRequestDto.class));
            verifyNoInteractions(problemCategoryService);
        }

        @Test
        @DisplayName("Should handle negative channelId parameter")
        void shouldHandleNegativeChannelId() {
            // Given
            ConstraintViolation<GetProblemCategoryRequestDto> violation = mock(ConstraintViolation.class);
            Set<ConstraintViolation<GetProblemCategoryRequestDto>> violations = Set.of(violation);
            when(validator.validate(any(GetProblemCategoryRequestDto.class))).thenReturn(violations);

            // When & Then
            assertThatThrownBy(() -> problemCategoryController
                    .getByChannelId(-1L, VALID_MEMBER_ID))
                    .isInstanceOf(ConstraintViolationException.class);

            verify(validator).validate(any(GetProblemCategoryRequestDto.class));
            verifyNoInteractions(problemCategoryService);
        }

        @Test
        @DisplayName("Should handle negative memberId parameter")
        void shouldHandleNegativeMemberId() {
            // Given
            ConstraintViolation<GetProblemCategoryRequestDto> violation = mock(ConstraintViolation.class);
            Set<ConstraintViolation<GetProblemCategoryRequestDto>> violations = Set.of(violation);
            when(validator.validate(any(GetProblemCategoryRequestDto.class))).thenReturn(violations);

            // When & Then
            assertThatThrownBy(() -> problemCategoryController
                    .getByChannelId(VALID_CHANNEL_ID, -1L))
                    .isInstanceOf(ConstraintViolationException.class);

            verify(validator).validate(any(GetProblemCategoryRequestDto.class));
            verifyNoInteractions(problemCategoryService);
        }
    }

    @Nested
    @DisplayName("addProblemCategory Tests")
    class AddProblemCategoryTests {

        @Test
        @DisplayName("Should create problem category when valid request is provided")
        void shouldCreateProblemCategory_WhenValidRequestProvided() {
            // Given
            doNothing().when(problemCategoryService).addProblemCategory(any(CreateProblemCategoryRequestDto.class));

            // When
            BaseResponse<Boolean> response = problemCategoryController.addProblemCategory(validCreateRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getData()).isNull(); // BaseResponse.success() returns null for data

            verify(problemCategoryService).addProblemCategory(validCreateRequest);
        }

        @Test
        @DisplayName("Should handle service exceptions gracefully")
        void shouldHandleServiceExceptions_Gracefully() {
            // Given
            doThrow(new RuntimeException("Service error")).when(problemCategoryService)
                    .addProblemCategory(any(CreateProblemCategoryRequestDto.class));

            // When & Then
            assertThatThrownBy(() -> problemCategoryController.addProblemCategory(validCreateRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Service error");

            verify(problemCategoryService).addProblemCategory(validCreateRequest);
        }

        @Test
        @DisplayName("Should handle null request gracefully")
        void shouldHandleNullRequest_Gracefully() {
            // When & Then - Spring validation will handle null request at framework level
            // This test ensures our controller method can handle null input
            assertDoesNotThrow(() -> {
                BaseResponse<Boolean> response = problemCategoryController.addProblemCategory(null);
                assertThat(response).isNotNull();
                assertThat(response.isStatus()).isTrue();
                assertThat(response.getData()).isNull(); // BaseResponse.success() returns null for data
            });

            verify(problemCategoryService).addProblemCategory(null);
        }

        @Test
        @DisplayName("Should handle request with null channelId")
        void shouldHandleRequestWithNullChannelId() {
            // Given
            CreateProblemCategoryRequestDto requestWithNullChannelId = CreateProblemCategoryRequestDto.builder()
                    .channelId(null)
                    .memberId(VALID_MEMBER_ID)
                    .problemCategoryName(VALID_CATEGORY_NAME)
                    .problemCategoryDescription(VALID_CATEGORY_DESCRIPTION)
                    .build();

            doNothing().when(problemCategoryService).addProblemCategory(any(CreateProblemCategoryRequestDto.class));

            // When
            BaseResponse<Boolean> response = problemCategoryController.addProblemCategory(requestWithNullChannelId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getData()).isNull(); // BaseResponse.success() returns null for data

            verify(problemCategoryService).addProblemCategory(requestWithNullChannelId);
        }

        @Test
        @DisplayName("Should handle request with null memberId")
        void shouldHandleRequestWithNullMemberId() {
            // Given
            CreateProblemCategoryRequestDto requestWithNullMemberId = CreateProblemCategoryRequestDto.builder()
                    .channelId(VALID_CHANNEL_ID)
                    .memberId(null)
                    .problemCategoryName(VALID_CATEGORY_NAME)
                    .problemCategoryDescription(VALID_CATEGORY_DESCRIPTION)
                    .build();

            doNothing().when(problemCategoryService).addProblemCategory(any(CreateProblemCategoryRequestDto.class));

            // When
            BaseResponse<Boolean> response = problemCategoryController.addProblemCategory(requestWithNullMemberId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getData()).isNull(); // BaseResponse.success() returns null for data

            verify(problemCategoryService).addProblemCategory(requestWithNullMemberId);
        }

        @Test
        @DisplayName("Should handle request with null category name")
        void shouldHandleRequestWithNullCategoryName() {
            // Given
            CreateProblemCategoryRequestDto requestWithNullName = CreateProblemCategoryRequestDto.builder()
                    .channelId(VALID_CHANNEL_ID)
                    .memberId(VALID_MEMBER_ID)
                    .problemCategoryName(null)
                    .problemCategoryDescription(VALID_CATEGORY_DESCRIPTION)
                    .build();

            doNothing().when(problemCategoryService).addProblemCategory(any(CreateProblemCategoryRequestDto.class));

            // When
            BaseResponse<Boolean> response = problemCategoryController.addProblemCategory(requestWithNullName);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getData()).isNull(); // BaseResponse.success() returns null for data

            verify(problemCategoryService).addProblemCategory(requestWithNullName);
        }

        @Test
        @DisplayName("Should handle request with blank category name")
        void shouldHandleRequestWithBlankCategoryName() {
            // Given
            CreateProblemCategoryRequestDto requestWithBlankName = CreateProblemCategoryRequestDto.builder()
                    .channelId(VALID_CHANNEL_ID)
                    .memberId(VALID_MEMBER_ID)
                    .problemCategoryName("")
                    .problemCategoryDescription(VALID_CATEGORY_DESCRIPTION)
                    .build();

            doNothing().when(problemCategoryService).addProblemCategory(any(CreateProblemCategoryRequestDto.class));

            // When
            BaseResponse<Boolean> response = problemCategoryController.addProblemCategory(requestWithBlankName);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getData()).isNull(); // BaseResponse.success() returns null for data

            verify(problemCategoryService).addProblemCategory(requestWithBlankName);
        }

        @Test
        @DisplayName("Should handle request with null category description")
        void shouldHandleRequestWithNullCategoryDescription() {
            // Given
            CreateProblemCategoryRequestDto requestWithNullDescription = CreateProblemCategoryRequestDto.builder()
                    .channelId(VALID_CHANNEL_ID)
                    .memberId(VALID_MEMBER_ID)
                    .problemCategoryName(VALID_CATEGORY_NAME)
                    .problemCategoryDescription(null)
                    .build();

            doNothing().when(problemCategoryService).addProblemCategory(any(CreateProblemCategoryRequestDto.class));

            // When
            BaseResponse<Boolean> response = problemCategoryController.addProblemCategory(requestWithNullDescription);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getData()).isNull(); // BaseResponse.success() returns null for data

            verify(problemCategoryService).addProblemCategory(requestWithNullDescription);
        }

        @Test
        @DisplayName("Should handle request with blank category description")
        void shouldHandleRequestWithBlankCategoryDescription() {
            // Given
            CreateProblemCategoryRequestDto requestWithBlankDescription = CreateProblemCategoryRequestDto.builder()
                    .channelId(VALID_CHANNEL_ID)
                    .memberId(VALID_MEMBER_ID)
                    .problemCategoryName(VALID_CATEGORY_NAME)
                    .problemCategoryDescription("")
                    .build();

            doNothing().when(problemCategoryService).addProblemCategory(any(CreateProblemCategoryRequestDto.class));

            // When
            BaseResponse<Boolean> response = problemCategoryController.addProblemCategory(requestWithBlankDescription);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getData()).isNull(); // BaseResponse.success() returns null for data

            verify(problemCategoryService).addProblemCategory(requestWithBlankDescription);
        }

        @Test
        @DisplayName("Should handle request with negative channelId")
        void shouldHandleRequestWithNegativeChannelId() {
            // Given
            CreateProblemCategoryRequestDto requestWithNegativeChannelId = CreateProblemCategoryRequestDto.builder()
                    .channelId(-1L)
                    .memberId(VALID_MEMBER_ID)
                    .problemCategoryName(VALID_CATEGORY_NAME)
                    .problemCategoryDescription(VALID_CATEGORY_DESCRIPTION)
                    .build();

            doNothing().when(problemCategoryService).addProblemCategory(any(CreateProblemCategoryRequestDto.class));

            // When
            BaseResponse<Boolean> response = problemCategoryController.addProblemCategory(requestWithNegativeChannelId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getData()).isNull(); // BaseResponse.success() returns null for data

            verify(problemCategoryService).addProblemCategory(requestWithNegativeChannelId);
        }

        @Test
        @DisplayName("Should handle request with negative memberId")
        void shouldHandleRequestWithNegativeMemberId() {
            // Given
            CreateProblemCategoryRequestDto requestWithNegativeMemberId = CreateProblemCategoryRequestDto.builder()
                    .channelId(VALID_CHANNEL_ID)
                    .memberId(-1L)
                    .problemCategoryName(VALID_CATEGORY_NAME)
                    .problemCategoryDescription(VALID_CATEGORY_DESCRIPTION)
                    .build();

            doNothing().when(problemCategoryService).addProblemCategory(any(CreateProblemCategoryRequestDto.class));

            // When
            BaseResponse<Boolean> response = problemCategoryController.addProblemCategory(requestWithNegativeMemberId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getData()).isNull(); // BaseResponse.success() returns null for data

            verify(problemCategoryService).addProblemCategory(requestWithNegativeMemberId);
        }
    }

    // Helper methods
    private void setupTestData() {
        validCreateRequest = CreateProblemCategoryRequestDto.builder()
                .channelId(VALID_CHANNEL_ID)
                .memberId(VALID_MEMBER_ID)
                .problemCategoryName(VALID_CATEGORY_NAME)
                .problemCategoryDescription(VALID_CATEGORY_DESCRIPTION)
                .build();

        validGetRequest = GetProblemCategoryRequestDto.builder()
                .channelId(VALID_CHANNEL_ID)
                .memberId(VALID_MEMBER_ID)
                .build();

        mockCategoryList = Arrays.asList(
                GetProblemCategoryResponseDto.builder()
                        .problemCategoryId(1L)
                        .categoryName("Math")
                        .description("Mathematics problems")
                        .build(),
                GetProblemCategoryResponseDto.builder()
                        .problemCategoryId(2L)
                        .categoryName("Science")
                        .description("Science problems")
                        .build()
        );
    }
}
