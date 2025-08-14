import { SlashCommandBuilder, ChatInputCommandInteraction } from 'discord.js';
import { BackendApiClient } from '../../services/backend-api-client';
import { StreamingResponseHandler } from '../../services/streaming-response-handler';
import { ConversationManager } from '../../services/conversation-manager';
import { AgentError, ERROR_MESSAGES, getUserFriendlyErrorMessage } from '../../errors/agent-error';
import { errorRecoveryService } from '../../services/error-recovery-service';
import { logger } from '../../utils/logger';

module.exports = {
	data: new SlashCommandBuilder()
		.setName('lenza-new')
		.setDescription('Start a fresh conversation with Lenza AI')
		.addStringOption(option =>
			option.setName('message')
				.setDescription('Your message to Lenza')
				.setRequired(true)
		),
	
	async execute(interaction: ChatInputCommandInteraction) {
		logger.info(`Lenza-new command: Starting execution for user ${interaction.user.username} in channel ${interaction.channel?.id}`);
		
		try {
			// Defer reply immediately to avoid timeout
			await interaction.deferReply();
			logger.info('Lenza-new command: deferReply successful');

			// Get the message from the command options
			const message = interaction.options.getString('message', true);
			
			// Validate message
			if (!message || message.trim().length === 0) {
				await interaction.editReply('❌ Message cannot be empty. Please provide a message for Lenza.');
				return;
			}

			if (message.length > 2000) {
				await interaction.editReply('❌ Message is too long. Please keep your message under 2000 characters.');
				return;
			}

			// Initialize services
			const conversationManager = new ConversationManager();
			const apiClient = new BackendApiClient();
			const streamHandler = new StreamingResponseHandler(interaction);

			// Extract conversation context from Discord interaction
			const context = conversationManager.extractConversationContext(interaction);
			logger.info(`Extracted context: serverId=${context.serverId}, channelId=${context.channelId}, memberId=${context.memberId}`);

			// Validate the context
			conversationManager.validateConversationContext(context);
			conversationManager.validateMessage(message);

			// Build new conversation payload
			const payload = conversationManager.buildNewConversationPayload(context, message);
			logger.info(`Built new conversation payload for user ${context.username}`);

			// Start new conversation with backend API
			logger.info('Invoking new conversation with backend API');
			const responseStream = apiClient.invokeNewConversation(payload);

			// Handle streaming response
			await streamHandler.handleStream(responseStream);
			
			logger.info('Lenza-new command: Execution completed successfully');

		} catch (error) {
			// Use the error recovery service for comprehensive error handling
			await errorRecoveryService.handleError(error as Error, interaction, {
				operation: 'lenza-new',
				userId: interaction.user.id,
				username: interaction.user.username,
				channelId: interaction.channel?.id,
				guildId: interaction.guild?.id,
				messageLength: message?.length || 0
			});
		}
		
		logger.info('Lenza-new command: Execution finished');
	},
};