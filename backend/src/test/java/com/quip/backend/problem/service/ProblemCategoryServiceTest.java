package com.quip.backend.problem.service;

import com.quip.backend.authorization.model.MemberChannelAuthorization;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.common.BaseTest;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.problem.dto.request.CreateProblemCategoryRequestDto;
import com.quip.backend.problem.dto.request.GetProblemCategoryRequestDto;
import com.quip.backend.problem.dto.response.GetProblemCategoryResponseDto;
import com.quip.backend.problem.mapper.database.ProblemCategoryMapper;
import com.quip.backend.problem.mapper.dto.request.CreateProblemCategoryRequestDtoMapper;
import com.quip.backend.problem.mapper.dto.response.GetProblemCategoryResponseDtoMapper;
import com.quip.backend.problem.model.ProblemCategory;
import com.quip.backend.server.model.Server;
import com.quip.backend.authorization.context.AuthorizationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ProblemCategoryService}.
 * <p>
 * This test class validates the problem category service functionality including
 * category creation, retrieval, and validation.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
public class ProblemCategoryServiceTest extends BaseTest {

    @InjectMocks
    private ProblemCategoryService problemCategoryService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private ProblemCategoryMapper problemCategoryMapper;

    @Mock
    private GetProblemCategoryResponseDtoMapper getProblemCategoryResponseDtoMapper;

    @Mock
    private CreateProblemCategoryRequestDtoMapper createProblemCategoryRequestDtoMapper;

    private static final long VALID_MEMBER_ID = 1L;
    private static final long VALID_CHANNEL_ID = 10L;
    private static final long VALID_SERVER_ID = 100L;
    private static final long VALID_PROBLEM_CATEGORY_ID = 1000L;
    private static final String VALID_CATEGORY_NAME = "NAME";
    private static final String VALID_DESCRIPTION_NAME = "DESCRIPTION";
    private static final String VALID_OPERATION = "MANAGE_PROBLEM";

    private static final String NAME_EMPTY_MSG = "Validation failed in [Problem Category Creation]: Field 'problemCategoryName' must not be empty.";
    private static final String DESC_EMPTY_MSG = "Validation failed in [Problem Category Creation]: Field 'problemCategoryDescription' must not be empty.";

    private static final String AUTH_TYPE_MANAGE_CATEGORY = "MANAGE_PROBLEM_CATEGORY";
    private static final String AUTH_TYPE_VIEW_CATEGORY = "VIEW_PROBLEM_CATEGORY";
    private static final String CREATE_OPERATION = "Problem Category Creation";
    private static final String GET_OPERATION = "Problem Category Retrieval";

    @BeforeEach
    void setUp() {
        reset(problemCategoryMapper, getProblemCategoryResponseDtoMapper, createProblemCategoryRequestDtoMapper);
    }

    /**
     * Tests for the getServerProblemCategories method which retrieves problem categories for a server.
     * This nested class validates that categories are correctly retrieved and mapped to DTOs.
     */
    @Nested
    @DisplayName("getServerProblemCategories() Tests")
    class GetServerProblemCategoriesTests {

        @Test
        @DisplayName("Should return non-empty list when server has categories")
        void shouldReturnNonEmptyList_WhenServerHasCategories() {
            // Given
            GetProblemCategoryRequestDto requestDto = new GetProblemCategoryRequestDto();
            requestDto.setMemberId(VALID_MEMBER_ID);
            requestDto.setChannelId(VALID_CHANNEL_ID);

            mockValidAuthorizationContext();

            ProblemCategory problemCategory = new ProblemCategory();
            problemCategory.setServerId(VALID_SERVER_ID);

            when(problemCategoryMapper.selectByServerId(VALID_SERVER_ID)).thenReturn(List.of(problemCategory));
            when(getProblemCategoryResponseDtoMapper.toProblemCategoryDto(problemCategory))
                    .thenReturn(new GetProblemCategoryResponseDto());

            // When
            List<GetProblemCategoryResponseDto> result =
                    problemCategoryService.getServerProblemCategories(requestDto);

            // Then
            assertNotNull(result);
            assertFalse(result.isEmpty());

            verify(problemCategoryMapper).selectByServerId(VALID_SERVER_ID);
            verify(getProblemCategoryResponseDtoMapper).toProblemCategoryDto(problemCategory);
            verify(authorizationService).validateAuthorization(VALID_MEMBER_ID, VALID_CHANNEL_ID, AUTH_TYPE_VIEW_CATEGORY, GET_OPERATION);
        }


    }

