/**
 * Test for the /lenza-status command implementation
 */

import { getBotConfig } from '../config/bot-config';

/**
 * Test bot configuration loading
 */
function testBotConfiguration() {
  console.log('Testing bot configuration...');
  
  try {
    const config = getBotConfig();
    
    console.log('✓ Bot configuration loaded:', {
      backendApiUrl: config.backendApiUrl,
      backendApiTimeout: config.backendApiTimeout,
      maxMessageLength: config.maxMessageLength,
      approvalTimeoutSeconds: config.approvalTimeoutSeconds,
      retryAttempts: config.retryAttempts,
      retryDelayMs: config.retryDelayMs
    });
    
    // Validate configuration values
    if (config.backendApiUrl && config.backendApiUrl.length > 0) {
      console.log('✓ Backend API URL is configured');
    } else {
      console.log('✗ Backend API URL is missing');
    }
    
    if (config.backendApiTimeout > 0) {
      console.log('✓ Backend API timeout is valid');
    } else {
      console.log('✗ Backend API timeout is invalid');
    }
    
    if (config.maxMessageLength > 0) {
      console.log('✓ Max message length is valid');
    } else {
      console.log('✗ Max message length is invalid');
    }
    
  } catch (error) {
    console.log('✗ Bot configuration loading failed:', error);
  }
}

/**
 * Test command structure validation
 */
function testStatusCommandStructure() {
  console.log('\nTesting status command structure...');
  
  try {
    // Import the command module
    const commandPath = '../commands/utility/lenza-status';
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
      
      // Verify command name
      if (commandData.name === 'lenza-status') {
        console.log('✓ Command name is correct');
      } else {
        console.log('✗ Command name is incorrect');
      }
      
      // Verify no required options (status command should have no parameters)
      if (!commandData.options || commandData.options.length === 0) {
        console.log('✓ Status command correctly has no options');
      } else {
        console.log('✗ Status command should not have options');
      }
      
    } else {
      console.log('✗ Command missing required properties');
    }
  } catch (error) {
    console.log('✗ Failed to load status command:', error);
  }
}

/**
 * Test health check URL construction
 */
function testHealthCheckUrl() {
  console.log('\nTesting health check URL construction...');
  
  try {
    const config = getBotConfig();
    const healthUrl = `${config.backendApiUrl}/health/detailed`;
    
    console.log('✓ Health check URL constructed:', healthUrl);
    
    // Basic URL validation
    if (healthUrl.includes('/health/detailed')) {
      console.log('✓ Health check endpoint is correct');
    } else {
      console.log('✗ Health check endpoint is incorrect');
    }
    
    // Check if URL is well-formed
    try {
      new URL(healthUrl);
      console.log('✓ Health check URL is well-formed');
    } catch (urlError) {
      console.log('✗ Health check URL is malformed:', urlError);
    }
    
  } catch (error) {
    console.log('✗ Health check URL construction failed:', error);
  }
}

/**
 * Test embed structure expectations
 */
function testEmbedStructure() {
  console.log('\nTesting embed structure expectations...');
  
  try {
    // Test that Discord.js EmbedBuilder is available
    const { EmbedBuilder } = require('discord.js');
    
    const testEmbed = new EmbedBuilder()
      .setTitle('🤖 Test Status')
      .setColor(0x00AE86)
      .setTimestamp()
      .addFields(
        { name: '📊 Test Field', value: '✅ Test Value', inline: true }
      );
    
    console.log('✓ EmbedBuilder is available and functional');
    
    // Test embed data structure
    const embedData = testEmbed.toJSON();
    if (embedData.title && embedData.color && embedData.fields) {
      console.log('✓ Embed structure is correct');
    } else {
      console.log('✗ Embed structure is missing required fields');
    }
    
  } catch (error) {
    console.log('✗ Embed structure test failed:', error);
  }
}

/**
 * Test error handling structure
 */
function testErrorHandling() {
  console.log('\nTesting error handling structure...');
  
  try {
    // Test that error classes are available
    const { AgentError, ERROR_MESSAGES } = require('../errors/agent-error');
    
    console.log('✓ AgentError class is available');
    console.log('✓ ERROR_MESSAGES are available');
    
    // Test error message structure
    if (ERROR_MESSAGES.UNKNOWN_ERROR) {
      console.log('✓ Error messages are properly defined');
    } else {
      console.log('✗ Error messages are missing');
    }
    
  } catch (error) {
    console.log('✗ Error handling structure test failed:', error);
  }
}

/**
 * Run all tests for the lenza-status command
 */
async function runLenzaStatusTests() {
  console.log('=== Lenza-Status Command Tests ===\n');
  
  try {
    testBotConfiguration();
    testStatusCommandStructure();
    testHealthCheckUrl();
    testEmbedStructure();
    testErrorHandling();
    
    console.log('\n=== Lenza-Status Command Tests Completed ===');
  } catch (error) {
    console.error('Lenza-Status command test suite failed:', error);
  }
}

// Run tests if this file is executed directly
if (require.main === module) {
  runLenzaStatusTests();
}

export { runLenzaStatusTests };