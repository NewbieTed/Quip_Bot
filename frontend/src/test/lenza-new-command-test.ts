/**
 * Test for the /lenza-new command implementation
 */

import { ConversationManager } from '../services/conversation-manager';
import { BackendApiClient } from '../services/backend-api-client';
import { StreamingResponseHandler } from '../services/streaming-response-handler';

/**
 * Test conversation manager functionality
 */
function testConversationManager() {
  console.log('Testing ConversationManager...');
  
  const manager = new ConversationManager();
  
  // Test context creation
  const context = manager.createConversationContext(
    '123456789',
    '987654321',
    '555666777',
    'testuser',
    'test-channel',
    'test-guild'
  );
  
  console.log('✓ Conversation context created:', {
    serverId: context.serverId,
    channelId: context.channelId,
    memberId: context.memberId,
    username: context.username
  });
  
  // Test payload building
  const payload = manager.buildNewConversationPayload(context, 'Hello Lenza!');
  console.log('✓ New conversation payload built:', {
    serverId: payload.serverId,
    channelId: payload.channelId,
    memberId: payload.memberId,
    messageLength: payload.message.length
  });
  
  // Test backend request conversion
  const backendRequest = manager.convertToBackendRequest(payload);
  console.log('✓ Backend request converted:', {
    channelId: backendRequest.channelId,
    memberId: backendRequest.memberId,
    serverId: backendRequest.serverId,
    messageLength: backendRequest.message.length
  });
  
  // Test validation
  try {
    manager.validateConversationContext(context);
    manager.validateMessage('Hello Lenza!');
    console.log('✓ Validation passed');
  } catch (error) {
    console.log('✗ Validation failed:', error);
  }
}

/**
 * Test message validation
 */
function testMessageValidation() {
  console.log('\nTesting message validation...');
  
  const manager = new ConversationManager();
  
  // Test valid message
  try {
    manager.validateMessage('Hello Lenza!');
    console.log('✓ Valid message accepted');
  } catch (error) {
    console.log('✗ Valid message rejected:', error);
  }
  
  // Test empty message
  try {
    manager.validateMessage('');
    console.log('✗ Empty message should be rejected');
  } catch (error) {
    console.log('✓ Empty message correctly rejected');
  }
  
  // Test long message
  try {
    const longMessage = 'a'.repeat(2001);
    manager.validateMessage(longMessage);
    console.log('✗ Long message should be rejected');
  } catch (error) {
    console.log('✓ Long message correctly rejected');
  }
}

/**
 * Test API client initialization and basic functionality
 */
function testApiClientForNewCommand() {
  console.log('\nTesting API client for new command...');
  
  try {
    const client = new BackendApiClient();
    console.log('✓ API client initialized for new command');
    
    // Test that the invokeNewConversation method exists
    if (typeof client.invokeNewConversation === 'function') {
      console.log('✓ invokeNewConversation method available');
    } else {
      console.log('✗ invokeNewConversation method missing');
    }
  } catch (error) {
    console.log('✗ API client initialization failed:', error);
  }
}

/**
 * Test command structure validation
 */
function testCommandStructure() {
  console.log('\nTesting command structure...');
  
  try {
    // Import the command module
    const commandPath = '../commands/utility/lenza-new';
    const command = require(commandPath);
    
    // Check required properties
    if ('data' in command && 'execute' in command) {
      console.log('✓ Command has required data and execute properties');
      
      // Check command data
      const commandData = command.data.toJSON();
      console.log('✓ Command data:', {
        name: commandData.name,
        description: commandData.description,
        optionsCount: commandData.options?.length || 0
      });
      
      // Check if it has the message option
      const messageOption = commandData.options?.find((opt: any) => opt.name === 'message');
      if (messageOption && messageOption.required) {
        console.log('✓ Required message option found');
      } else {
        console.log('✗ Required message option missing or not required');
      }
    } else {
      console.log('✗ Command missing required properties');
    }
  } catch (error) {
    console.log('✗ Failed to load command:', error);
  }
}

/**
 * Run all tests for the lenza-new command
 */
async function runLenzaNewTests() {
  console.log('=== Lenza-New Command Tests ===\n');
  
  try {
    testConversationManager();
    testMessageValidation();
    testApiClientForNewCommand();
    testCommandStructure();
    
    console.log('\n=== Lenza-New Command Tests Completed ===');
  } catch (error) {
    console.error('Lenza-New command test suite failed:', error);
  }
}

// Run tests if this file is executed directly
if (require.main === module) {
  runLenzaNewTests();
}

export { runLenzaNewTests };