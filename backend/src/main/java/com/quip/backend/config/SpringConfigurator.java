package com.quip.backend.config;

import jakarta.websocket.server.ServerEndpointConfig;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

/**
 * Custom configurator for WebSocket endpoints that enables Spring dependency injection.
 * <p>
 * This class bridges the gap between the WebSocket API's endpoint instantiation mechanism
 * and Spring's dependency injection system. It allows WebSocket endpoints to be created
 * through Spring's bean factory, enabling them to have their dependencies autowired.
 * </p>
 */
public class SpringConfigurator extends ServerEndpointConfig.Configurator {
    /**
     * Static reference to Spring's bean factory.
     * This is used to create and autowire WebSocket endpoint instances.
     */
    private static AutowireCapableBeanFactory springContext;

    /**
     * Sets the Spring application context.
     * <p>
     * This method is called by the BackendApplication class when the application starts,
     * providing access to Spring's bean factory for WebSocket endpoint instantiation.
     * </p>
     *
     * @param context The Spring application context
     */
    public static void setApplicationContext(ApplicationContext context) {
        springContext = context.getAutowireCapableBeanFactory();
    }

    /**
     * Creates a new instance of a WebSocket endpoint class using Spring's bean factory.
     * <p>
     * This method overrides the default behavior of the WebSocket API, which would
     * normally create endpoint instances using the default constructor. Instead,
     * it delegates to Spring's bean factory, allowing for dependency injection.
     * </p>
     *
     * @param endpointClass The class of the endpoint to instantiate
     * @return A new instance of the endpoint class with dependencies autowired
     * @throws InstantiationException If the endpoint cannot be instantiated
     */
    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        return springContext.createBean(endpointClass);
    }
}