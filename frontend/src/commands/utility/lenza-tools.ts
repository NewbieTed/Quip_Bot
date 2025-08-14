import { SlashCommandBuilder, ChatInputCommandInteraction, EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle } from 'discord.js';
import { BackendApiClient } from '../../services/backend-api-client';
import { ConversationManager } from '../../services/conversation-manager';
import { AgentError, ERROR_MESSAGES } from '../../errors/agent-error';
import { ToolWhitelistRequest } from '../../models/agent-models';
import { logger } from '../../utils/logger';

module.exports = {
	data: new SlashCommandBuilder()
		.setName('lenza-tools')
		.setDescription('Manage your tool whitelist for Lenza AI')
		.addSubcommand(subcommand =>
			subcommand
				.setName('view')
				.setDescription('View your current tool whitelist')
		)
		.addSubcommand(subcommand =>
			subcommand
				.setName('add')
				.setDescription('Add a tool to your whitelist')
				.addStringOption(option =>
					option.setName('tool')
						.setDescription('Name of the tool to add')
						.setRequired(true)
				)
				.addStringOption(option =>
					option.setName('scope')
						.setDescription('Scope of the whitelist entry')
						.setRequired(false)
						.addChoices(
							{ name: 'Global (all servers)', value: 'global' },
							{ name: 'Server (this server only)', value: 'server' },
							{ name: 'Conversation (current conversation only)', value: 'conversation' }
						)
				)
		)
		.addSubcommand(subcommand =>
			subcommand
				.setName('remove')
				.setDescription('Remove a tool from your whitelist')
				.addStringOption(option =>
					option.setName('tool')
						.setDescription('Name of the tool to remove')
						.setRequired(true)
				)
				.addStringOption(option =>
					option.setName('scope')
						.setDescription('Scope of the whitelist entry to remove')
						.setRequired(false)
						.addChoices(
							{ name: 'Global', value: 'global' },
							{ name: 'Server', value: 'server' },
							{ name: 'Conversation', value: 'conversation' }
						)
				)
		)
		.addSubcommand(subcommand =>
			subcommand
				.setName('clear')
				.setDescription('Clear all tools from your whitelist')
				.addStringOption(option =>
					option.setName('scope')
						.setDescription('Scope to clear (leave empty to clear all)')
						.setRequired(false)
						.addChoices(
							{ name: 'Global only', value: 'global' },
							{ name: 'Server only', value: 'server' },
							{ name: 'Conversation only', value: 'conversation' }
						)
				)
		),
	
	async execute(interaction: ChatInputCommandInteraction) {
		logger.info(`Lenza-tools command: Starting execution for user ${interaction.user.username} in channel ${interaction.channel?.id}`);
		
		try {
			// Defer reply immediately to avoid timeout
			await interaction.deferReply();
			logger.info('Lenza-tools command: deferReply successful');

			const subcommand = interaction.options.getSubcommand();
			
			// Initialize services
			const conversationManager = new ConversationManager();
			const apiClient = new BackendApiClient();

			// Extract conversation context from Discord interaction
			const context = conversationManager.extractConversationContext(interaction);
			logger.info(`Extracted context: serverId=${context.serverId}, channelId=${context.channelId}, memberId=${context.memberId}`);

			// Validate the context
			conversationManager.validateConversationContext(context);

			switch (subcommand) {
				case 'view':
					await handleViewWhitelist(interaction, apiClient, context);
					break;
				case 'add':
					await handleAddTool(interaction, apiClient, context);
					break;
				case 'remove':
					await handleRemoveTool(interaction, apiClient, context);
					break;
				case 'clear':
					await handleClearWhitelist(interaction, apiClient, context);
					break;
				default:
					await interaction.editReply('❌ Unknown subcommand. Please use `/lenza-tools view`, `/lenza-tools add`, `/lenza-tools remove`, or `/lenza-tools clear`.');
			}
			
			logger.info('Lenza-tools command: Execution completed successfully');

		} catch (error) {
			logger.error('Lenza-tools command: Error during execution:', error);
			
			try {
				let errorMessage: string = ERROR_MESSAGES.UNKNOWN_ERROR;
				
				if (error instanceof AgentError) {
					switch (error.type) {
						case 'network':
							errorMessage = ERROR_MESSAGES.AGENT_UNAVAILABLE;
							break;
						case 'timeout':
							errorMessage = ERROR_MESSAGES.NETWORK_TIMEOUT;
							break;
						case 'parsing':
							errorMessage = ERROR_MESSAGES.PARSING_ERROR;
							break;
						case 'api':
							errorMessage = error.message.includes('rate limit') 
								? ERROR_MESSAGES.RATE_LIMITED 
								: ERROR_MESSAGES.AGENT_UNAVAILABLE;
							break;
						default:
							errorMessage = ERROR_MESSAGES.UNKNOWN_ERROR;
					}
				} else if (error instanceof Error) {
					// Handle validation errors and other known errors
					if (error.message.includes('guild') || error.message.includes('channel')) {
						errorMessage = '❌ This command can only be used in a server channel.';
					} else if (error.message.includes('tool') || error.message.includes('whitelist')) {
						errorMessage = `❌ ${error.message}`;
					} else {
						errorMessage = ERROR_MESSAGES.UNKNOWN_ERROR;
					}
				}

				// Try to send error message to user
				if (interaction.replied || interaction.deferred) {
					await interaction.editReply(`❌ ${errorMessage}`);
				} else {
					await interaction.reply(`❌ ${errorMessage}`);
				}
			} catch (replyError) {
				logger.error('Lenza-tools command: Failed to send error message to user:', replyError);
			}
		}
		
		logger.info('Lenza-tools command: Execution finished');
	},
};