    /**
     * Tests for the addProblemCategory method which creates a new problem category.
     * This nested class validates that categories are correctly created and validated,
     * and that appropriate exceptions are thrown for invalid inputs.
     */
    @Nested
    @DisplayName("addProblemCategory() Tests")
    class AddProblemCategoryTests {

        @Test
        @DisplayName("Should pass validation when valid request is provided")
        void shouldPassValidation_WhenValidRequest() {
            // Given
            CreateProblemCategoryRequestDto requestDto = buildCategoryRequest("Valid Name", "Valid Description");

            mockValidAuthorizationContext();

            ProblemCategory problemCategory = buildProblemCategory("Valid Name", "Valid Description");
            // Reset serverId to null to test that it gets set properly
            problemCategory.setServerId(null);

            when(createProblemCategoryRequestDtoMapper.toProblemCategory(requestDto)).thenReturn(problemCategory);

            // When & Then
            assertDoesNotThrow(() ->
                    problemCategoryService.addProblemCategory(requestDto)
            );

            // Verify that serverId was set correctly (kills mutation on line 66)
            assertEquals(VALID_SERVER_ID, problemCategory.getServerId());

            verify(createProblemCategoryRequestDtoMapper).toProblemCategory(requestDto);
            verify(authorizationService).validateAuthorization(VALID_MEMBER_ID, VALID_CHANNEL_ID, AUTH_TYPE_MANAGE_CATEGORY, CREATE_OPERATION);
            verify(problemCategoryMapper).insert(problemCategory);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "  ", "\t", "\n"})
        @DisplayName("Should throw ValidationException when problem category name is invalid")
        void shouldThrowValidationException_WhenProblemCategoryNameIsInvalid(String invalidProblemCategoryName) {
            // Given
            CreateProblemCategoryRequestDto requestDto = buildCategoryRequest(invalidProblemCategoryName, VALID_DESCRIPTION_NAME);

            AuthorizationContext context = new AuthorizationContext(null, null, new Server(), null);
            context.server().setId(VALID_SERVER_ID);

            when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
                    .thenReturn(context);

            ProblemCategory category = buildProblemCategory(invalidProblemCategoryName, VALID_DESCRIPTION_NAME);

            when(createProblemCategoryRequestDtoMapper.toProblemCategory(requestDto)).thenReturn(category);

            // When & Then
            assertThatThrownBy(() ->
                    problemCategoryService.addProblemCategory(requestDto)
            )
            .isInstanceOf(ValidationException.class)
            .hasMessage(NAME_EMPTY_MSG);

            verify(createProblemCategoryRequestDtoMapper).toProblemCategory(requestDto);
            verify(authorizationService).validateAuthorization(VALID_MEMBER_ID, VALID_CHANNEL_ID, AUTH_TYPE_MANAGE_CATEGORY, CREATE_OPERATION);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "  ", "\t", "\n"})
        @DisplayName("Should throw ValidationException when problem description is invalid")
        void shouldThrowValidationException_WhenProblemDescriptionIsInvalid(String invalidProblemDescriptionName) {
            // Given
            CreateProblemCategoryRequestDto requestDto = buildCategoryRequest(VALID_CATEGORY_NAME, invalidProblemDescriptionName);

            AuthorizationContext context = new AuthorizationContext(null, null, new Server(), null);
            context.server().setId(VALID_SERVER_ID);

            when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
                    .thenReturn(context);

            ProblemCategory category = buildProblemCategory(VALID_CATEGORY_NAME, invalidProblemDescriptionName);

            when(createProblemCategoryRequestDtoMapper.toProblemCategory(requestDto)).thenReturn(category);

            // When & Then
            assertThatThrownBy(() ->
                    problemCategoryService.addProblemCategory(requestDto)
            )
            .isInstanceOf(ValidationException.class)
            .hasMessage(DESC_EMPTY_MSG);

            verify(authorizationService).validateAuthorization(VALID_MEMBER_ID, VALID_CHANNEL_ID, AUTH_TYPE_MANAGE_CATEGORY, CREATE_OPERATION);
            verify(createProblemCategoryRequestDtoMapper).toProblemCategory(requestDto);
        }


        @Test
        @DisplayName("Should throw ValidationException when problem category name is null")
        void shouldThrowIllegalArgumentException_WhenProblemCategoryNameIsNull() {
            // Given
            CreateProblemCategoryRequestDto requestDto = buildCategoryRequest(null, VALID_DESCRIPTION_NAME);

            AuthorizationContext context = new AuthorizationContext(null, null, new Server(), null);
            context.server().setId(VALID_SERVER_ID);

            when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
                    .thenReturn(context);

            ProblemCategory category = buildProblemCategory(null, VALID_DESCRIPTION_NAME);

            when(createProblemCategoryRequestDtoMapper.toProblemCategory(requestDto)).thenReturn(category);

            // When & Then
            assertThatThrownBy(() ->
                    problemCategoryService.addProblemCategory(requestDto)
            )
            .isInstanceOf(ValidationException.class)
            .hasMessage(NAME_EMPTY_MSG);

            verify(authorizationService).validateAuthorization(VALID_MEMBER_ID, VALID_CHANNEL_ID, AUTH_TYPE_MANAGE_CATEGORY, CREATE_OPERATION);
            verify(createProblemCategoryRequestDtoMapper).toProblemCategory(requestDto);
        }


        @Test
        @DisplayName("Should throw ValidationException when problem category description is null")
        void shouldThrowValidationException_WhenProblemCategoryDescriptionIsNull() {
            // Given
            CreateProblemCategoryRequestDto requestDto = buildCategoryRequest(VALID_CATEGORY_NAME, null);

            AuthorizationContext context = new AuthorizationContext(null, null, new Server(), null);
            context.server().setId(VALID_SERVER_ID);

            when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
                    .thenReturn(context);

            ProblemCategory category = buildProblemCategory(VALID_CATEGORY_NAME, null);

            when(createProblemCategoryRequestDtoMapper.toProblemCategory(requestDto)).thenReturn(category);

            // When & Then
            assertThatThrownBy(() ->
                    problemCategoryService.addProblemCategory(requestDto)
            )
            .isInstanceOf(ValidationException.class)
            .hasMessage(DESC_EMPTY_MSG);

            verify(authorizationService).validateAuthorization(VALID_MEMBER_ID, VALID_CHANNEL_ID, AUTH_TYPE_MANAGE_CATEGORY, CREATE_OPERATION);
            verify(createProblemCategoryRequestDtoMapper).toProblemCategory(requestDto);
        }
    }

    /**
     * Tests for the validateProblemCategory method which ensures a problem category exists and is valid.
     * This nested class contains tests that verify problem category validation logic works correctly
     * for both valid and invalid category IDs.
     */
    @Nested
    @DisplayName("validateProblemCategory() Tests")
    class ValidateProblemCategoryTests {

        @Test
        @DisplayName("Should not throw when category exists")
        void shouldNotThrow_WhenCategoryExists() {
            // Given
            ProblemCategory expectedCategory = new ProblemCategory();
            expectedCategory.setId(VALID_PROBLEM_CATEGORY_ID);
            when(problemCategoryMapper.selectById(VALID_PROBLEM_CATEGORY_ID)).thenReturn(expectedCategory);

            // When
            ProblemCategory result = problemCategoryService.validateProblemCategory(VALID_PROBLEM_CATEGORY_ID, VALID_OPERATION);

            // Then
            assertNotNull(result);
            assertEquals(expectedCategory, result);
            assertEquals(VALID_PROBLEM_CATEGORY_ID, result.getId());

            verify(problemCategoryMapper).selectById(VALID_PROBLEM_CATEGORY_ID);
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, 10L, -1L, 100L})
        @DisplayName("Should throw ValidationException when category does not exist")
        void shouldThrowValidationException_WhenCategoryDoesNotExist(Long invalidProblemCategoryId) {
            // Given
            when(problemCategoryMapper.selectById(invalidProblemCategoryId)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() ->
                    problemCategoryService.validateProblemCategory(
                            invalidProblemCategoryId,
                            VALID_OPERATION
                    )
            )
            .isInstanceOf(ValidationException.class)
            .hasMessage("Validation failed in [" + VALID_OPERATION + "]: Field 'problemCategoryId' must refer to an existing problem category.");

            verify(problemCategoryMapper).selectById(invalidProblemCategoryId);
        }

        @Test
        @DisplayName("Should throw ValidationException when channelId is null")
        void shouldThrowValidationException_WhenProblemCategoryIdIsNull() {
            assertThatThrownBy(() ->
                    problemCategoryService.validateProblemCategory(
                            null,
                            VALID_OPERATION
                    )
            )
            .isInstanceOf(ValidationException.class)
            .hasMessage("Validation failed in [" + VALID_OPERATION + "]: Field 'problemCategoryId' must not be null.");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when operation is null")
        void shouldThrowIllegalArgumentException_WhenOperationIsNull() {
            assertThatThrownBy(() ->
                    problemCategoryService.validateProblemCategory(
                            VALID_CHANNEL_ID,
                            null
                    )
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Parameter 'operation' must not be null.");
        }
    }

    /**
     * Creates a mock AuthorizationContext for testing.
     * <p>
     * This method sets up a valid authorization context with a server ID
     * and configures the authorizationService mock to return it.
     * </p>
     * 
     * @return A mock AuthorizationContext for testing
     */
    private AuthorizationContext mockValidAuthorizationContext() {
        AuthorizationContext context = new AuthorizationContext(null, null, new Server(), new MemberChannelAuthorization());
        context.server().setId(VALID_SERVER_ID);
        when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(context);
        return context;
    }

    /**
     * Creates a ProblemCategory entity with the specified name and description.
     * <p>
     * This method is used to create test data for problem category tests.
     * </p>
     * 
     * @param name The category name
     * @param description The category description
     * @return A ProblemCategory entity with the specified values
     */
    private ProblemCategory buildProblemCategory(String name, String description) {
        ProblemCategory category = new ProblemCategory();
        category.setCategoryName(name);
        category.setDescription(description);
        category.setServerId(VALID_SERVER_ID);
        return category;
    }

    /**
     * Creates a CreateProblemCategoryRequestDto with the specified name and description.
     * <p>
     * This method is used to create test request DTOs for problem category creation tests.
     * </p>
     * 
     * @param name The category name
     * @param description The category description
     * @return A CreateProblemCategoryRequestDto with the specified values
     */
    private CreateProblemCategoryRequestDto buildCategoryRequest(String name, String description) {
        CreateProblemCategoryRequestDto requestDto = new CreateProblemCategoryRequestDto();
        requestDto.setMemberId(VALID_MEMBER_ID);
        requestDto.setChannelId(VALID_CHANNEL_ID);
        requestDto.setProblemCategoryName(name);
        requestDto.setProblemCategoryDescription(description);
        return requestDto;
    }
}
