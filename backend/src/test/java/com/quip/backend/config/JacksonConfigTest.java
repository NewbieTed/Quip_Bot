package com.quip.backend.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JacksonConfig to ensure proper Java 8 date/time serialization.
 */
@ExtendWith(MockitoExtension.class)
class JacksonConfigTest {

    @Test
    void objectMapper_ShouldHaveJavaTimeModuleRegistered() {
        // Given
        JacksonConfig jacksonConfig = new JacksonConfig();

        // When
        ObjectMapper objectMapper = jacksonConfig.objectMapper();

        // Then
        // Check that JavaTimeModule is registered (the exact module ID may vary)
        assertThat(objectMapper.getRegisteredModuleIds())
                .anyMatch(id -> id.toString().contains("JavaTimeModule") || id.toString().contains("jsr310"));
    }

    @Test
    void objectMapper_ShouldDisableWriteDatesAsTimestamps() {
        // Given
        JacksonConfig jacksonConfig = new JacksonConfig();

        // When
        ObjectMapper objectMapper = jacksonConfig.objectMapper();

        // Then
        assertThat(objectMapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS))
                .isFalse();
    }

    @Test
    void objectMapper_ShouldSerializeOffsetDateTimeCorrectly() throws JsonProcessingException {
        // Given
        JacksonConfig jacksonConfig = new JacksonConfig();
        ObjectMapper objectMapper = jacksonConfig.objectMapper();
        OffsetDateTime testDateTime = OffsetDateTime.of(2024, 1, 15, 10, 30, 45, 0, ZoneOffset.UTC);

        // When
        String json = objectMapper.writeValueAsString(testDateTime);

        // Then
        assertThat(json).contains("2024-01-15T10:30:45Z");
        assertThat(json).doesNotContain("1705315845"); // Should not be a timestamp
    }

    @Test
    void objectMapper_ShouldDeserializeOffsetDateTimeCorrectly() throws JsonProcessingException {
        // Given
        JacksonConfig jacksonConfig = new JacksonConfig();
        ObjectMapper objectMapper = jacksonConfig.objectMapper();
        String json = "\"2024-01-15T10:30:45Z\"";

        // When
        OffsetDateTime result = objectMapper.readValue(json, OffsetDateTime.class);

        // Then
        assertThat(result).isEqualTo(OffsetDateTime.of(2024, 1, 15, 10, 30, 45, 0, ZoneOffset.UTC));
    }

    /**
     * Test class to verify serialization of objects containing OffsetDateTime fields.
     */
    static class TestEntity {
        public String name;
        public OffsetDateTime createdAt;

        public TestEntity() {}

        public TestEntity(String name, OffsetDateTime createdAt) {
            this.name = name;
            this.createdAt = createdAt;
        }
    }

    @Test
    void objectMapper_ShouldSerializeEntityWithOffsetDateTimeField() throws JsonProcessingException {
        // Given
        JacksonConfig jacksonConfig = new JacksonConfig();
        ObjectMapper objectMapper = jacksonConfig.objectMapper();
        OffsetDateTime testDateTime = OffsetDateTime.of(2024, 1, 15, 10, 30, 45, 0, ZoneOffset.UTC);
        TestEntity entity = new TestEntity("Test Entity", testDateTime);

        // When
        String json = objectMapper.writeValueAsString(entity);

        // Then
        assertThat(json).contains("\"name\":\"Test Entity\"");
        assertThat(json).contains("\"createdAt\":\"2024-01-15T10:30:45Z\"");
    }

    @Test
    void objectMapper_ShouldDeserializeEntityWithOffsetDateTimeField() throws JsonProcessingException {
        // Given
        JacksonConfig jacksonConfig = new JacksonConfig();
        ObjectMapper objectMapper = jacksonConfig.objectMapper();
        String json = "{\"name\":\"Test Entity\",\"createdAt\":\"2024-01-15T10:30:45Z\"}";

        // When
        TestEntity result = objectMapper.readValue(json, TestEntity.class);

        // Then
        assertThat(result.name).isEqualTo("Test Entity");
        assertThat(result.createdAt).isEqualTo(OffsetDateTime.of(2024, 1, 15, 10, 30, 45, 0, ZoneOffset.UTC));
    }
}