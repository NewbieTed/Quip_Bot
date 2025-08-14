/**
 * Test for the /lenza-resume command implementation
 */

import { ConversationManager } from '../services/conversation-manager';
import { BackendApiClient } from '../services/backend-api-client';
import { StreamingResponseHandler } from '../services/streaming-response-handler';

/**
 * Test conversation manager functionality for resume command
 */
function testConversationManagerForResume() {
  console.log('Testing ConversationManager for resume command...');
  
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
  
  // Test resume payload building with message
  const payloadWithMessage = manager.buildResumeConversationPayload(context, 'Continue conversation');
  console.log('✓ Resume conversation payload with message built:', {
    serverId: payloadWithMessage.serverId,
    channelId: payloadWithMessage.channelId,
    memberId: payloadWithMessage.memberId,
    hasMessage: payloadWithMessage.message !== undefined,
    messageLength: payloadWithMessage.message?.length || 0
  });
  
  // Test resume payload building without message
  const payloadWithoutMessage = manager.buildResumeConversationPayload(context);
  console.log('✓ Resume conversation payload without message built:', {
    serverId: payloadWithoutMessage.serverId,
    channelId: payloadWithoutMessage.channelId,
    memberId: payloadWithoutMessage.memberId,
    hasMessage: payloadWithoutMessage.message !== undefined
  });
  
  // Test resume payload with approval
  const payloadWithApproval = manager.buildResumeConversationPayload(context, undefined, true);
  console.log('✓ Resume conversation payload with approval built:', {
    serverId: payloadWithApproval.serverId,
    channelId: payloadWithApproval.channelId,
    memberId: payloadWithApproval.memberId,
    approved: payloadWithApproval.approved
  });
  
  // Test backend request conversion for resume
  const backendRequest = manager.convertToBackendRequest(payloadWithMessage);
  console.log('✓ Backend request converted for resume:', {
    channelId: backendRequest.channelId,
    memberId: backendRequest.memberId,
    serverId: backendRequest.serverId,
    messageLength: backendRequest.message.length
  });
  
  // Test backend request conversion without message
  const backendRequestNoMessage = manager.convertToBackendRequest(payloadWithoutMessage);
  console.log('✓ Backend request converted without message:', {
    channelId: backendRequestNoMessage.channelId,
    memberId: backendRequestNoMessage.memberId,
    serverId: backendRequestNoMessage.serverId,
    messageLength: backendRequestNoMessage.message.length
  });
}

/**
 * Test API client functionality for resume command
 */
function testApiClientForResumeCommand() {
  console.log('\nTesting API client for resume command...');
  
  try {
    const client = new BackendApiClient();
    console.log('✓ API client initialized for resume command');
    
    // Test that the resumeConversation method exists
    if (typeof client.resumeConversation === 'function') {
      console.log('✓ resumeConversation method available');
    } else {
      console.log('✗ resumeConversation method missing');
    }
    
    // Test that the legacy invokeAgent method still exists for backward compatibility
    if (typeof client.invokeAgent === 'function') {
      console.log('✓ invokeAgent method available (legacy support)');
    } else {
      console.log('✗ invokeAgent method missing');
    }
  } catch (error) {
    console.log('✗ API client initialization failed:', error);
  }
}

/**
 * Test resume command structure validation
 */
function testResumeCommandStructure() {
  console.log('\nTesting resume command structure...');
  
  try {
    // Import the command module
    const commandPath = '../commands/utility/lenza-resume';
    const command = require(commandPath);
    
    // Check required properties
    if ('data' in command && 'execute' in command) {
      console.log('✓ Resume command has required data and execute properties');
      
      // Check command data
      const commandData = command.data.toJSON();
      console.log('✓ Resume command data:', {
        name: commandData.name,
        description: commandData.description,
        optionsCount: commandData.options?.length || 0
      });
      
      // Check if it has the optional message option
      const messageOption = commandData.options?.find((opt: any) => opt.name === 'message');
      if (messageOption && !messageOption.required) {
        console.log('✓ Optional message option found');
      } else if (messageOption && messageOption.required) {
        console.log('✗ Message option should be optional, not required');
      } else {
        console.log('✗ Message option missing');
      }
      
      // Verify command name
      if (commandData.name === 'lenza-resume') {
        console.log('✓ Command name is correct');
      } else {
        console.log('✗ Command name is incorrect:', commandData.name);
      }
    } else {
      console.log('✗ Resume command missing required properties');
    }
  } catch (error) {
    console.log('✗ Failed to load resume command:', error);
  }
}

/**
 * Test payload validation for edge cases
 */
function testResumePayloadValidation() {
  console.log('\nTesting resume payload validation...');
  
  const manager = new ConversationManager();
  const context = manager.createConversationContext(
    '123456789',
    '987654321',
    '555666777',
    'testuser'
  );
  
  // Test with null message (should convert to undefined)
  try {
    const payload = manager.buildResumeConversationPayload(context, undefined);
    if (payload.message === undefined) {
      console.log('✓ Undefined message handled correctly');
    } else {
      console.log('✗ Undefined message not handled correctly');
    }
  } catch (error) {
    console.log('✗ Failed to handle undefined message:', error);
  }
  
  // Test with empty string message (should be trimmed)
  try {
    const payload = manager.buildResumeConversationPayload(context, '   ');
    if (payload.message === '') {
      console.log('✓ Empty string message trimmed correctly');
    } else {
      console.log('✗ Empty string message not trimmed correctly');
    }
  } catch (error) {
    console.log('✗ Failed to handle empty string message:', error);
  }
  
  // Test with tool whitelist update
  try {
    const payload = manager.buildResumeConversationPayload(
      context, 
      'test message', 
      undefined, 
      ['tool1', 'tool2']
    );
    if (payload.toolWhitelistUpdate && payload.toolWhitelistUpdate.length === 2) {
      console.log('✓ Tool whitelist update handled correctly');
    } else {
      console.log('✗ Tool whitelist update not handled correctly');
    }
  } catch (error) {
    console.log('✗ Failed to handle tool whitelist update:', error);
  }
}

/**
 * Run all tests for the lenza-resume command
 */
async function runLenzaResumeTests() {
  console.log('=== Lenza-Resume Command Tests ===\n');
  
  try {
    testConversationManagerForResume();
    testApiClientForResumeCommand();
    testResumeCommandStructure();
    testResumePayloadValidation();
    
    console.log('\n=== Lenza-Resume Command Tests Completed ===');
  } catch (error) {
    console.error('Lenza-Resume command test suite failed:', error);
  }
}

// Run tests if this file is executed directly
if (require.main === module) {
  runLenzaResumeTests();
}

export { runLenzaResumeTests };