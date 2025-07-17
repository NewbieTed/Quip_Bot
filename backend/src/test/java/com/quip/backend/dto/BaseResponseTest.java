package com.quip.backend.dto;

import com.quip.backend.common.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BaseResponse}.
 * <p>
 * This test class validates the base response functionality including
 * constructors, factory methods, and data access.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BaseResponse Tests")
class BaseResponseTest extends BaseTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create BaseResponse with default constructor")
        void shouldCreateBaseResponse_WithDefaultConstructor() {
            // When
            BaseResponse<String> response = new BaseResponse<>();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTimestamp()).isNotNull();
            assertThat(response.getTimestamp()).isBeforeOrEqualTo(Instant.now());
        }

        @Test
        @DisplayName("Should create BaseResponse with parameterized constructor")
        void shouldCreateBaseResponse_WithParameterizedConstructor() {
            // Given
            boolean status = true;
            int statusCode = 200;
            String message = "Success";
            String data = "test data";

            // When
            BaseResponse<String> response = new BaseResponse<>(status, statusCode, message, data);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isEqualTo(status);
            assertThat(response.getStatusCode()).isEqualTo(statusCode);
            assertThat(response.getMessage()).isEqualTo(message);
            assertThat(response.getData()).isEqualTo(data);
            assertThat(response.getTimestamp()).isNotNull();
            assertThat(response.getTimestamp()).isBeforeOrEqualTo(Instant.now());
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create success response with no data")
        void shouldCreateSuccessResponse_WithNoData() {
            // When
            BaseResponse<Boolean> response = BaseResponse.success();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getMessage()).isEqualTo("");
            assertThat(response.getData()).isNull();
            assertThat(response.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should create success response with data")
        void shouldCreateSuccessResponse_WithData() {
            // Given
            String testData = "test data";

            // When
            BaseResponse<String> response = BaseResponse.success(testData);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getMessage()).isEqualTo("");
            assertThat(response.getData()).isEqualTo(testData);
            assertThat(response.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should create success response with message and data")
        void shouldCreateSuccessResponse_WithMessageAndData() {
            // Given
            String message = "Operation successful";
            String testData = "test data";

            // When
            BaseResponse<String> response = BaseResponse.success(message, testData);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isTrue();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getMessage()).isEqualTo(message);
            assertThat(response.getData()).isEqualTo(testData);
            assertThat(response.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should create failure response with status code and message")
        void shouldCreateFailureResponse_WithStatusCodeAndMessage() {
            // Given
            int statusCode = HttpStatus.BAD_REQUEST.value();
            String message = "Bad request";

            // When
            BaseResponse<String> response = BaseResponse.failure(statusCode, message);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.isStatus()).isFalse();
            assertThat(response.getStatusCode()).isEqualTo(statusCode);
            assertThat(response.getMessage()).isEqualTo(message);
            assertThat(response.getData()).isNull();
            assertThat(response.getTimestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Data Access Tests")
    class DataAccessTests {

        @Test
        @DisplayName("Should set and get all properties correctly")
        void shouldSetAndGetAllProperties_Correctly() {
            // Given
            BaseResponse<String> response = new BaseResponse<>();
            boolean status = false;
            int statusCode = 404;
            String message = "Not found";
            String data = "error data";
            Instant timestamp = Instant.now();

            // When
            response.setStatus(status);
            response.setStatusCode(statusCode);
            response.setMessage(message);
            response.setData(data);
            response.setTimestamp(timestamp);

            // Then
            assertThat(response.isStatus()).isEqualTo(status);
            assertThat(response.getStatusCode()).isEqualTo(statusCode);
            assertThat(response.getMessage()).isEqualTo(message);
            assertThat(response.getData()).isEqualTo(data);
            assertThat(response.getTimestamp()).isEqualTo(timestamp);
        }

        @Test
        @DisplayName("Should handle null values correctly")
        void shouldHandleNullValues_Correctly() {
            // Given
            BaseResponse<String> response = new BaseResponse<>();

            // When
            response.setMessage(null);
            response.setData(null);
            response.setTimestamp(null);

            // Then
            assertThat(response.getMessage()).isNull();
            assertThat(response.getData()).isNull();
            assertThat(response.getTimestamp()).isNull();
        }
    }

    @Nested
    @DisplayName("Generic Type Tests")
    class GenericTypeTests {

        @Test
        @DisplayName("Should work with different generic types")
        void shouldWorkWithDifferentGenericTypes() {
            // Given & When
            BaseResponse<Integer> intResponse = BaseResponse.success(42);
            BaseResponse<Boolean> boolResponse = BaseResponse.success(true);
            BaseResponse<Object> objectResponse = BaseResponse.success(new Object());

            // Then
            assertThat(intResponse.getData()).isEqualTo(42);
            assertThat(boolResponse.getData()).isTrue();
            assertThat(objectResponse.getData()).isNotNull();
        }

        @Test
        @DisplayName("Should handle complex objects as data")
        void shouldHandleComplexObjects_AsData() {
            // Given
            TestDataObject testObject = new TestDataObject("test", 123);

            // When
            BaseResponse<TestDataObject> response = BaseResponse.success("Success", testObject);

            // Then
            assertThat(response.getData()).isEqualTo(testObject);
            assertThat(response.getData().getName()).isEqualTo("test");
            assertThat(response.getData().getValue()).isEqualTo(123);
        }
    }

    // Helper class for testing complex objects
    private static class TestDataObject {
        private final String name;
        private final int value;

        public TestDataObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestDataObject that = (TestDataObject) obj;
            return value == that.value && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode() + value;
        }
    }
}
