package com.quip.backend.config.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Custom cache key generator that creates consistent and meaningful cache keys
 * for the application's caching needs.
 */
@Slf4j
@Component("customCacheKeyGenerator")
public class CustomCacheKeyGenerator implements KeyGenerator {

    private static final String KEY_SEPARATOR = ":";
    private static final String NULL_VALUE = "null";

    /**
     * Generates a cache key based on the target object, method, and parameters.
     * 
     * @param target the target instance
     * @param method the method being called
     * @param params the method parameters
     * @return generated cache key
     */
    @NonNull
    @Override
    public Object generate(Object target, Method method, Object... params) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // Add class name
        keyBuilder.append(target.getClass().getSimpleName());
        keyBuilder.append(KEY_SEPARATOR);
        
        // Add method name
        keyBuilder.append(method.getName());
        
        // Add parameters if present
        if (params.length > 0) {
            keyBuilder.append(KEY_SEPARATOR);
            String paramString = Arrays.stream(params)
                    .map(this::parameterToString)
                    .collect(Collectors.joining(KEY_SEPARATOR));
            keyBuilder.append(paramString);
        }
        
        String cacheKey = keyBuilder.toString();
        log.debug("Generated cache key: {}", cacheKey);
        
        return cacheKey;
    }

    /**
     * Converts a parameter to a string representation for cache key generation.
     * 
     * @param param the parameter to convert
     * @return string representation of the parameter
     */
    private String parameterToString(Object param) {
        if (param == null) {
            return NULL_VALUE;
        }
        
        // Handle common types that need special formatting
        if (param instanceof String) {
            return (String) param;
        } else if (param instanceof Number) {
            return param.toString();
        } else if (param instanceof Boolean) {
            return param.toString();
        } else {
            // For complex objects, use hashCode to ensure uniqueness
            return param.getClass().getSimpleName() + "_" + Math.abs(param.hashCode());
        }
    }
}