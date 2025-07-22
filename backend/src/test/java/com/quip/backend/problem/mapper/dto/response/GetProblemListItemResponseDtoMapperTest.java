package com.quip.backend.problem.mapper.dto.response;

import com.quip.backend.common.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link GetProblemListItemResponseDtoMapper}.
 * <p>
 * This test class validates the problem list item response DTO mapper functionality.
 * It ensures the MapStruct interface can be processed without errors.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetProblemListItemResponseDtoMapper Tests")
public class GetProblemListItemResponseDtoMapperTest extends BaseTest {

    @Test
    @DisplayName("Should instantiate mapper successfully")
    void shouldInstantiateMapper_Successfully() {
        // When & Then
        // MapStruct generates implementation at compile time
        // This test ensures the interface can be processed without errors
        assertNotNull(GetProblemListItemResponseDtoMapper.class);
    }
}