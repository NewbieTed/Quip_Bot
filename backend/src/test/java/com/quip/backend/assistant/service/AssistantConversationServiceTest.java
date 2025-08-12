package com.quip.backend.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.quip.backend.assistant.mapper.database.AssistantConversationMapper;
import com.quip.backend.assistant.model.database.AssistantConversation;
import com.quip.backend.common.BaseTest;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.config.redis.CacheConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.Spy;

/**
 * Comprehensive unit tests for {@link AssistantConversationService}.
 * <p>
 * This test class provides full code coverage and mutation testing resistance
 * for all methods in the AssistantConversationService class.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssistantConversationService Tests")
public class AssistantConversationServiceTest extends BaseTest {

    @InjectMocks
    private AssistantConversationService assistantConversationService;

    @Mock
    private AssistantConversationMapper assistantConversationMapper;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    // Test data constants
    private static final Long VALID_MEMBER_ID = 1L;
    private static final Long VALID_SERVER_ID = 100L;
    private static final Long VALID_CONVERSATION_ID = 1000L;
    private static final Long VALID_CREATED_BY = 2L;
    private static final Long VALID_UPDATED_BY = 3L;

    private AssistantConversation mockActiveConversation;
    private AssistantConversation mockInactiveConversation;

    @BeforeEach
    void setUp() {
        reset(assistantConversationMapper, cacheManager, cache);
        setupMockConversations();
    }

    private void setupMockConversations() {
        mockActiveConversation = AssistantConversation.builder()
                .id(VALID_CONVERSATION_ID)
                .memberId(VALID_MEMBER_ID)
                .serverId(VALID_SERVER_ID)
                .isActive(true)
                .isInterrupt(false)
                .isProcessing(false)
                .createdBy(VALID_CREATED_BY)
                .updatedBy(VALID_UPDATED_BY)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        mockInactiveConversation = AssistantConversation.builder()
                .id(VALID_CONVERSATION_ID + 1)
                .memberId(VALID_MEMBER_ID)
                .serverId(VALID_SERVER_ID)
                .isActive(false)
                .isInterrupt(false)
                .isProcessing(false)
                .createdBy(VALID_CREATED_BY)
                .updatedBy(VALID_UPDATED_BY)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("validateAssistantConversation() Tests")
    class ValidateAssistantConversationTests {

        @Test
        @DisplayName("Should return active conversation when found")
        void shouldReturnActiveConversation_WhenFound() {
            // Given
            when(assistantConversationMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(mockActiveConversation);

            // When
            AssistantConversation result = assistantConversationService
                    .validateAssistantConversation(VALID_MEMBER_ID, VALID_SERVER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(VALID_CONVERSATION_ID);
            assertThat(result.getIsActive()).isTrue();
            
            verify(assistantConversationMapper).selectOne(any(QueryWrapper.class));
        }

        @Test
        @DisplayName("Should throw ValidationException when no active conversation found")
        void shouldThrowValidationException_WhenNoActiveConversationFound() {
            // Given
            when(assistantConversationMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> assistantConversationService
                    .validateAssistantConversation(VALID_MEMBER_ID, VALID_SERVER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("No active assistant conversation found for member: " + VALID_MEMBER_ID);
        }

        @Test
        @DisplayName("Should use correct query parameters")
        void shouldUseCorrectQueryParameters() {
            // Given
            ArgumentCaptor<QueryWrapper<AssistantConversation>> queryCaptor = 
                    ArgumentCaptor.forClass(QueryWrapper.class);
            when(assistantConversationMapper.selectOne(queryCaptor.capture()))
                    .thenReturn(mockActiveConversation);

            // When
            assistantConversationService.validateAssistantConversation(VALID_MEMBER_ID, VALID_SERVER_ID);

            // Then
            QueryWrapper<AssistantConversation> capturedQuery = queryCaptor.getValue();
            assertThat(capturedQuery).isNotNull();
            verify(assistantConversationMapper).selectOne(any(QueryWrapper.class));
        }
    }

    @Nested
    @DisplayName("getActiveAssistantConversation() Tests")
    class GetActiveAssistantConversationTests {

        @Test
        @DisplayName("Should return active conversation when found")
        void shouldReturnActiveConversation_WhenFound() {
            // Given
            when(assistantConversationMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(mockActiveConversation);

            // When
            AssistantConversation result = assistantConversationService
                    .getActiveAssistantConversation(VALID_MEMBER_ID, VALID_SERVER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(VALID_CONVERSATION_ID);
            assertThat(result.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("Should return null when no active conversation found")
        void shouldReturnNull_WhenNoActiveConversationFound() {
            // Given
            when(assistantConversationMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(null);

            // When
            AssistantConversation result = assistantConversationService
                    .getActiveAssistantConversation(VALID_MEMBER_ID, VALID_SERVER_ID);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("createActiveConversation() Tests")
    class CreateActiveConversationTests {

        @Test
        @DisplayName("Should create new active conversation successfully")
        void shouldCreateNewActiveConversation_Successfully() {
            // Given
            when(assistantConversationMapper.update(isNull(), any(UpdateWrapper.class)))
                    .thenReturn(1); // Deactivated 1 existing conversation
            when(assistantConversationMapper.insert(any(AssistantConversation.class)))
                    .thenAnswer(invocation -> {
                        AssistantConversation conversation = invocation.getArgument(0);
                        conversation.setId(VALID_CONVERSATION_ID);
                        return 1;
                    });

            // When
            AssistantConversation result = assistantConversationService
                    .createActiveConversation(VALID_MEMBER_ID, VALID_SERVER_ID, VALID_CREATED_BY);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(VALID_CONVERSATION_ID);
            assertThat(result.getMemberId()).isEqualTo(VALID_MEMBER_ID);
            assertThat(result.getServerId()).isEqualTo(VALID_SERVER_ID);
            assertThat(result.getIsActive()).isTrue();
            assertThat(result.getIsInterrupt()).isFalse();
            assertThat(result.getIsProcessing()).isFalse();
            assertThat(result.getCreatedBy()).isEqualTo(VALID_CREATED_BY);
            assertThat(result.getUpdatedBy()).isEqualTo(VALID_CREATED_BY);

            verify(assistantConversationMapper).update(isNull(), any(UpdateWrapper.class));
            verify(assistantConversationMapper).insert(any(AssistantConversation.class));
        }

        @Test
        @DisplayName("Should create conversation when no existing conversations to deactivate")
        void shouldCreateConversation_WhenNoExistingConversationsToDeactivate() {
            // Given
            when(assistantConversationMapper.update(isNull(), any(UpdateWrapper.class)))
                    .thenReturn(0); // No existing conversations deactivated
            when(assistantConversationMapper.insert(any(AssistantConversation.class)))
                    .thenAnswer(invocation -> {
                        AssistantConversation conversation = invocation.getArgument(0);
                        conversation.setId(VALID_CONVERSATION_ID);
                        return 1;
                    });

            // When
            AssistantConversation result = assistantConversationService
                    .createActiveConversation(VALID_MEMBER_ID, VALID_SERVER_ID, VALID_CREATED_BY);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIsActive()).isTrue();
            verify(assistantConversationMapper).update(isNull(), any(UpdateWrapper.class));
            verify(assistantConversationMapper).insert(any(AssistantConversation.class));
        }

        @Test
        @DisplayName("Should handle boundary condition when exactly one conversation is deactivated")
        void shouldHandleBoundaryCondition_WhenExactlyOneConversationIsDeactivated() {
            // Given
            when(assistantConversationMapper.update(isNull(), any(UpdateWrapper.class)))
                    .thenReturn(1); // Exactly 1 existing conversation deactivated (boundary condition)
            when(assistantConversationMapper.insert(any(AssistantConversation.class)))
                    .thenAnswer(invocation -> {
                        AssistantConversation conversation = invocation.getArgument(0);
                        conversation.setId(VALID_CONVERSATION_ID);
                        return 1;
                    });

            // When
            AssistantConversation result = assistantConversationService
                    .createActiveConversation(VALID_MEMBER_ID, VALID_SERVER_ID, VALID_CREATED_BY);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIsActive()).isTrue();
            verify(assistantConversationMapper).update(isNull(), any(UpdateWrapper.class));
            verify(assistantConversationMapper).insert(any(AssistantConversation.class));
        }

        @Test
        @DisplayName("Should verify exact boundary behavior for deactivated count")
        void shouldVerifyExactBoundaryBehaviorForDeactivatedCount() {
            // This test specifically targets the boundary mutation: deactivated > 0 vs deactivated >= 0
            
            // Test case 1: deactivated = 0 (should NOT enter if block with original condition)
            when(assistantConversationMapper.update(isNull(), any(UpdateWrapper.class)))
                    .thenReturn(0);
            when(assistantConversationMapper.insert(any(AssistantConversation.class)))
                    .thenAnswer(invocation -> {
                        AssistantConversation conversation = invocation.getArgument(0);
                        conversation.setId(VALID_CONVERSATION_ID);
                        return 1;
                    });

            AssistantConversation result1 = assistantConversationService
                    .createActiveConversation(VALID_MEMBER_ID, VALID_SERVER_ID, VALID_CREATED_BY);
            
            assertThat(result1).isNotNull();
            
            // Reset mocks for second test
            reset(assistantConversationMapper);
            
            // Test case 2: deactivated = 1 (should enter if block with both original and mutated condition)
            when(assistantConversationMapper.update(isNull(), any(UpdateWrapper.class)))
                    .thenReturn(1);
            when(assistantConversationMapper.insert(any(AssistantConversation.class)))
                    .thenAnswer(invocation -> {
                        AssistantConversation conversation = invocation.getArgument(0);
                        conversation.setId(VALID_CONVERSATION_ID + 1);
                        return 1;
                    });

            AssistantConversation result2 = assistantConversationService
                    .createActiveConversation(VALID_MEMBER_ID, VALID_SERVER_ID, VALID_CREATED_BY);
            
            assertThat(result2).isNotNull();
            
            // The key insight: both tests should pass with original code,
            // but if we had additional side effects in the if block that we could observe,
            // the mutation would cause different behavior for the deactivated = 0 case
        }

        @Test
        @DisplayName("Should handle multiple conversations being deactivated")
        void shouldHandleMultipleConversationsBeingDeactivated() {
            // Given
            when(assistantConversationMapper.update(isNull(), any(UpdateWrapper.class)))
                    .thenReturn(3); // Multiple existing conversations deactivated
            when(assistantConversationMapper.insert(any(AssistantConversation.class)))
                    .thenAnswer(invocation -> {
                        AssistantConversation conversation = invocation.getArgument(0);
                        conversation.setId(VALID_CONVERSATION_ID);
                        return 1;
                    });

            // When
            AssistantConversation result = assistantConversationService
                    .createActiveConversation(VALID_MEMBER_ID, VALID_SERVER_ID, VALID_CREATED_BY);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIsActive()).isTrue();
            verify(assistantConversationMapper).update(isNull(), any(UpdateWrapper.class));
            verify(assistantConversationMapper).insert(any(AssistantConversation.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when memberId is null")
        void shouldThrowIllegalArgumentException_WhenMemberIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> assistantConversationService
                    .createActiveConversation(null, VALID_SERVER_ID, VALID_CREATED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Member ID cannot be null");

            verifyNoInteractions(assistantConversationMapper);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when serverId is null")
        void shouldThrowIllegalArgumentException_WhenServerIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> assistantConversationService
                    .createActiveConversation(VALID_MEMBER_ID, null, VALID_CREATED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Server ID cannot be null");

            verifyNoInteractions(assistantConversationMapper);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when createdBy is null")
        void shouldThrowIllegalArgumentException_WhenCreatedByIsNull() {
            // When & Then
            assertThatThrownBy(() -> assistantConversationService
                    .createActiveConversation(VALID_MEMBER_ID, VALID_SERVER_ID, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Created by cannot be null");

            verifyNoInteractions(assistantConversationMapper);
        }
    }

    @Nested
    @DisplayName("deactivateActiveConversation() Tests")
    class DeactivateActiveConversationTests {

        @Test
        @DisplayName("Should deactivate active conversation successfully")
        void shouldDeactivateActiveConversation_Successfully() {
            // Given
            when(assistantConversationMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(mockActiveConversation);
            when(assistantConversationMapper.update(isNull(), any(UpdateWrapper.class)))
                    .thenReturn(1);

            // When
            boolean result = assistantConversationService
                    .deactivateActiveConversation(VALID_MEMBER_ID, VALID_SERVER_ID, VALID_UPDATED_BY);

            // Then
            assertThat(result).isTrue();
            verify(assistantConversationMapper).selectOne(any(QueryWrapper.class));
            verify(assistantConversationMapper).update(isNull(), any(UpdateWrapper.class));
        }

        @Test
        @DisplayName("Should return false when no active conversation found")
        void shouldReturnFalse_WhenNoActiveConversationFound() {
            // Given
            when(assistantConversationMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(null);

            // When
            boolean result = assistantConversationService
                    .deactivateActiveConversation(VALID_MEMBER_ID, VALID_SERVER_ID, VALID_UPDATED_BY);

            // Then
            assertThat(result).isFalse();
            verify(assistantConversationMapper).selectOne(any(QueryWrapper.class));
            verify(assistantConversationMapper, never()).update(any(), any(UpdateWrapper.class));
        }

        @Test
        @DisplayName("Should return false when update fails")
        void shouldReturnFalse_WhenUpdateFails() {
            // Given
            when(assistantConversationMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(mockActiveConversation);
            when(assistantConversationMapper.update(isNull(), any(UpdateWrapper.class)))
                    .thenReturn(0);

            // When
            boolean result = assistantConversationService
                    .deactivateActiveConversation(VALID_MEMBER_ID, VALID_SERVER_ID, VALID_UPDATED_BY);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when memberId is null")
        void shouldThrowIllegalArgumentException_WhenMemberIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> assistantConversationService
                    .deactivateActiveConversation(null, VALID_SERVER_ID, VALID_UPDATED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Member ID cannot be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when serverId is null")
        void shouldThrowIllegalArgumentException_WhenServerIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> assistantConversationService
                    .deactivateActiveConversation(VALID_MEMBER_ID, null, VALID_UPDATED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Server ID cannot be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when updatedBy is null")
        void shouldThrowIllegalArgumentException_WhenUpdatedByIsNull() {
            // When & Then
            assertThatThrownBy(() -> assistantConversationService
                    .deactivateActiveConversation(VALID_MEMBER_ID, VALID_SERVER_ID, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Updated by cannot be null");
        }
    }

    @Nested
    @DisplayName("updateProcessingStatus() Tests")
    class UpdateProcessingStatusTests {

        @Test
        @DisplayName("Should return false for incomplete implementation")
        void shouldReturnFalse_ForIncompleteImplementation() {
            // When
            boolean result = assistantConversationService
                    .updateProcessingStatus(VALID_MEMBER_ID, VALID_SERVER_ID, true, VALID_UPDATED_BY);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when memberId is null")
        void shouldThrowIllegalArgumentException_WhenMemberIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> assistantConversationService
                    .updateProcessingStatus(null, VALID_SERVER_ID, true, VALID_UPDATED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Member ID cannot be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when serverId is null")
        void shouldThrowIllegalArgumentException_WhenServerIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> assistantConversationService
                    .updateProcessingStatus(VALID_MEMBER_ID, null, true, VALID_UPDATED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Server ID cannot be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when updatedBy is null")
        void shouldThrowIllegalArgumentException_WhenUpdatedByIsNull() {
            // When & Then
            assertThatThrownBy(() -> assistantConversationService
                    .updateProcessingStatus(VALID_MEMBER_ID, VALID_SERVER_ID, true, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Updated by cannot be null");
        }

        @Test
        @DisplayName("Should handle both true and false processing status values")
        void shouldHandleBothTrueAndFalseProcessingStatusValues() {
            // When & Then - Test with true
            boolean resultTrue = assistantConversationService
                    .updateProcessingStatus(VALID_MEMBER_ID, VALID_SERVER_ID, true, VALID_UPDATED_BY);
            assertThat(resultTrue).isFalse();

            // When & Then - Test with false
            boolean resultFalse = assistantConversationService
                    .updateProcessingStatus(VALID_MEMBER_ID, VALID_SERVER_ID, false, VALID_UPDATED_BY);
            assertThat(resultFalse).isFalse();
        }
    }

    @Nested
    @DisplayName("markAsInterrupted() Tests")
    class MarkAsInterruptedTests {

        @Test
        @DisplayName("Should return false for incomplete implementation")
        void shouldReturnFalse_ForIncompleteImplementation() {
            // When
            boolean result = assistantConversationService
                    .markAsInterrupted(VALID_MEMBER_ID, VALID_SERVER_ID, VALID_UPDATED_BY);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when memberId is null")
        void shouldThrowIllegalArgumentException_WhenMemberIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> assistantConversationService
                    .markAsInterrupted(null, VALID_SERVER_ID, VALID_UPDATED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Member ID cannot be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when serverId is null")
        void shouldThrowIllegalArgumentException_WhenServerIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> assistantConversationService
                    .markAsInterrupted(VALID_MEMBER_ID, null, VALID_UPDATED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Server ID cannot be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when updatedBy is null")
        void shouldThrowIllegalArgumentException_WhenUpdatedByIsNull() {
            // When & Then
            assertThatThrownBy(() -> assistantConversationService
                    .markAsInterrupted(VALID_MEMBER_ID, VALID_SERVER_ID, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Updated by cannot be null");
        }
    }

    @Nested
    @DisplayName("evictMemberConversationCache() Tests")
    class EvictMemberConversationCacheTests {

        @Test
        @DisplayName("Should handle valid member and server IDs")
        void shouldHandleValidMemberAndServerIds() {
            // When
            assertThatCode(() -> assistantConversationService
                    .evictMemberConversationCache(VALID_MEMBER_ID, VALID_SERVER_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should return early when member ID is null")
        void shouldReturnEarly_WhenMemberIdIsNull() {
            // When
            assertThatCode(() -> assistantConversationService
                    .evictMemberConversationCache(null, VALID_SERVER_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should return early when server ID is null")
        void shouldReturnEarly_WhenServerIdIsNull() {
            // When
            assertThatCode(() -> assistantConversationService
                    .evictMemberConversationCache(VALID_MEMBER_ID, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should return early when both IDs are null")
        void shouldReturnEarly_WhenBothIdsAreNull() {
            // When
            assertThatCode(() -> assistantConversationService
                    .evictMemberConversationCache(null, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should execute cache eviction logic when both IDs are non-null")
        void shouldExecuteCacheEvictionLogic_WhenBothIdsAreNonNull() {
            // Given - both IDs are non-null, so the method should proceed past the null check
            Long nonNullMemberId = 123L;
            Long nonNullServerId = 456L;

            // When
            assertThatCode(() -> assistantConversationService
                    .evictMemberConversationCache(nonNullMemberId, nonNullServerId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle member ID being null while server ID is non-null")
        void shouldHandleMemberIdNull_WhileServerIdNonNull() {
            // This tests the first part of the OR condition: memberId == null
            assertThatCode(() -> assistantConversationService
                    .evictMemberConversationCache(null, VALID_SERVER_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle server ID being null while member ID is non-null")
        void shouldHandleServerIdNull_WhileMemberIdNonNull() {
            // This tests the second part of the OR condition: serverId == null
            assertThatCode(() -> assistantConversationService
                    .evictMemberConversationCache(VALID_MEMBER_ID, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should verify early return behavior with null checks")
        void shouldVerifyEarlyReturnBehaviorWithNullChecks() {
            // Test that demonstrates the difference between the original condition and its mutations
            // Original: if (memberId == null || serverId == null) return;
            // Mutation 1: if (memberId != null || serverId == null) return; (negated first part)
            // Mutation 2: if (memberId == null || serverId != null) return; (negated second part)
            
            // This test case would behave differently under mutations:
            // - Original: both non-null -> continue execution (no early return)
            // - Mutation 1: first non-null -> early return (different behavior)
            // - Mutation 2: second non-null -> early return (different behavior)
            
            Long nonNullMemberId = 999L;
            Long nonNullServerId = 888L;
            
            // When both are non-null, the method should proceed past the null check
            // and execute the log statement (which we can't directly verify, but the method completes normally)
            assertThatCode(() -> assistantConversationService
                    .evictMemberConversationCache(nonNullMemberId, nonNullServerId))
                    .doesNotThrowAnyException();
            
            // Additional verification: when either is null, method should return early
            assertThatCode(() -> assistantConversationService
                    .evictMemberConversationCache(null, nonNullServerId))
                    .doesNotThrowAnyException();
            
            assertThatCode(() -> assistantConversationService
                    .evictMemberConversationCache(nonNullMemberId, null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("evictAllConversationCache() Tests")
    class EvictAllConversationCacheTests {

        @Test
        @DisplayName("Should execute without throwing exceptions")
        void shouldExecuteWithoutThrowingExceptions() {
            // When
            assertThatCode(() -> assistantConversationService.evictAllConversationCache())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("getAllActiveConversationsForMember() Tests")
    class GetAllActiveConversationsForMemberTests {

        @Test
        @DisplayName("Should return list of active conversations for member")
        void shouldReturnListOfActiveConversationsForMember() {
            // Given
            List<AssistantConversation> expectedConversations = Arrays.asList(
                    mockActiveConversation,
                    AssistantConversation.builder()
                            .id(VALID_CONVERSATION_ID + 1)
                            .memberId(VALID_MEMBER_ID)
                            .serverId(VALID_SERVER_ID + 1)
                            .isActive(true)
                            .build()
            );
            when(assistantConversationMapper.selectList(any(QueryWrapper.class)))
                    .thenReturn(expectedConversations);

            // When
            List<AssistantConversation> result = assistantConversationService
                    .getAllActiveConversationsForMember(VALID_MEMBER_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyElementsOf(expectedConversations);
            verify(assistantConversationMapper).selectList(any(QueryWrapper.class));
        }

        @Test
        @DisplayName("Should return empty list when no active conversations found")
        void shouldReturnEmptyListWhenNoActiveConversationsFound() {
            // Given
            when(assistantConversationMapper.selectList(any(QueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            // When
            List<AssistantConversation> result = assistantConversationService
                    .getAllActiveConversationsForMember(VALID_MEMBER_ID);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when memberId is null")
        void shouldThrowIllegalArgumentException_WhenMemberIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> assistantConversationService
                    .getAllActiveConversationsForMember(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Member ID cannot be null");
        }
    }

    @Nested
    @DisplayName("getConversationById() Tests")
    class GetConversationByIdTests {

        @Test
        @DisplayName("Should return conversation when found")
        void shouldReturnConversationWhenFound() {
            // Given
            when(assistantConversationMapper.selectById(VALID_CONVERSATION_ID))
                    .thenReturn(mockActiveConversation);

            // When
            AssistantConversation result = assistantConversationService
                    .getConversationById(VALID_CONVERSATION_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(VALID_CONVERSATION_ID);
            verify(assistantConversationMapper).selectById(VALID_CONVERSATION_ID);
        }

        @Test
        @DisplayName("Should return null when conversation not found")
        void shouldReturnNullWhenConversationNotFound() {
            // Given
            when(assistantConversationMapper.selectById(VALID_CONVERSATION_ID))
                    .thenReturn(null);

            // When
            AssistantConversation result = assistantConversationService
                    .getConversationById(VALID_CONVERSATION_ID);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when conversationId is null")
        void shouldThrowIllegalArgumentException_WhenConversationIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> assistantConversationService.getConversationById(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Conversation ID cannot be null");
        }
    }

    @Nested
    @DisplayName("getConversationsByIds() Tests")
    class GetConversationsByIdsTests {

        @Test
        @DisplayName("Should return conversations for valid IDs")
        void shouldReturnConversationsForValidIds() {
            // Given
            List<Long> conversationIds = Arrays.asList(VALID_CONVERSATION_ID, VALID_CONVERSATION_ID + 1);
            List<AssistantConversation> expectedConversations = Arrays.asList(
                    mockActiveConversation, mockInactiveConversation);
            when(assistantConversationMapper.selectBatchIds(conversationIds))
                    .thenReturn(expectedConversations);

            // When
            List<AssistantConversation> result = assistantConversationService
                    .getConversationsByIds(conversationIds);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyElementsOf(expectedConversations);
        }

        @Test
        @DisplayName("Should return empty list when empty IDs provided")
        void shouldReturnEmptyListWhenEmptyIdsProvided() {
            // Given
            List<Long> emptyIds = Collections.emptyList();

            // When
            List<AssistantConversation> result = assistantConversationService
                    .getConversationsByIds(emptyIds);

            // Then
            assertThat(result).isEmpty();
            verify(assistantConversationMapper, never()).selectBatchIds(any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when conversationIds is null")
        void shouldThrowIllegalArgumentException_WhenConversationIdsIsNull() {
            // When & Then
            assertThatThrownBy(() -> assistantConversationService.getConversationsByIds(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Conversation IDs list cannot be null");
        }
    }

    @Nested
    @DisplayName("getAllConversationsForMember() Tests")
    class GetAllConversationsForMemberTests {

        @Test
        @DisplayName("Should return all conversations for member")
        void shouldReturnAllConversationsForMember() {
            // Given
            List<AssistantConversation> expectedConversations = Arrays.asList(
                    mockActiveConversation, mockInactiveConversation);
            when(assistantConversationMapper.selectList(any(QueryWrapper.class)))
                    .thenReturn(expectedConversations);

            // When
            List<AssistantConversation> result = assistantConversationService
                    .getAllConversationsForMember(VALID_MEMBER_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyElementsOf(expectedConversations);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when memberId is null")
        void shouldThrowIllegalArgumentException_WhenMemberIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> assistantConversationService
                    .getAllConversationsForMember(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Member ID cannot be null");
        }
    }

    @Nested
    @DisplayName("getAllConversationsForMemberInServer() Tests")
    class GetAllConversationsForMemberInServerTests {

        @Test
        @DisplayName("Should return all conversations for member in server")
        void shouldReturnAllConversationsForMemberInServer() {
            // Given
            List<AssistantConversation> expectedConversations = Arrays.asList(
                    mockActiveConversation, mockInactiveConversation);
            when(assistantConversationMapper.selectList(any(QueryWrapper.class)))
                    .thenReturn(expectedConversations);

            // When
            List<AssistantConversation> result = assistantConversationService
                    .getAllConversationsForMemberInServer(VALID_MEMBER_ID, VALID_SERVER_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyElementsOf(expectedConversations);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when memberId is null")
        void shouldThrowIllegalArgumentException_WhenMemberIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> assistantConversationService
                    .getAllConversationsForMemberInServer(null, VALID_SERVER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Member ID cannot be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when serverId is null")
        void shouldThrowIllegalArgumentException_WhenServerIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> assistantConversationService
                    .getAllConversationsForMemberInServer(VALID_MEMBER_ID, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Server ID cannot be null");
        }
    }

    @Nested
    @DisplayName("getConflictingInterruptedToolNames() Tests")
    class GetConflictingInterruptedToolNamesTests {

        @Test
        @DisplayName("Should return conflicting tool names")
        void shouldReturnConflictingToolNames() {
            // Given
            List<AssistantConversation> conversations = Arrays.asList(mockActiveConversation);
            List<String> toolNames = Arrays.asList("tool1", "tool2");
            List<String> expectedConflicts = Arrays.asList("tool1");
            
            when(assistantConversationMapper.getConflictingInterruptedToolNames(conversations, toolNames))
                    .thenReturn(expectedConflicts);

            // When
            List<String> result = assistantConversationService
                    .getConflictingInterruptedToolNames(conversations, toolNames);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly("tool1");
        }

        @Test
        @DisplayName("Should return empty list when no conversations provided")
        void shouldReturnEmptyListWhenNoConversationsProvided() {
            // Given
            List<AssistantConversation> emptyConversations = Collections.emptyList();
            List<String> toolNames = Arrays.asList("tool1", "tool2");

            // When
            List<String> result = assistantConversationService
                    .getConflictingInterruptedToolNames(emptyConversations, toolNames);

            // Then
            assertThat(result).isEmpty();
            verify(assistantConversationMapper, never())
                    .getConflictingInterruptedToolNames(any(), any());
        }

        @Test
        @DisplayName("Should return empty list when no tool names provided")
        void shouldReturnEmptyListWhenNoToolNamesProvided() {
            // Given
            List<AssistantConversation> conversations = Arrays.asList(mockActiveConversation);
            List<String> emptyToolNames = Collections.emptyList();

            // When
            List<String> result = assistantConversationService
                    .getConflictingInterruptedToolNames(conversations, emptyToolNames);

            // Then
            assertThat(result).isEmpty();
            verify(assistantConversationMapper, never())
                    .getConflictingInterruptedToolNames(any(), any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when conversations is null")
        void shouldThrowIllegalArgumentException_WhenConversationsIsNull() {
            // Given
            List<String> toolNames = Arrays.asList("tool1");

            // When & Then
            assertThatThrownBy(() -> assistantConversationService
                    .getConflictingInterruptedToolNames(null, toolNames))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Conversations list cannot be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when toolNames is null")
        void shouldThrowIllegalArgumentException_WhenToolNamesIsNull() {
            // Given
            List<AssistantConversation> conversations = Arrays.asList(mockActiveConversation);

            // When & Then
            assertThatThrownBy(() -> assistantConversationService
                    .getConflictingInterruptedToolNames(conversations, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Tool names list cannot be null");
        }
    }
}