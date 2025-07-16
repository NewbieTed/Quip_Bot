package com.quip.backend.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;

public abstract class BaseTest {

    @BeforeEach
    void logBefore(TestInfo testInfo) {
        System.out.println("Testing: " + testInfo.getDisplayName());
    }

    @AfterEach
    void logAfter(TestInfo testInfo) {
        System.out.println("Test Passed: " + testInfo.getDisplayName());
    }
}