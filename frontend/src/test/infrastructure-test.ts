/**
 * Basic test to verify core infrastructure components
 */

import { getBotConfig } from '../config/bot-config';
import { AgentError, getUserFriendlyErrorMessage } from '../errors/agent-error';
import { NetworkErrorHandler } from '../utils/network-error-handler';
import { BackendApiClient } from '../services/backend-api-client';

/**
 * Test configuration loading
 */
function testConfiguration() {
  console.log('Testing configuration...');
  
  const config = getBotConfig();
  console.log('✓ Configuration loaded:', {
    backendApiUrl: config.backendApiUrl,
    backendApiTimeout: config.backendApiTimeout,
    maxMessageLength: config.maxMessageLength
  });
}

/**
 * Test error handling
 */
function testErrorHandling() {
  console.log('\nTesting error handling...');
  
  const networkError = new AgentError('network', 'Test network error');
  const timeoutError = new AgentError('timeout', 'Test timeout error');
  const apiError = new AgentError('api', 'Test API error');
  
  console.log('✓ Network error message:', getUserFriendlyErrorMessage(networkError));
  console.log('✓ Timeout error message:', getUserFriendlyErrorMessage(timeoutError));
  console.log('✓ API error message:', getUserFriendlyErrorMessage(apiError));
}

/**
 * Test network error handler
 */
async function testNetworkHandler() {
  console.log('\nTesting network handler...');
  
  const handler = new NetworkErrorHandler();
  
  // Test successful operation
  try {
    const result = await handler.handleWithRetry(async () => {
      return 'success';
    }, 1);
    console.log('✓ Successful operation result:', result);
  } catch (error) {
    console.log('✗ Unexpected error in successful operation:', error);
  }
  
  // Test failed operation (should not retry non-retryable errors)
  try {
    await handler.handleWithRetry(async () => {
      throw new AgentError('parsing', 'Test parsing error');
    }, 2);
    console.log('✗ Should have thrown error');
  } catch (error) {
    if (error instanceof AgentError && error.type === 'parsing') {
      console.log('✓ Non-retryable error handled correctly');
    } else {
      console.log('✗ Unexpected error type:', error);
    }
  }
}

/**
 * Test API client initialization
 */
function testApiClient() {
  console.log('\nTesting API client...');
  
  try {
    const client = new BackendApiClient();
    console.log('✓ Backend API client initialized successfully');
  } catch (error) {
    console.log('✗ Failed to initialize API client:', error);
  }
}

/**
 * Run all tests
 */
async function runTests() {
  console.log('=== Core Infrastructure Tests ===\n');
  
  try {
    testConfiguration();
    testErrorHandling();
    await testNetworkHandler();
    testApiClient();
    
    console.log('\n=== All Tests Completed ===');
  } catch (error) {
    console.error('Test suite failed:', error);
  }
}

// Run tests if this file is executed directly
if (require.main === module) {
  runTests();
}

export { runTests };