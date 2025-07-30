package com.quip.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for HTTP client functionality.
 * <p>
 * This class configures HTTP clients for reactive communication with external services.
 * It provides beans for creating HTTP connections to external APIs and services.
 * </p>
 */
@Configuration
public class HttpClientConfig {

    /**
     * Creates a WebClient for making HTTP requests to external services.
     * <p>
     * This client is used by services to make HTTP requests to external APIs
     * and handle streaming responses. It's configured with appropriate buffer
     * sizes to handle large streaming responses.
     * </p>
     *
     * @return A reactive HTTP client implementation
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB buffer
                .build();
    }

    /**
     * Creates a RestTemplate for making synchronous HTTP requests to external services.
     * <p>
     * This client is used by services to make blocking HTTP requests to external APIs
     * where synchronous communication is preferred over reactive streams.
     * </p>
     *
     * @return A synchronous HTTP client implementation
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}