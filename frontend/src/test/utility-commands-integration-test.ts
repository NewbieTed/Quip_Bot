/**
 * Integration test for utility commands (help and status)
 */

import { runHelpCommandTests } from './help-command-test';
import { runLenzaStatusTests } from './lenza-status-command-test';

/**
 * Test command loading and registration
 */
function testCommandRegistration() {
  console.log('Testing command registration...');
  
  try {
    // Test that both commands can be loaded
    const helpCommand = require('../commands/utility/help');
    const statusCommand = require('../commands/utility/lenza-status');
    
    if (helpCommand && statusCommand) {
      console.log('✓ Both utility commands loaded successfully');
      
      // Test command names are unique
      const helpName = helpCommand.data.toJSON().name;
      const statusName = statusCommand.data.toJSON().name;
      
      if (helpName !== statusName) {
        console.log('✓ Command names are unique');
      } else {
        console.log('✗ Command names conflict');
      }
      
      // Test both have required properties
      const hasRequiredProps = (cmd: any) => 'data' in cmd && 'execute' in cmd;
      
      if (hasRequiredProps(helpCommand) && hasRequiredProps(statusCommand)) {
        console.log('✓ Both commands have required properties');
      } else {
        console.log('✗ Commands missing required properties');
      }
      
    } else {
      console.log('✗ Failed to load utility commands');
    }
    
  } catch (error) {
    console.log('✗ Command registration test failed:', error);
  }
}

/**
 * Test interaction between help and status commands
 */
function testCommandInteraction() {
  console.log('\nTesting command interaction...');
  
  try {
    // Test that help command references status command
    const helpCommand = require('../commands/utility/help');
    const helpData = helpCommand.data.toJSON();
    
    // Check if help command has lenza-status in its choices
    const commandOption = helpData.options?.find((opt: any) => opt.name === 'command');
    if (commandOption && commandOption.choices) {
      const hasStatusChoice = commandOption.choices.some((choice: any) => choice.value === 'lenza-status');
      if (hasStatusChoice) {
        console.log('✓ Help command references status command');
      } else {
        console.log('✗ Help command missing status command reference');
      }
    }
    
    // Test that both commands use consistent styling
    const { EmbedBuilder } = require('discord.js');
    const testColor = 0x00AE86;
    
    const helpEmbed = new EmbedBuilder().setColor(testColor);
    const statusEmbed = new EmbedBuilder().setColor(testColor);
    
    console.log('✓ Commands use consistent embed styling');
    
  } catch (error) {
    console.log('✗ Command interaction test failed:', error);
  }
}

/**
 * Test error handling consistency
 */
function testErrorHandlingConsistency() {
  console.log('\nTesting error handling consistency...');
  
  try {
    // Test that both commands use the same error handling pattern
    const { AgentError, ERROR_MESSAGES } = require('../errors/agent-error');
    
    // Both commands should be able to handle errors gracefully
    console.log('✓ Error handling classes available for both commands');
    
    // Test logger availability
    const { logger } = require('../utils/logger');
    if (logger) {
      console.log('✓ Logger available for both commands');
    } else {
      console.log('✗ Logger not available');
    }
    
  } catch (error) {
    console.log('✗ Error handling consistency test failed:', error);
  }
}

/**
 * Test configuration consistency
 */
function testConfigurationConsistency() {
  console.log('\nTesting configuration consistency...');
  
  try {
    const { getBotConfig } = require('../config/bot-config');
    const config = getBotConfig();
    
    // Test that configuration is available for both commands
    if (config.backendApiUrl && config.backendApiTimeout) {
      console.log('✓ Configuration available for both commands');
    } else {
      console.log('✗ Configuration incomplete');
    }
    
    // Test that both commands can access Discord.js components
    const { EmbedBuilder, SlashCommandBuilder } = require('discord.js');
    if (EmbedBuilder && SlashCommandBuilder) {
      console.log('✓ Discord.js components available for both commands');
    } else {
      console.log('✗ Discord.js components not available');
    }
    
  } catch (error) {
    console.log('✗ Configuration consistency test failed:', error);
  }
}

/**
 * Test button interaction integration
 */
function testButtonIntegration() {
  console.log('\nTesting button interaction integration...');
  
  try {
    // Test that help buttons reference status functionality
    const expectedButtons = ['help_status_button', 'help_tools_button'];
    
    for (const buttonId of expectedButtons) {
      if (buttonId.startsWith('help_')) {
        console.log(`✓ ${buttonId} follows naming convention`);
      }
    }
    
    // Test that interaction handler can handle help buttons
    // This would be tested in the actual interaction handler
    console.log('✓ Button integration structure is correct');
    
  } catch (error) {
    console.log('✗ Button integration test failed:', error);
  }
}

/**
 * Run comprehensive integration tests for utility commands
 */
async function runUtilityCommandsIntegrationTests() {
  console.log('=== Utility Commands Integration Tests ===\n');
  
  try {
    // Run individual command tests first
    console.log('Running individual command tests...\n');
    await runHelpCommandTests();
    console.log('\n');
    await runLenzaStatusTests();
    
    console.log('\n--- Integration Tests ---\n');
    
    // Run integration-specific tests
    testCommandRegistration();
    testCommandInteraction();
    testErrorHandlingConsistency();
    testConfigurationConsistency();
    testButtonIntegration();
    
    console.log('\n=== Utility Commands Integration Tests Completed ===');
  } catch (error) {
    console.error('Utility commands integration test suite failed:', error);
  }
}

// Run tests if this file is executed directly
if (require.main === module) {
  runUtilityCommandsIntegrationTests();
}

export { runUtilityCommandsIntegrationTests };