/**
 * Handle viewing the current tool whitelist
 */
async function handleViewWhitelist(
	interaction: ChatInputCommandInteraction, 
	apiClient: BackendApiClient, 
	context: any
) {
	try {
		// For now, we'll show a message that this feature requires backend support
		// In a real implementation, we'd call a GET endpoint to retrieve the whitelist
		const embed = new EmbedBuilder()
			.setColor(0x0099FF)
			.setTitle('🔧 Your Tool Whitelist')
			.setDescription('This feature is coming soon! Currently, you can view your whitelisted tools when they are requested for approval during conversations.')
			.addFields(
				{ name: 'Available Actions', value: '• Use `/lenza-tools add <tool>` to add tools\n• Use `/lenza-tools remove <tool>` to remove tools\n• Use `/lenza-tools clear` to clear all tools' },
				{ name: 'Scopes', value: '• **Global**: Available across all servers\n• **Server**: Available only in this server\n• **Conversation**: Available only in specific conversations' }
			)
			.setFooter({ text: 'Tool whitelist helps Lenza know which tools you trust for automatic approval.' });

		await interaction.editReply({ embeds: [embed] });
	} catch (error) {
		logger.error('Error viewing whitelist:', error);
		throw new AgentError('api', 'Failed to retrieve tool whitelist');
	}
}

/**
 * Handle adding a tool to the whitelist
 */
async function handleAddTool(
	interaction: ChatInputCommandInteraction, 
	apiClient: BackendApiClient, 
	context: any
) {
	const toolName = interaction.options.getString('tool', true);
	const scope = interaction.options.getString('scope') || 'server'; // Default to server scope
	
	try {
		// Validate tool name
		if (!toolName || toolName.trim().length === 0) {
			throw new Error('Tool name cannot be empty');
		}

		if (toolName.length > 100) {
			throw new Error('Tool name is too long. Please keep it under 100 characters.');
		}

		// Prepare the whitelist update request
		const updateRequest = {
			memberId: parseInt(context.memberId),
			channelId: parseInt(context.channelId),
			addRequests: [{
				toolName: toolName.trim(),
				scope: scope.toUpperCase(),
				agentConversationId: scope === 'conversation' ? null : null, // TODO: Get actual conversation ID if needed
				expiresAt: null // Permanent approval
			}],
			removeRequests: []
		};

		// Send update request to backend
		const response = await apiClient.updateToolWhitelist(updateRequest);
		
		if (response.success) {
			const embed = new EmbedBuilder()
				.setColor(0x00FF00)
				.setTitle('✅ Tool Added to Whitelist')
				.setDescription(`Successfully added **${toolName}** to your whitelist with **${scope}** scope.`)
				.addFields(
					{ name: 'Tool Name', value: toolName, inline: true },
					{ name: 'Scope', value: scope.charAt(0).toUpperCase() + scope.slice(1), inline: true },
					{ name: 'Status', value: 'Active', inline: true }
				)
				.setFooter({ text: 'This tool will now be automatically approved in future requests.' });

			await interaction.editReply({ embeds: [embed] });
		} else {
			throw new AgentError('api', response.message || 'Failed to add tool to whitelist');
		}
	} catch (error) {
		logger.error('Error adding tool to whitelist:', error);
		if (error instanceof AgentError) {
			throw error;
		}
		throw new AgentError('api', `Failed to add tool "${toolName}" to whitelist: ${error instanceof Error ? error.message : 'Unknown error'}`);
	}
}

/**
 * Handle removing a tool from the whitelist
 */
async function handleRemoveTool(
	interaction: ChatInputCommandInteraction, 
	apiClient: BackendApiClient, 
	context: any
) {
	const toolName = interaction.options.getString('tool', true);
	const scope = interaction.options.getString('scope') || 'server'; // Default to server scope
	
	try {
		// Validate tool name
		if (!toolName || toolName.trim().length === 0) {
			throw new Error('Tool name cannot be empty');
		}

		// Prepare the whitelist update request
		const updateRequest = {
			memberId: parseInt(context.memberId),
			channelId: parseInt(context.channelId),
			addRequests: [],
			removeRequests: [{
				toolName: toolName.trim(),
				scope: scope.toUpperCase(),
				agentConversationId: scope === 'conversation' ? null : null // TODO: Get actual conversation ID if needed
			}]
		};

		// Send update request to backend
		const response = await apiClient.updateToolWhitelist(updateRequest);
		
		if (response.success) {
			const embed = new EmbedBuilder()
				.setColor(0xFF9900)
				.setTitle('🗑️ Tool Removed from Whitelist')
				.setDescription(`Successfully removed **${toolName}** from your whitelist with **${scope}** scope.`)
				.addFields(
					{ name: 'Tool Name', value: toolName, inline: true },
					{ name: 'Scope', value: scope.charAt(0).toUpperCase() + scope.slice(1), inline: true },
					{ name: 'Status', value: 'Removed', inline: true }
				)
				.setFooter({ text: 'This tool will now require manual approval in future requests.' });

			await interaction.editReply({ embeds: [embed] });
		} else {
			throw new AgentError('api', response.message || 'Failed to remove tool from whitelist');
		}
	} catch (error) {
		logger.error('Error removing tool from whitelist:', error);
		if (error instanceof AgentError) {
			throw error;
		}
		throw new AgentError('api', `Failed to remove tool "${toolName}" from whitelist: ${error instanceof Error ? error.message : 'Unknown error'}`);
	}
}

