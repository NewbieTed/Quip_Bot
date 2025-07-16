package com.quip.backend.config;

import com.quip.backend.common.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SpringConfigurator}.
 * <p>
 * This test class validates the Spring WebSocket configurator functionality including
 * proper application context setup and endpoint instance creation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpringConfigurator Tests")
class SpringConfiguratorTest extends BaseTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    private SpringConfigurator springConfigurator;

    @BeforeEach
    void setUp() {
        springConfigurator = new SpringConfigurator();
    }

    @Test
    @DisplayName("Should set application context successfully")
    void shouldSetApplicationContext_Successfully() {
        // Given
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(autowireCapableBeanFactory);

        // When
        SpringConfigurator.setApplicationContext(applicationContext);

        // Then
        verify(applicationContext).getAutowireCapableBeanFactory();
    }

    @Test
    @DisplayName("Should create endpoint instance successfully")
    void shouldCreateEndpointInstance_Successfully() throws InstantiationException {
        // Given
        TestEndpoint expectedInstance = new TestEndpoint();
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(autowireCapableBeanFactory);
        when(autowireCapableBeanFactory.createBean(TestEndpoint.class)).thenReturn(expectedInstance);

        SpringConfigurator.setApplicationContext(applicationContext);

        // When
        TestEndpoint result = springConfigurator.getEndpointInstance(TestEndpoint.class);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isSameAs(expectedInstance);
        verify(autowireCapableBeanFactory).createBean(TestEndpoint.class);
    }

    @Test
    @DisplayName("Should throw NullPointerException when spring context is null")
    void shouldThrowNullPointerException_WhenSpringContextIsNull() {
        // Given - Create a mock ApplicationContext that returns null for getAutowireCapableBeanFactory
        ApplicationContext nullContext = mock(ApplicationContext.class);
        when(nullContext.getAutowireCapableBeanFactory()).thenReturn(null);
        SpringConfigurator.setApplicationContext(nullContext);

        // When & Then
        assertThatThrownBy(() -> springConfigurator.getEndpointInstance(TestEndpoint.class))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should handle multiple endpoint instance creations")
    void shouldHandleMultipleEndpointInstanceCreations_Successfully() throws InstantiationException {
        // Given
        TestEndpoint firstInstance = new TestEndpoint();
        TestEndpoint secondInstance = new TestEndpoint();
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(autowireCapableBeanFactory);
        when(autowireCapableBeanFactory.createBean(TestEndpoint.class))
                .thenReturn(firstInstance)
                .thenReturn(secondInstance);

        SpringConfigurator.setApplicationContext(applicationContext);

        // When
        TestEndpoint first = springConfigurator.getEndpointInstance(TestEndpoint.class);
        TestEndpoint second = springConfigurator.getEndpointInstance(TestEndpoint.class);

        // Then
        assertThat(first).isSameAs(firstInstance);
        assertThat(second).isSameAs(secondInstance);
        assertThat(first).isNotSameAs(second);
        verify(autowireCapableBeanFactory, times(2)).createBean(TestEndpoint.class);
    }

    @Test
    @DisplayName("Should handle different endpoint classes")
    void shouldHandleDifferentEndpointClasses_Successfully() throws InstantiationException {
        // Given
        TestEndpoint testInstance = new TestEndpoint();
        AnotherTestEndpoint anotherInstance = new AnotherTestEndpoint();
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(autowireCapableBeanFactory);
        when(autowireCapableBeanFactory.createBean(TestEndpoint.class)).thenReturn(testInstance);
        when(autowireCapableBeanFactory.createBean(AnotherTestEndpoint.class)).thenReturn(anotherInstance);

        SpringConfigurator.setApplicationContext(applicationContext);

        // When
        TestEndpoint testResult = springConfigurator.getEndpointInstance(TestEndpoint.class);
        AnotherTestEndpoint anotherResult = springConfigurator.getEndpointInstance(AnotherTestEndpoint.class);

        // Then
        assertThat(testResult).isSameAs(testInstance);
        assertThat(anotherResult).isSameAs(anotherInstance);
        verify(autowireCapableBeanFactory).createBean(TestEndpoint.class);
        verify(autowireCapableBeanFactory).createBean(AnotherTestEndpoint.class);
    }

    @Test
    @DisplayName("Should propagate RuntimeException from createBean")
    void shouldPropagateRuntimeException_FromCreateBean() {
        // Given
        RuntimeException expectedException = new RuntimeException("Test exception");
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(autowireCapableBeanFactory);
        when(autowireCapableBeanFactory.createBean(TestEndpoint.class)).thenThrow(expectedException);

        SpringConfigurator.setApplicationContext(applicationContext);

        // When & Then
        assertThatThrownBy(() -> springConfigurator.getEndpointInstance(TestEndpoint.class))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test exception");
    }

    @Test
    @DisplayName("Should instantiate SpringConfigurator successfully")
    void shouldInstantiateSpringConfigurator_Successfully() {
        // When & Then
        assertThat(springConfigurator).isNotNull();
    }

    @Test
    @DisplayName("Should handle null application context gracefully")
    void shouldHandleNullApplicationContext_Gracefully() {
        // Given - Pass null directly to setApplicationContext
        // When
        assertThatThrownBy(() -> SpringConfigurator.setApplicationContext(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should override application context when set multiple times")
    void shouldOverrideApplicationContext_WhenSetMultipleTimes() throws InstantiationException {
        // Given
        ApplicationContext firstContext = mock(ApplicationContext.class);
        ApplicationContext secondContext = mock(ApplicationContext.class);
        AutowireCapableBeanFactory firstFactory = mock(AutowireCapableBeanFactory.class);
        AutowireCapableBeanFactory secondFactory = mock(AutowireCapableBeanFactory.class);
        TestEndpoint expectedInstance = new TestEndpoint();

        when(firstContext.getAutowireCapableBeanFactory()).thenReturn(firstFactory);
        when(secondContext.getAutowireCapableBeanFactory()).thenReturn(secondFactory);
        when(secondFactory.createBean(TestEndpoint.class)).thenReturn(expectedInstance);

        // When
        SpringConfigurator.setApplicationContext(firstContext);
        SpringConfigurator.setApplicationContext(secondContext);
        TestEndpoint result = springConfigurator.getEndpointInstance(TestEndpoint.class);

        // Then
        assertThat(result).isSameAs(expectedInstance);
        verify(firstContext).getAutowireCapableBeanFactory();
        verify(secondContext).getAutowireCapableBeanFactory();
        verify(secondFactory).createBean(TestEndpoint.class);
        verifyNoInteractions(firstFactory);
    }

    // Test helper classes
    private static class TestEndpoint {
    }

    private static class AnotherTestEndpoint {
    }
}
