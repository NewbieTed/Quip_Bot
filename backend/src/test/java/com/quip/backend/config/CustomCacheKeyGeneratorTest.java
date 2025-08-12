package com.quip.backend.config;

import com.quip.backend.config.redis.CustomCacheKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CustomCacheKeyGenerator
 */
@ExtendWith(MockitoExtension.class)
class CustomCacheKeyGeneratorTest {

    private CustomCacheKeyGenerator keyGenerator;
    private TestService testService;

    @BeforeEach
    void setUp() {
        keyGenerator = new CustomCacheKeyGenerator();
        testService = new TestService();
    }

    @Test
    void generate_WithNoParameters_ShouldReturnClassAndMethodName() throws NoSuchMethodException {
        // Given
        Method method = TestService.class.getMethod("methodWithNoParams");
        
        // When
        Object key = keyGenerator.generate(testService, method);
        
        // Then
        assertThat(key).isEqualTo("TestService:methodWithNoParams");
    }

    @Test
    void generate_WithStringParameter_ShouldIncludeParameter() throws NoSuchMethodException {
        // Given
        Method method = TestService.class.getMethod("methodWithStringParam", String.class);
        String param = "testParam";
        
        // When
        Object key = keyGenerator.generate(testService, method, param);
        
        // Then
        assertThat(key).isEqualTo("TestService:methodWithStringParam:testParam");
    }

    @Test
    void generate_WithMultipleParameters_ShouldIncludeAllParameters() throws NoSuchMethodException {
        // Given
        Method method = TestService.class.getMethod("methodWithMultipleParams", String.class, Integer.class, Boolean.class);
        String stringParam = "test";
        Integer intParam = 123;
        Boolean boolParam = true;
        
        // When
        Object key = keyGenerator.generate(testService, method, stringParam, intParam, boolParam);
        
        // Then
        assertThat(key).isEqualTo("TestService:methodWithMultipleParams:test:123:true");
    }

    @Test
    void generate_WithNullParameter_ShouldHandleNull() throws NoSuchMethodException {
        // Given
        Method method = TestService.class.getMethod("methodWithStringParam", String.class);
        String param = null;
        
        // When
        Object key = keyGenerator.generate(testService, method, param);
        
        // Then
        assertThat(key).isEqualTo("TestService:methodWithStringParam:null");
    }

    @Test
    void generate_WithComplexObject_ShouldUseHashCode() throws NoSuchMethodException {
        // Given
        Method method = TestService.class.getMethod("methodWithComplexParam", Object.class);
        TestObject complexParam = new TestObject("test");
        
        // When
        Object key = keyGenerator.generate(testService, method, complexParam);
        
        // Then
        String keyString = (String) key;
        assertThat(keyString).startsWith("TestService:methodWithComplexParam:TestObject_");
        assertThat(keyString).contains(String.valueOf(Math.abs(complexParam.hashCode())));
    }

    @Test
    void generate_WithNumberParameter_ShouldIncludeNumber() throws NoSuchMethodException {
        // Given
        Method method = TestService.class.getMethod("methodWithNumberParam", Long.class);
        Long param = 12345L;
        
        // When
        Object key = keyGenerator.generate(testService, method, param);
        
        // Then
        assertThat(key).isEqualTo("TestService:methodWithNumberParam:12345");
    }

    @Test
    void generate_WithBooleanParameter_ShouldIncludeBoolean() throws NoSuchMethodException {
        // Given
        Method method = TestService.class.getMethod("methodWithBooleanParam", Boolean.class);
        Boolean param = false;
        
        // When
        Object key = keyGenerator.generate(testService, method, param);
        
        // Then
        assertThat(key).isEqualTo("TestService:methodWithBooleanParam:false");
    }

    // Test service class for method reflection
    public static class TestService {
        public void methodWithNoParams() {}
        public void methodWithStringParam(String param) {}
        public void methodWithMultipleParams(String str, Integer num, Boolean bool) {}
        public void methodWithComplexParam(Object param) {}
        public void methodWithNumberParam(Long param) {}
        public void methodWithBooleanParam(Boolean param) {}
    }

    // Test object class for complex parameter testing
    public static class TestObject {
        private final String value;

        public TestObject(String value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestObject that = (TestObject) obj;
            return value != null ? value.equals(that.value) : that.value == null;
        }
    }
}