/**
 * Handle clearing the whitelist
 */
async function handleClearWhitelist(
	interaction: ChatInputCommandInteraction, 
	apiClient: BackendApiClient, 
	context: any
) {
	const scope = interaction.options.getString('scope');
	
	try {
		// Create confirmation embed
		const embed = new EmbedBuilder()
			.setColor(0xFF0000)
			.setTitle('⚠️ Clear Tool Whitelist')
			.setDescription(scope 
				? `Are you sure you want to clear all **${scope}** scoped tools from your whitelist?`
				: 'Are you sure you want to clear **ALL** tools from your whitelist?'
			)
			.addFields(
				{ name: 'This action will:', value: scope 
					? `• Remove all ${scope} scoped whitelisted tools\n• Require manual approval for those tools in future requests`
					: '• Remove ALL whitelisted tools from all scopes\n• Require manual approval for all tools in future requests'
				},
				{ name: 'This action cannot be undone', value: 'You will need to re-add tools individually after clearing.' }
			)
			.setFooter({ text: 'Click Confirm to proceed or Cancel to abort.' });

		// Create confirmation buttons
		const confirmButton = new ButtonBuilder()
			.setCustomId(`clear_whitelist_confirm_${scope || 'all'}`)
			.setLabel('Confirm Clear')
			.setStyle(ButtonStyle.Danger);

		const cancelButton = new ButtonBuilder()
			.setCustomId('clear_whitelist_cancel')
			.setLabel('Cancel')
			.setStyle(ButtonStyle.Secondary);

		const row = new ActionRowBuilder<ButtonBuilder>()
			.addComponents(confirmButton, cancelButton);

		const response = await interaction.editReply({ 
			embeds: [embed], 
			components: [row] 
		});

		// Wait for button interaction
		try {
			const buttonInteraction = await response.awaitMessageComponent({ 
				time: 30000 // 30 second timeout
			});

			if (buttonInteraction.customId === 'clear_whitelist_cancel') {
				const cancelEmbed = new EmbedBuilder()
					.setColor(0x808080)
					.setTitle('❌ Operation Cancelled')
					.setDescription('Tool whitelist clear operation has been cancelled.')
					.setFooter({ text: 'Your whitelist remains unchanged.' });

				await buttonInteraction.update({ 
					embeds: [cancelEmbed], 
					components: [] 
				});
				return;
			}

			if (buttonInteraction.customId.startsWith('clear_whitelist_confirm_')) {
				// Extract scope from button ID
				const confirmScope = buttonInteraction.customId.replace('clear_whitelist_confirm_', '');
				const actualScope = confirmScope === 'all' ? null : confirmScope;

				// For now, show that this feature needs backend implementation
				// In a real implementation, we'd need a way to get current whitelist and remove all items
				const notImplementedEmbed = new EmbedBuilder()
					.setColor(0xFF9900)
					.setTitle('🚧 Feature Coming Soon')
					.setDescription('The clear whitelist feature requires additional backend support and will be available in a future update.')
					.addFields(
						{ name: 'Alternative', value: 'You can remove tools individually using `/lenza-tools remove <tool>`' },
						{ name: 'Scope', value: actualScope ? `Would have cleared: ${actualScope} scoped tools` : 'Would have cleared: All tools' }
					)
					.setFooter({ text: 'Thank you for your patience as we continue to improve Lenza!' });

				await buttonInteraction.update({ 
					embeds: [notImplementedEmbed], 
					components: [] 
				});
			}
		} catch (timeoutError) {
			// Handle timeout
			const timeoutEmbed = new EmbedBuilder()
				.setColor(0x808080)
				.setTitle('⏰ Operation Timed Out')
				.setDescription('The clear whitelist operation has timed out.')
				.setFooter({ text: 'Your whitelist remains unchanged. Please try again if needed.' });

			await interaction.editReply({ 
				embeds: [timeoutEmbed], 
				components: [] 
			});
		}
	} catch (error) {
		logger.error('Error clearing whitelist:', error);
		if (error instanceof AgentError) {
			throw error;
		}
		throw new AgentError('api', `Failed to clear whitelist: ${error instanceof Error ? error.message : 'Unknown error'}`);
	}
}