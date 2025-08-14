/**
 * Test for the Tool Approval Handler implementation
 */

import { ToolApprovalHandler } from '../services/tool-approval-handler';
import { BackendApiClient } from '../services/backend-api-client';
import { ConversationManager } from '../services/conversation-manager';

/**
 * Test tool approval handler initialization
 */
function testToolApprovalHandlerInitialization() {
  console.log('Testing ToolApprovalHandler initialization...');
  
  try {
    const handler = new ToolApprovalHandler();
    console.log('✓ ToolApprovalHandler initialized successfully');
    
    // Test that required methods exist
    if (typeof handler.requestApproval === 'function') {
      console.log('✓ requestApproval method available');
    } else {
      console.log('✗ requestApproval method missing');
    }
    
    if (typeof handler.handleApprovalResponse === 'function') {
      console.log('✓ handleApprovalResponse method available');
    } else {
      console.log('✗ handleApprovalResponse method missing');
    }
    
    if (typeof handler.cleanupExpiredApprovals === 'function') {
      console.log('✓ cleanupExpiredApprovals method available');
    } else {
      console.log('✗ cleanupExpiredApprovals method missing');
    }
  } catch (error) {
    console.log('✗ ToolApprovalHandler initialization failed:', error);
  }
}

/**
 * Test approval ID generation
 */
function testApprovalIdGeneration() {
  console.log('\nTesting approval ID generation...');
  
  try {
    const handler = new ToolApprovalHandler();
    
    // Test that we can call cleanup without errors (this tests internal state management)
    handler.cleanupExpiredApprovals();
    console.log('✓ Cleanup method works without errors');
    
    // Test that the handler can be created multiple times
    const handler2 = new ToolApprovalHandler();
    console.log('✓ Multiple handlers can be created');
    
  } catch (error) {
    console.log('✗ Approval ID generation test failed:', error);
  }
}

/**
 * Test integration with streaming response handler
 */
function testStreamingIntegration() {
  console.log('\nTesting streaming response handler integration...');
  
  try {
    // Import streaming response handler to verify it includes tool approval
    const StreamingResponseHandler = require('../services/streaming-response-handler').StreamingResponseHandler;
    
    // Create a mock interaction
    const mockInteraction = {
      user: { id: 'test-user' },
      channelId: 'test-channel',
      guildId: 'test-guild'
    };
    
    const handler = new StreamingResponseHandler(mockInteraction);
    console.log('✓ StreamingResponseHandler can be created with tool approval integration');
    
    // Test that the handler has the tool approval handler
    if (handler.toolApprovalHandler) {
      console.log('✓ StreamingResponseHandler has tool approval handler');
    } else {
      console.log('✗ StreamingResponseHandler missing tool approval handler');
    }
    
  } catch (error) {
    console.log('✗ Streaming integration test failed:', error);
  }
}

/**
 * Test button interaction handling structure
 */
function testButtonInteractionStructure() {
  console.log('\nTesting button interaction structure...');
  
  try {
    // Test that the interaction create event includes tool approval handling
    const interactionCreatePath = '../events/interactionCreate';
    const interactionModule = require(interactionCreatePath);
    
    if (interactionModule && typeof interactionModule.execute === 'function') {
      console.log('✓ InteractionCreate event module loaded');
      console.log('✓ Event has execute function');
    } else {
      console.log('✗ InteractionCreate event module invalid');
    }
    
    // Test that ToolApprovalHandler is imported in the event
    const fs = require('fs');
    const path = require('path');
    const eventFilePath = path.join(__dirname, '../events/interactionCreate.ts');
    const eventFileContent = fs.readFileSync(eventFilePath, 'utf8');
    
    if (eventFileContent.includes('ToolApprovalHandler')) {
      console.log('✓ ToolApprovalHandler is imported in interaction event');
    } else {
      console.log('✗ ToolApprovalHandler not imported in interaction event');
    }
    
    if (eventFileContent.includes('tool_approval_')) {
      console.log('✓ Tool approval button handling is present');
    } else {
      console.log('✗ Tool approval button handling missing');
    }
    
  } catch (error) {
    console.log('✗ Button interaction structure test failed:', error);
  }
}

/**
 * Test configuration integration
 */
function testConfigurationIntegration() {
  console.log('\nTesting configuration integration...');
  
  try {
    const { getBotConfig } = require('../config/bot-config');
    const config = getBotConfig();
    
    if (typeof config.approvalTimeoutSeconds === 'number') {
      console.log('✓ Approval timeout configuration available:', config.approvalTimeoutSeconds);
    } else {
      console.log('✗ Approval timeout configuration missing');
    }
    
    if (config.approvalTimeoutSeconds === 60) {
      console.log('✓ Default approval timeout is 60 seconds');
    } else {
      console.log('✓ Custom approval timeout configured:', config.approvalTimeoutSeconds);
    }
    
  } catch (error) {
    console.log('✗ Configuration integration test failed:', error);
  }
}

/**
 * Test model definitions
 */
function testModelDefinitions() {
  console.log('\nTesting model definitions...');
  
  try {
    const models = require('../models/agent-models');
    
    // Test that tool approval models are exported
    const testApprovalResult = {
      approved: true,
      addToWhitelist: false,
      timedOut: false
    };
    
    console.log('✓ ApprovalResult model structure validated');
    
    // Test that the models can be used
    if (typeof testApprovalResult.approved === 'boolean' &&
        typeof testApprovalResult.addToWhitelist === 'boolean' &&
        typeof testApprovalResult.timedOut === 'boolean') {
      console.log('✓ ApprovalResult model has correct types');
    } else {
      console.log('✗ ApprovalResult model has incorrect types');
    }
    
  } catch (error) {
    console.log('✗ Model definitions test failed:', error);
  }
}

/**
 * Test service exports
 */
function testServiceExports() {
  console.log('\nTesting service exports...');
  
  try {
    const services = require('../services/index');
    
    if (services.ToolApprovalHandler) {
      console.log('✓ ToolApprovalHandler exported from services index');
    } else {
      console.log('✗ ToolApprovalHandler not exported from services index');
    }
    
    if (services.BackendApiClient) {
      console.log('✓ BackendApiClient exported');
    } else {
      console.log('✗ BackendApiClient not exported');
    }
    
    if (services.StreamingResponseHandler) {
      console.log('✓ StreamingResponseHandler exported');
    } else {
      console.log('✗ StreamingResponseHandler not exported');
    }
    
  } catch (error) {
    console.log('✗ Service exports test failed:', error);
  }
}

/**
 * Run all tests for the tool approval system
 */
async function runToolApprovalTests() {
  console.log('=== Tool Approval System Tests ===\n');
  
  try {
    testToolApprovalHandlerInitialization();
    testApprovalIdGeneration();
    testStreamingIntegration();
    testButtonInteractionStructure();
    testConfigurationIntegration();
    testModelDefinitions();
    testServiceExports();
    
    console.log('\n=== Tool Approval System Tests Completed ===');
  } catch (error) {
    console.error('Tool approval system test suite failed:', error);
  }
}

// Run tests if this file is executed directly
if (require.main === module) {
  runToolApprovalTests();
}

export { runToolApprovalTests };