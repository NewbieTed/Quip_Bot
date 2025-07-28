package com.quip.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Global Jackson configuration to ensure proper handling of Java 8 date/time types
 * across the entire application, including Redis serialization.
 */
@Slf4j
@Configuration
public class JacksonConfig {

    /**
     * Creates a primary ObjectMapper bean that will be used throughout the application.
     * This ensures consistent JSON serialization/deserialization behavior.
     *
     * @return configured ObjectMapper with Java 8 time support
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Register JavaTimeModule to handle Java 8 date/time types
        objectMapper.registerModule(new JavaTimeModule());
        
        // Disable writing dates as timestamps to use ISO format
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        log.info("Global ObjectMapper configured with JavaTimeModule support");
        return objectMapper;
    }
}