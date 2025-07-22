package com.quip.backend.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;

/**
 * Base class for all test classes in the application.
 * <p>
 * This abstract class provides common functionality for all tests,
 * such as logging test execution. All test classes should extend this
 * class to inherit the common test setup and teardown behavior.
 * </p>
 */
public abstract class BaseTest {

    /**
     * Logs the start of each test method.
     * <p>
     * This method is executed before each test method and prints the test name
     * to the console, making it easier to track test execution in logs.
     * </p>
     *
     * @param testInfo Information about the current test
     */
    @BeforeEach
    void logBefore(TestInfo testInfo) {
        System.out.println("Testing: " + testInfo.getDisplayName());
    }

    /**
     * Logs the successful completion of each test method.
     * <p>
     * This method is executed after each test method and prints a success message
     * to the console. Note that this method will only run if the test passes.
     * </p>
     *
     * @param testInfo Information about the current test
     */
    @AfterEach
    void logAfter(TestInfo testInfo) {
        System.out.println("Test Passed: " + testInfo.getDisplayName());
    }
}