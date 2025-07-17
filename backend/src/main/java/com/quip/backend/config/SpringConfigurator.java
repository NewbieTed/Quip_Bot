package com.quip.backend.config;

import jakarta.websocket.server.ServerEndpointConfig;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

public class SpringConfigurator extends ServerEndpointConfig.Configurator {
    private static AutowireCapableBeanFactory springContext;

    public static void setApplicationContext(ApplicationContext context) {
        springContext = context.getAutowireCapableBeanFactory();
    }

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        return springContext.createBean(endpointClass);
    }
}