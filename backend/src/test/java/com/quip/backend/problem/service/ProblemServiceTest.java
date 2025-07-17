package com.quip.backend.problem.service;

import com.quip.backend.authorization.context.AuthorizationContext;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.channel.model.Channel;
import com.quip.backend.channel.service.ChannelService;
import com.quip.backend.common.BaseTest;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.member.model.Member;
import com.quip.backend.member.service.MemberService;
import com.quip.backend.problem.dto.request.CreateProblemChoiceRequestDto;
import com.quip.backend.problem.dto.request.CreateProblemRequestDto;
import com.quip.backend.problem.dto.request.GetProblemRequestDto;
import com.quip.backend.problem.dto.response.GetProblemListItemResponseDto;
import com.quip.backend.problem.dto.response.GetProblemResponseDto;
import com.quip.backend.problem.mapper.database.ProblemCategoryMapper;
import com.quip.backend.problem.mapper.database.ProblemChoiceMapper;
import com.quip.backend.problem.mapper.database.ProblemMapper;
import com.quip.backend.problem.mapper.dto.request.CreateProblemChoiceRequestDtoMapper;
import com.quip.backend.problem.mapper.dto.request.CreateProblemRequestDtoMapper;
import com.quip.backend.problem.mapper.dto.response.GetProblemListItemResponseDtoMapper;
import com.quip.backend.problem.model.Problem;
import com.quip.backend.problem.model.ProblemCategory;
import com.quip.backend.problem.model.ProblemChoice;
import com.quip.backend.server.model.Server;
import com.quip.backend.server.service.ServerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProblemService Tests")
public class ProblemServiceTest extends BaseTest {

    @InjectMocks
    private ProblemService problemService;

    @Mock
    private ProblemCategoryService problemCategoryService;

    @Mock
    private ProblemChoiceService problemChoiceService;

    @Mock
    private MemberService memberService;

    @Mock
    private ChannelService channelService;

    @Mock
    private ServerService serverService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private ProblemMapper problemMapper;

    @Mock
    private CreateProblemRequestDtoMapper createProblemRequestDtoMapper;

    @Mock
    private ProblemCategoryMapper problemCategoryMapper;

    @Mock
    private ProblemChoiceMapper problemChoiceMapper;

    @Mock
    private CreateProblemChoiceRequestDtoMapper createProblemChoiceRequestDtoMapper;

    @Mock
    private GetProblemListItemResponseDtoMapper getProblemListItemResponseDtoMapper;

    private static final long VALID_MEMBER_ID = 1L;
    private static final long VALID_CHANNEL_ID = 2L;
    private static final long VALID_SERVER_ID = 3L;
    private static final long VALID_CATEGORY_ID = 4L;
    private static final String VALID_QUESTION = "What is 2 + 2?";
    private static final String CREATE_OPERATION = "Problem Creation";
    private static final String RETRIEVE_OPERATION = "Problem Retrieval";

    private CreateProblemRequestDto validCreateProblemRequest;
    private GetProblemRequestDto validGetProblemRequest;
    private AuthorizationContext mockAuthorizationContext;
    private ProblemCategory mockProblemCategory;

    @BeforeEach
    void setUp() {
        reset(memberService, channelService, serverService, authorizationService,
                problemMapper, problemCategoryService, problemChoiceService,
                createProblemRequestDtoMapper, getProblemListItemResponseDtoMapper,
                problemCategoryMapper, problemChoiceMapper, createProblemChoiceRequestDtoMapper);

        // Setup common test data
        setupValidCreateProblemRequest();
        setupValidGetProblemRequest();
        setupMockAuthorizationContext();
        setupMockProblemCategory();
    }

    @Nested
    @DisplayName("getProblem() Tests")
    class GetProblemTests {

