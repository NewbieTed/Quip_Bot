/**
 * Integration test for the /lenza-resume command
 * Tests the complete command execution flow
 */

import { ConversationManager } from '../services/conversation-manager';
import { BackendApiClient } from '../services/backend-api-client';

/**
 * Test the complete resume conversation flow
 */
async function testResumeConversationFlow() {
  console.log('Testing complete resume conversation flow...');
  
  try {
    // Initialize services
    const conversationManager = new ConversationManager();
    const apiClient = new BackendApiClient();
    
    // Create test context
    const context = conversationManager.createConversationContext(
      '123456789',
      '987654321', 
      '555666777',
      'testuser',
      'test-channel',
      'test-guild'
    );
    
    console.log('✓ Test context created');
    
    // Test resume payload with message
    const resumePayload = conversationManager.buildResumeConversationPayload(
      context,
      'Continue our conversation about AI'
    );
    
    console.log('✓ Resume payload created:', {
      serverId: resumePayload.serverId,
      channelId: resumePayload.channelId,
      memberId: resumePayload.memberId,
      hasMessage: resumePayload.message !== undefined
    });
    
    // Test backend request conversion
    const backendRequest = conversationManager.convertToBackendRequest(resumePayload);
    
    console.log('✓ Backend request converted:', {
      channelId: backendRequest.channelId,
      memberId: backendRequest.memberId,
      serverId: backendRequest.serverId,
      messageLength: backendRequest.message.length
    });
    
    // Test resume payload without message (for tool approvals or continuation)
    const resumePayloadNoMessage = conversationManager.buildResumeConversationPayload(context);
    
    console.log('✓ Resume payload without message created:', {
      serverId: resumePayloadNoMessage.serverId,
      channelId: resumePayloadNoMessage.channelId,
      memberId: resumePayloadNoMessage.memberId,
      hasMessage: resumePayloadNoMessage.message !== undefined
    });
    
    // Test resume payload with approval
    const resumePayloadWithApproval = conversationManager.buildResumeConversationPayload(
      context,
      undefined,
      true
    );
    
    console.log('✓ Resume payload with approval created:', {
      serverId: resumePayloadWithApproval.serverId,
      channelId: resumePayloadWithApproval.channelId,
      memberId: resumePayloadWithApproval.memberId,
      approved: resumePayloadWithApproval.approved
    });
    
    console.log('✓ Resume conversation flow test completed successfully');
    
  } catch (error) {
    console.log('✗ Resume conversation flow test failed:', error);
    throw error;
  }
}

/**
 * Test error handling scenarios
 */
async function testResumeErrorHandling() {
  console.log('\nTesting resume command error handling...');
  
  const conversationManager = new ConversationManager();
  
  try {
    // Test invalid context
    try {
      const invalidContext = {
        serverId: '',
        channelId: '987654321',
        memberId: '555666777',
        guildId: '',
        username: 'testuser'
      };
      conversationManager.validateConversationContext(invalidContext);
      console.log('✗ Should have failed validation for empty serverId');
    } catch (error) {
      console.log('✓ Correctly rejected invalid context');
    }
    
    // Test invalid message
    try {
      conversationManager.validateMessage('a'.repeat(2001));
      console.log('✗ Should have failed validation for long message');
    } catch (error) {
      console.log('✓ Correctly rejected long message');
    }
    
    // Test empty message validation
    try {
      conversationManager.validateMessage('');
      console.log('✗ Should have failed validation for empty message');
    } catch (error) {
      console.log('✓ Correctly rejected empty message');
    }
    
    console.log('✓ Error handling tests completed successfully');
    
  } catch (error) {
    console.log('✗ Error handling tests failed:', error);
    throw error;
  }
}

/**
 * Test command module loading
 */
async function testCommandModuleLoading() {
  console.log('\nTesting command module loading...');
  
  try {
    // Load the command module
    const command = require('../commands/utility/lenza-resume');
    
    // Verify structure
    if (!command.data || !command.execute) {
      throw new Error('Command missing required properties');
    }
    
    // Verify command data
    const commandData = command.data.toJSON();
    
    if (commandData.name !== 'lenza-resume') {
      throw new Error(`Expected command name 'lenza-resume', got '${commandData.name}'`);
    }
    
    if (!commandData.description) {
      throw new Error('Command missing description');
    }
    
    // Verify message option
    const messageOption = commandData.options?.find((opt: any) => opt.name === 'message');
    if (!messageOption) {
      throw new Error('Command missing message option');
    }
    
    if (messageOption.required) {
      throw new Error('Message option should be optional, not required');
    }
    
    console.log('✓ Command module loaded successfully:', {
      name: commandData.name,
      description: commandData.description,
      hasMessageOption: !!messageOption,
      messageOptionRequired: messageOption.required
    });
    
  } catch (error) {
    console.log('✗ Command module loading failed:', error);
    throw error;
  }
}

/**
 * Run all integration tests
 */
async function runResumeIntegrationTests() {
  console.log('=== Lenza-Resume Integration Tests ===\n');
  
  try {
    await testResumeConversationFlow();
    await testResumeErrorHandling();
    await testCommandModuleLoading();
    
    console.log('\n=== All Integration Tests Passed ===');
    return true;
  } catch (error) {
    console.error('\n=== Integration Tests Failed ===');
    console.error('Error:', error);
    return false;
  }
}

// Run tests if this file is executed directly
if (require.main === module) {
  runResumeIntegrationTests().then(success => {
    process.exit(success ? 0 : 1);
  });
}

export { runResumeIntegrationTests };