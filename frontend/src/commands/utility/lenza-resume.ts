import { SlashCommandBuilder, ChatInputCommandInteraction } from 'discord.js';
import { BackendApiClient } from '../../services/backend-api-client';
import { StreamingResponseHandler } from '../../services/streaming-response-handler';
import { ConversationManager } from '../../services/conversation-manager';
import { AgentError, ERROR_MESSAGES, getUserFriendlyErrorMessage } from '../../errors/agent-error';
import { errorRecoveryService } from '../../services/error-recovery-service';
import { logger } from '../../utils/logger';

module.exports = {
	data: new SlashCommandBuilder()
		.setName('lenza-resume')
		.setDescription('Continue an existing conversation with Lenza AI')
		.addStringOption(option =>
			option.setName('message')
				.setDescription('Your message to continue the conversation (optional)')
				.setRequired(false)
		),
	
	async execute(interaction: ChatInputCommandInteraction) {
		logger.info(`Lenza-resume command: Starting execution for user ${interaction.user.username} in channel ${interaction.channel?.id}`);
		
		try {
			// Defer reply immediately to avoid timeout
			await interaction.deferReply();
			logger.info('Lenza-resume command: deferReply successful');

			// Get the optional message from the command options
			const message = interaction.options.getString('message', false);
			
			// Validate message if provided
			if (message !== null) {
				if (message.trim().length === 0) {
					await interaction.editReply('❌ Message cannot be empty. Please provide a message or omit the message parameter to continue without a new message.');
					return;
				}

				if (message.length > 2000) {
					await interaction.editReply('❌ Message is too long. Please keep your message under 2000 characters.');
					return;
				}
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

			// Validate message if provided
			if (message !== null) {
				conversationManager.validateMessage(message);
			}

			// Build resume conversation payload
			const payload = conversationManager.buildResumeConversationPayload(
				context,
				message || undefined // Convert null to undefined
			);
			logger.info(`Built resume conversation payload for user ${context.username}, hasMessage=${payload.message !== undefined}`);

			// Resume conversation with backend API
			logger.info('Resuming conversation with backend API');
			const responseStream = apiClient.resumeConversation(payload);

			// Handle streaming response
			await streamHandler.handleStream(responseStream);
			
			logger.info('Lenza-resume command: Execution completed successfully');

		} catch (error) {
			// Get the optional message from the command options
			const message = interaction.options.getString('message', false);
			
			// Use the error recovery service for comprehensive error handling
			await errorRecoveryService.handleError(error as Error, interaction, {
				operation: 'lenza-resume',
				userId: interaction.user.id,
				username: interaction.user.username,
				channelId: interaction.channel?.id,
				guildId: interaction.guild?.id,
				hasMessage: message !== null,
				messageLength: message?.length || 0
			});
		}
		
		logger.info('Lenza-resume command: Execution finished');
	},
};