        @Test
        @DisplayName("Should return null as placeholder implementation")
        void shouldReturnNull_AsPlaceholderImplementation() {
            // When
            GetProblemResponseDto result = problemService.getProblem();

            // Then
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("getProblemsByCategory() Tests")
    class GetProblemsByCategoryTests {

        @Test
        @DisplayName("Should return non-empty list when category has problems")
        void shouldReturnNonEmptyList_WhenCategoryHasProblems() {
            // Given
            when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
                    .thenReturn(mockAuthorizationContext);
            when(problemCategoryService.validateProblemCategory(anyLong(), anyString())).thenReturn(mockProblemCategory);

            Problem mockProblem = new Problem();
            mockProblem.setId(1L);
            mockProblem.setQuestion(VALID_QUESTION);
            
            when(problemMapper.selectByProblemCategoryId(VALID_CATEGORY_ID)).thenReturn(List.of(mockProblem));
            
            GetProblemListItemResponseDto mockResponseDto = new GetProblemListItemResponseDto();
            when(getProblemListItemResponseDtoMapper.toGetProblemListResponseDto(mockProblem))
                    .thenReturn(mockResponseDto);

            // When
            List<GetProblemListItemResponseDto> result = problemService.getProblemsByCategory(validGetProblemRequest);

            // Then
            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            assertEquals(mockResponseDto, result.get(0));

            verify(authorizationService).validateAuthorization(VALID_MEMBER_ID, VALID_CHANNEL_ID, "VIEW_PROBLEM", RETRIEVE_OPERATION);
            verify(problemCategoryService).validateProblemCategory(VALID_CATEGORY_ID, RETRIEVE_OPERATION);
            verify(problemMapper).selectByProblemCategoryId(VALID_CATEGORY_ID);
            verify(getProblemListItemResponseDtoMapper).toGetProblemListResponseDto(mockProblem);
        }

        @Test
        @DisplayName("Should return empty list when category has no problems")
        void shouldReturnEmptyList_WhenCategoryHasNoProblems() {
            // Given
            when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
                    .thenReturn(mockAuthorizationContext);
            when(problemCategoryService.validateProblemCategory(anyLong(), anyString())).thenReturn(mockProblemCategory);
            when(problemMapper.selectByProblemCategoryId(VALID_CATEGORY_ID)).thenReturn(Collections.emptyList());

            // When
            List<GetProblemListItemResponseDto> result = problemService.getProblemsByCategory(validGetProblemRequest);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(problemMapper).selectByProblemCategoryId(VALID_CATEGORY_ID);
            verifyNoInteractions(getProblemListItemResponseDtoMapper);
        }

        @Test
        @DisplayName("Should handle multiple problems correctly")
        void shouldHandleMultipleProblems_Correctly() {
            // Given
            when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
                    .thenReturn(mockAuthorizationContext);
            when(problemCategoryService.validateProblemCategory(anyLong(), anyString())).thenReturn(mockProblemCategory);

            Problem problem1 = new Problem();
            problem1.setId(1L);
            Problem problem2 = new Problem();
            problem2.setId(2L);
            
            when(problemMapper.selectByProblemCategoryId(VALID_CATEGORY_ID)).thenReturn(List.of(problem1, problem2));
            
            GetProblemListItemResponseDto responseDto1 = new GetProblemListItemResponseDto();
            GetProblemListItemResponseDto responseDto2 = new GetProblemListItemResponseDto();
            
            when(getProblemListItemResponseDtoMapper.toGetProblemListResponseDto(problem1)).thenReturn(responseDto1);
            when(getProblemListItemResponseDtoMapper.toGetProblemListResponseDto(problem2)).thenReturn(responseDto2);

            // When
            List<GetProblemListItemResponseDto> result = problemService.getProblemsByCategory(validGetProblemRequest);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertThat(result).containsExactly(responseDto1, responseDto2);

            verify(getProblemListItemResponseDtoMapper).toGetProblemListResponseDto(problem1);
            verify(getProblemListItemResponseDtoMapper).toGetProblemListResponseDto(problem2);
        }
    }

    @Nested
    @DisplayName("addProblem() Tests")
    class AddProblemTests {

        @Test
        @DisplayName("Should successfully add problem with valid data")
        void shouldSuccessfullyAddProblem_WithValidData() {
            // Given
            when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
                    .thenReturn(mockAuthorizationContext);
            when(problemCategoryService.validateProblemCategory(anyLong(), anyString())).thenReturn(mockProblemCategory);
            doNothing().when(problemChoiceService).validateProblemChoices(anyList(), anyString());

            Problem mockProblem = new Problem();
            mockProblem.setId(1L);
            // Reset serverId to null to test that it gets set properly (kills mutation on line 109)
            mockProblem.setServerId(null);
            when(createProblemRequestDtoMapper.toProblem(validCreateProblemRequest)).thenReturn(mockProblem);

            ProblemChoice mockChoice1 = new ProblemChoice();
            ProblemChoice mockChoice2 = new ProblemChoice();
            // Reset problemId to null to test that it gets set properly (kills mutation on line 122)
            mockChoice1.setProblemId(null);
            mockChoice2.setProblemId(null);
            when(createProblemChoiceRequestDtoMapper.toProblemChoice(any(CreateProblemChoiceRequestDto.class)))
                    .thenReturn(mockChoice1, mockChoice2);

            // When
            assertDoesNotThrow(() -> problemService.addProblem(validCreateProblemRequest));

            // Then
            verify(authorizationService).validateAuthorization(VALID_MEMBER_ID, VALID_CHANNEL_ID, "MANAGE_PROBLEM", CREATE_OPERATION);
            verify(problemCategoryService).validateProblemCategory(VALID_CATEGORY_ID, CREATE_OPERATION);
            verify(problemChoiceService).validateProblemChoices(validCreateProblemRequest.getChoices(), "Problem Choice Creation");
            verify(createProblemRequestDtoMapper).toProblem(validCreateProblemRequest);
            verify(problemMapper).insert(any(Problem.class));
            verify(problemChoiceMapper, times(2)).insert(any(ProblemChoice.class));
            
            // Verify that serverId was set correctly (kills mutation on line 109)
            assertEquals(VALID_SERVER_ID, mockProblem.getServerId());
            
            // Verify that problemId was set correctly for both choices (kills mutation on line 122)
            assertEquals(1L, mockChoice1.getProblemId());
            assertEquals(1L, mockChoice2.getProblemId());
        }

        @Test
        @DisplayName("Should throw ValidationException when request is null")
        void shouldThrowValidationException_WhenRequestIsNull() {
            // When & Then
            assertThatThrownBy(() -> problemService.addProblem(null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Validation failed in [Problem Creation]: Field 'body' must not be null.");

            verifyNoInteractions(authorizationService, problemCategoryService, problemChoiceService);
        }

        @Test
        @DisplayName("Should throw ValidationException when question is empty")
        void shouldThrowValidationException_WhenQuestionIsEmpty() {
            // Given
            CreateProblemRequestDto requestWithEmptyQuestion = CreateProblemRequestDto.builder()
                    .memberId(VALID_MEMBER_ID)
                    .channelId(VALID_CHANNEL_ID)
                    .problemCategoryId(VALID_CATEGORY_ID)
                    .question("")
                    .choices(validCreateProblemRequest.getChoices())
                    .build();

            when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
                    .thenReturn(mockAuthorizationContext);
            when(problemCategoryService.validateProblemCategory(anyLong(), anyString())).thenReturn(mockProblemCategory);

            // When & Then
            assertThatThrownBy(() -> problemService.addProblem(requestWithEmptyQuestion))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Validation failed in [Problem Creation]: Field 'question' must not be empty.");
        }

        @Test
        @DisplayName("Should throw ValidationException when question is null")
        void shouldThrowValidationException_WhenQuestionIsNull() {
            // Given
            CreateProblemRequestDto requestWithNullQuestion = CreateProblemRequestDto.builder()
                    .memberId(VALID_MEMBER_ID)
                    .channelId(VALID_CHANNEL_ID)
                    .problemCategoryId(VALID_CATEGORY_ID)
                    .question(null)
                    .choices(validCreateProblemRequest.getChoices())
                    .build();

            when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
                    .thenReturn(mockAuthorizationContext);
            when(problemCategoryService.validateProblemCategory(anyLong(), anyString())).thenReturn(mockProblemCategory);

            // When & Then
            assertThatThrownBy(() -> problemService.addProblem(requestWithNullQuestion))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Validation failed in [Problem Creation]: Field 'question' must not be empty.");
        }

        @Test
        @DisplayName("Should throw ValidationException when question is whitespace only")
        void shouldThrowValidationException_WhenQuestionIsWhitespaceOnly() {
            // Given
            CreateProblemRequestDto requestWithWhitespaceQuestion = CreateProblemRequestDto.builder()
                    .memberId(VALID_MEMBER_ID)
                    .channelId(VALID_CHANNEL_ID)
                    .problemCategoryId(VALID_CATEGORY_ID)
                    .question("   ")
                    .choices(validCreateProblemRequest.getChoices())
                    .build();

            when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
                    .thenReturn(mockAuthorizationContext);
            when(problemCategoryService.validateProblemCategory(anyLong(), anyString())).thenReturn(mockProblemCategory);

            // When & Then
            assertThatThrownBy(() -> problemService.addProblem(requestWithWhitespaceQuestion))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Validation failed in [Problem Creation]: Field 'question' must not be empty.");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when problem insertion fails")
        void shouldThrowIllegalStateException_WhenProblemInsertionFails() {
            // Given
            when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
                    .thenReturn(mockAuthorizationContext);
            when(problemCategoryService.validateProblemCategory(anyLong(), anyString())).thenReturn(mockProblemCategory);
            doNothing().when(problemChoiceService).validateProblemChoices(anyList(), anyString());

            Problem mockProblem = new Problem();
            // Don't set ID to simulate insertion failure
            when(createProblemRequestDtoMapper.toProblem(validCreateProblemRequest)).thenReturn(mockProblem);

            // When & Then
            assertThatThrownBy(() -> problemService.addProblem(validCreateProblemRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Problem insertion failed, no ID returned.");

            verify(problemMapper).insert(any(Problem.class));
            verifyNoInteractions(problemChoiceMapper);
        }

        @Test
        @DisplayName("Should handle null choices list")
        void shouldHandleNullChoicesList() {
            // Given
            CreateProblemRequestDto requestWithNullChoices = CreateProblemRequestDto.builder()
                    .memberId(VALID_MEMBER_ID)
                    .channelId(VALID_CHANNEL_ID)
                    .problemCategoryId(VALID_CATEGORY_ID)
                    .question(VALID_QUESTION)
                    .choices(null)
                    .build();

            when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
                    .thenReturn(mockAuthorizationContext);
            when(problemCategoryService.validateProblemCategory(anyLong(), anyString())).thenReturn(mockProblemCategory);
            doNothing().when(problemChoiceService).validateProblemChoices(isNull(), anyString());

            Problem mockProblem = new Problem();
            mockProblem.setId(1L);
            when(createProblemRequestDtoMapper.toProblem(requestWithNullChoices)).thenReturn(mockProblem);

            // When
            assertDoesNotThrow(() -> problemService.addProblem(requestWithNullChoices));

            // Then
            verify(problemMapper).insert(any(Problem.class));
            verifyNoInteractions(problemChoiceMapper);
        }

        @Test
        @DisplayName("Should handle empty choices list")
        void shouldHandleEmptyChoicesList() {
            // Given
            CreateProblemRequestDto requestWithEmptyChoices = CreateProblemRequestDto.builder()
                    .memberId(VALID_MEMBER_ID)
                    .channelId(VALID_CHANNEL_ID)
                    .problemCategoryId(VALID_CATEGORY_ID)
                    .question(VALID_QUESTION)
                    .choices(new ArrayList<>())
                    .build();

            when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
                    .thenReturn(mockAuthorizationContext);
            when(problemCategoryService.validateProblemCategory(anyLong(), anyString())).thenReturn(mockProblemCategory);
            doNothing().when(problemChoiceService).validateProblemChoices(anyList(), anyString());

            Problem mockProblem = new Problem();
            mockProblem.setId(1L);
            when(createProblemRequestDtoMapper.toProblem(requestWithEmptyChoices)).thenReturn(mockProblem);

            // When
            assertDoesNotThrow(() -> problemService.addProblem(requestWithEmptyChoices));

            // Then
            verify(problemMapper).insert(any(Problem.class));
            verifyNoInteractions(problemChoiceMapper);
        }
    }

    @Nested
    @DisplayName("validateProblem() Tests")
    class ValidateProblemTests {

        @Test
        @DisplayName("Should not throw when question is valid")
        void shouldNotThrow_WhenQuestionIsValid() {
            assertDoesNotThrow(() ->
                    problemService.validateProblem(VALID_QUESTION, CREATE_OPERATION));
        }

        @Test
        @DisplayName("Should not throw when question has leading/trailing whitespace")
        void shouldNotThrow_WhenQuestionHasLeadingTrailingWhitespace() {
            assertDoesNotThrow(() ->
                    problemService.validateProblem("  " + VALID_QUESTION + "  ", CREATE_OPERATION));
        }

        @Test
        @DisplayName("Should throw ValidationException when question is empty")
        void shouldThrowValidationException_WhenQuestionIsEmpty() {
            assertThatThrownBy(() -> problemService.validateProblem("", CREATE_OPERATION))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Validation failed in [" + CREATE_OPERATION + "]: Field 'question' must not be empty.");
        }

        @Test
        @DisplayName("Should throw ValidationException when question is whitespace only")
        void shouldThrowValidationException_WhenQuestionIsWhitespaceOnly() {
            assertThatThrownBy(() -> problemService.validateProblem("   ", CREATE_OPERATION))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Validation failed in [" + CREATE_OPERATION + "]: Field 'question' must not be empty.");
        }

        @Test
        @DisplayName("Should throw ValidationException when question is null")
        void shouldThrowValidationException_WhenQuestionIsNull() {
            assertThatThrownBy(() -> problemService.validateProblem(null, CREATE_OPERATION))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Validation failed in [" + CREATE_OPERATION + "]: Field 'question' must not be empty.");
        }

        @Test
        @DisplayName("Should work with different operation names")
        void shouldWorkWithDifferentOperationNames() {
            String customOperation = "Custom Operation";
            
            assertThatThrownBy(() -> problemService.validateProblem(null, customOperation))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Validation failed in [" + customOperation + "]: Field 'question' must not be empty.");
        }
    }

    private void setupValidCreateProblemRequest() {
        List<CreateProblemChoiceRequestDto> choices = List.of(
                CreateProblemChoiceRequestDto.builder()
                        .choiceText("Choice 1")
                        .isCorrect(true)
                        .build(),
                CreateProblemChoiceRequestDto.builder()
                        .choiceText("Choice 2")
                        .isCorrect(false)
                        .build()
        );

        validCreateProblemRequest = CreateProblemRequestDto.builder()
                .memberId(VALID_MEMBER_ID)
                .channelId(VALID_CHANNEL_ID)
                .problemCategoryId(VALID_CATEGORY_ID)
                .question(VALID_QUESTION)
                .choices(choices)
                .build();
    }

    private void setupValidGetProblemRequest() {
        validGetProblemRequest = new GetProblemRequestDto();
        validGetProblemRequest.setMemberId(VALID_MEMBER_ID);
        validGetProblemRequest.setChannelId(VALID_CHANNEL_ID);
        validGetProblemRequest.setProblemCategoryId(VALID_CATEGORY_ID);
    }

    private void setupMockAuthorizationContext() {
        Member mockMember = new Member();
        mockMember.setId(VALID_MEMBER_ID);

        Channel mockChannel = new Channel();
        mockChannel.setId(VALID_CHANNEL_ID);

        Server mockServer = new Server();
        mockServer.setId(VALID_SERVER_ID);

        mockAuthorizationContext = new AuthorizationContext(mockMember, mockChannel, mockServer, null);
    }

    private void setupMockProblemCategory() {
        mockProblemCategory = new ProblemCategory();
        mockProblemCategory.setId(VALID_CATEGORY_ID);
    }
}
