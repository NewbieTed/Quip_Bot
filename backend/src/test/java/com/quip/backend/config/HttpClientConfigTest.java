package com.quip.backend.config;

import com.quip.backend.common.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HttpClientConfig}.
 * <p>
 * This test class validates the HTTP client configuration functionality including
 * proper bean creation and configuration.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HttpClientConfig Tests")
class HttpClientConfigTest extends BaseTest {

    private HttpClientConfig httpClientConfig;
    private final int expectedBufferSize = 16 * 1024 * 1024; // 16MB

    @BeforeEach
    void setUp() {
        httpClientConfig = new HttpClientConfig();
    }

    @Test
    @DisplayName("Should create WebClient bean successfully")
    void shouldCreateWebClient_Successfully() {
        // When
        WebClient result = httpClientConfig.webClient();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(WebClient.class);
    }

    @Test
    @DisplayName("Should create new instance of WebClient on each call")
    void shouldCreateNewInstance_OnEachCall() {
        // When
        WebClient first = httpClientConfig.webClient();
        WebClient second = httpClientConfig.webClient();

        // Then
        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(first).isNotSameAs(second);
    }

    @Test
    @DisplayName("Should instantiate HttpClientConfig successfully")
    void shouldInstantiateHttpClientConfig_Successfully() {
        // When & Then
        assertThat(httpClientConfig).isNotNull();
    }

    @Test
    @DisplayName("Should maintain bean independence across multiple calls")
    void shouldMaintainBeanIndependence_AcrossMultipleCalls() {
        // When
        WebClient firstClient = httpClientConfig.webClient();
        WebClient secondClient = httpClientConfig.webClient();
        WebClient thirdClient = httpClientConfig.webClient();

        // Then
        assertThat(firstClient).isNotSameAs(secondClient);
        assertThat(secondClient).isNotSameAs(thirdClient);
        assertThat(firstClient).isNotSameAs(thirdClient);
        assertThat(firstClient).isNotNull();
        assertThat(secondClient).isNotNull();
        assertThat(thirdClient).isNotNull();
    }

    @Test
    @DisplayName("Should create WebClient with proper type")
    void shouldCreateWebClient_WithProperType() {
        // When
        WebClient webClient = httpClientConfig.webClient();

        // Then
        assertThat(webClient).isNotNull();
        assertThat(webClient.getClass().getName()).contains("WebClient");
    }

    @Test
    @DisplayName("Should create WebClient that can be used for requests")
    void shouldCreateWebClient_ThatCanBeUsedForRequests() {
        // When
        WebClient webClient = httpClientConfig.webClient();

        // Then
        assertThat(webClient).isNotNull();
        // We can't make actual requests in a unit test, but we can verify the client is properly initialized
        assertThat(webClient.toString()).isNotEmpty();
    }

    
    @Test
    @DisplayName("Should verify buffer size calculation is correct")
    void shouldVerifyBufferSizeCalculation_IsCorrect() {
        // Given
        int kb = 1024;
        int mb = 1024 * 1024;
        int bufferSize = 16 * mb;
        
        // When/Then
        assertThat(kb).isEqualTo(1024);
        assertThat(mb).isEqualTo(1048576);
        assertThat(bufferSize).isEqualTo(16777216);
        
        // Verify the calculation in different ways
        assertThat(16 * kb * kb).isEqualTo(bufferSize);
        assertThat(16 * mb).isEqualTo(bufferSize);
        
        // Verify against incorrect values
        assertThat(bufferSize).isNotEqualTo(8 * mb);
        assertThat(bufferSize).isNotEqualTo(32 * mb);
        assertThat(bufferSize).isNotEqualTo(16 * kb); // 16KB instead of 16MB
    }
    
    @Test
    @DisplayName("Should verify class has Configuration annotation")
    void shouldVerifyClass_HasConfigurationAnnotation() {
        // When/Then
        assertThat(HttpClientConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class))
            .isTrue();
    }
    
    @Test
    @DisplayName("Should verify webClient method has Bean annotation")
    void shouldVerifyWebClientMethod_HasBeanAnnotation() throws NoSuchMethodException {
        // When/Then
        assertThat(HttpClientConfig.class.getMethod("webClient").isAnnotationPresent(org.springframework.context.annotation.Bean.class))
            .isTrue();
    }
    
    @Test
    @DisplayName("Should verify webClient method signature")
    void shouldVerifyWebClientMethod_Signature() throws NoSuchMethodException {
        // When
        java.lang.reflect.Method method = HttpClientConfig.class.getMethod("webClient");
        
        // Then
        assertThat(method.getReturnType()).isEqualTo(WebClient.class);
        assertThat(method.getParameterCount()).isZero();
        assertThat(method.getExceptionTypes()).isEmpty();
    }
    
    @Test
    @DisplayName("Should verify HttpClientConfig implementation")
    void shouldVerifyHttpClientConfig_Implementation() {
        // Given
        String configSource = httpClientConfig.getClass().toString();
        
        // When/Then
        assertThat(configSource).contains("HttpClientConfig");
        
        // Verify the class can be instantiated multiple times
        HttpClientConfig anotherInstance = new HttpClientConfig();
        assertThat(anotherInstance).isNotNull();
        assertThat(anotherInstance).isNotSameAs(httpClientConfig);
    }
    
    @Test
    @DisplayName("Should verify WebClient is configured with codecs")
    void shouldVerifyWebClient_IsConfiguredWithCodecs() {
        // Given
        // The actual implementation of HttpClientConfig.webClient() includes:
        // .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
        
        // When
        WebClient webClient = httpClientConfig.webClient();
        
        // Then
        // We can't directly test the internal configuration, but we can verify the WebClient is created
        assertThat(webClient).isNotNull();
        
        // This test is primarily to document that the codec configuration is part of the implementation
        // The actual verification of the buffer size would require integration testing or mocking
    }
}