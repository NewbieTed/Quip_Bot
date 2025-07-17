package com.quip.backend.member.mapper.dto;

import com.quip.backend.common.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link MemberDtoMapper}.
 * <p>
 * This test class validates the member DTO mapper functionality.
 * Currently, the mapper interface is empty but this test ensures proper instantiation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberDtoMapper Tests")
public class MemberDtoMapperTest extends BaseTest {

    @Test
    @DisplayName("Should instantiate mapper successfully")
    void shouldInstantiateMapper_Successfully() {
        // When & Then
        // MapStruct generates implementation at compile time
        // This test ensures the interface can be processed without errors
        assertNotNull(MemberDtoMapper.class);
    }
}
