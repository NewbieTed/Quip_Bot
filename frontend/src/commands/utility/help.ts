import { SlashCommandBuilder, ChatInputCommandInteraction, EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle } from 'discord.js';
import { logger } from '../../utils/logger';

module.exports = {
	data: new SlashCommandBuilder()
		.setName('help')
		.setDescription('Show help documentation and command examples')
		.addStringOption(option =>
			option.setName('command')
				.setDescription('Get detailed help for a specific command')
				.setRequired(false)
				.addChoices(
					{ name: 'lenza-new', value: 'lenza-new' },
					{ name: 'lenza-resume', value: 'lenza-resume' },
					{ name: 'lenza-tools', value: 'lenza-tools' },
					{ name: 'lenza-status', value: 'lenza-status' },
					{ name: 'ping', value: 'ping' },
					{ name: 'urban-dictionary', value: 'urban-dictionary' }
				)
		),
	
	async execute(interaction: ChatInputCommandInteraction) {
		logger.info(`Help command: Starting execution for user ${interaction.user.username}`);
		
		try {
			// Defer reply immediately to avoid timeout
			await interaction.deferReply();
			logger.info('Help command: deferReply successful');

			const specificCommand = interaction.options.getString('command');

			if (specificCommand) {
				// Show detailed help for specific command
				await showCommandHelp(interaction, specificCommand);
			} else {
				// Show general help overview
				await showGeneralHelp(interaction);
			}

			logger.info('Help command: Execution completed successfully');

		} catch (error) {
			logger.error('Help command: Error during execution:', error);
			
			try {
				const errorMessage = '❌ Failed to display help information. Please try again later.';
				
				if (interaction.replied || interaction.deferred) {
					await interaction.editReply(errorMessage);
				} else {
					await interaction.reply(errorMessage);
				}
			} catch (replyError) {
				logger.error('Help command: Failed to send error message to user:', replyError);
			}
		}
		
		logger.info('Help command: Execution finished');
	},
};

async function showGeneralHelp(interaction: ChatInputCommandInteraction) {
	const helpEmbed = new EmbedBuilder()
		.setTitle('🤖 Lenza Bot - Command Help')
		.setDescription('Welcome to Lenza Bot! Here are all available commands organized by category.')
		.setColor(0x00AE86)
		.setTimestamp();

	// Lenza AI Commands
	helpEmbed.addFields({
		name: '🧠 Lenza AI Commands',
		value: [
			'`/lenza-new <message>` - Start a fresh conversation with Lenza AI',
			'`/lenza-resume <message>` - Continue your existing conversation with Lenza',
			'`/lenza-tools [action] [tool]` - Manage your tool whitelist preferences',
			'`/lenza-status` - Show bot health and backend connectivity status'
		].join('\n'),
		inline: false
	});

	// Utility Commands
	helpEmbed.addFields({
		name: '🛠️ Utility Commands',
		value: [
			'`/ping` - Check bot response time and latency',
			'`/urban-dictionary <term>` - Look up definitions from Urban Dictionary',
			'`/help [command]` - Show this help or get detailed help for a specific command'
		].join('\n'),
		inline: false
	});

	// Quick Start Guide
	helpEmbed.addFields({
		name: '🚀 Quick Start Guide',
		value: [
			'1. **New Conversation**: Use `/lenza-new` to start fresh with Lenza AI',
			'2. **Continue Chatting**: Use `/lenza-resume` to continue your conversation',
			'3. **Tool Management**: Use `/lenza-tools` to manage AI tool permissions',
			'4. **Get Help**: Use `/help <command>` for detailed command information'
		].join('\n'),
		inline: false
	});

	// Important Notes
	helpEmbed.addFields({
		name: '⚠️ Important Notes',
		value: [
			'• Lenza may request permission to use tools - you can approve, deny, or trust them',
			'• Conversations are maintained per channel - each channel has its own context',
			'• Use `/lenza-new` to reset your conversation context in a channel',
			'• Tool approvals timeout after 60 seconds and are automatically denied'
		].join('\n'),
		inline: false
	});

	// Add footer with additional help
	helpEmbed.setFooter({ 
		text: 'Use /help <command> for detailed examples • Developed for Lenza AI integration' 
	});

	// Create action row with helpful buttons
	const actionRow = new ActionRowBuilder<ButtonBuilder>()
		.addComponents(
			new ButtonBuilder()
				.setLabel('Bot Status')
				.setStyle(ButtonStyle.Secondary)
				.setEmoji('📊')
				.setCustomId('help_status_button'),
			new ButtonBuilder()
				.setLabel('Tool Management')
				.setStyle(ButtonStyle.Secondary)
				.setEmoji('🔧')
				.setCustomId('help_tools_button')
		);

	await interaction.editReply({ 
		embeds: [helpEmbed],
		components: [actionRow]
	});
}

async function showCommandHelp(interaction: ChatInputCommandInteraction, command: string) {
	let helpEmbed: EmbedBuilder;

	switch (command) {
		case 'lenza-new':
			helpEmbed = new EmbedBuilder()
				.setTitle('🆕 /lenza-new Command Help')
				.setDescription('Start a fresh conversation with Lenza AI, clearing any previous context.')
				.setColor(0x00AE86)
				.addFields(
					{
						name: '📝 Usage',
						value: '`/lenza-new <message>`',
						inline: false
					},
					{
						name: '📋 Parameters',
						value: '• **message** (required) - Your message to Lenza AI',
						inline: false
					},
					{
						name: '💡 Examples',
						value: [
							'`/lenza-new Hello, can you help me with Python programming?`',
							'`/lenza-new What\'s the weather like today?`',
							'`/lenza-new Can you explain quantum computing in simple terms?`'
						].join('\n'),
						inline: false
					},
					{
						name: '⚠️ Notes',
						value: [
							'• This creates a completely new conversation context',
							'• Any previous conversation history in this channel will be ignored',
							'• Lenza may request tool approvals during the conversation',
							'• Messages are limited to 2000 characters'
						].join('\n'),
						inline: false
					}
				);
			break;

		case 'lenza-resume':
			helpEmbed = new EmbedBuilder()
				.setTitle('🔄 /lenza-resume Command Help')
				.setDescription('Continue your existing conversation with Lenza AI, maintaining context.')
				.setColor(0x00AE86)
				.addFields(
					{
						name: '📝 Usage',
						value: '`/lenza-resume <message>`',
						inline: false
					},
					{
						name: '📋 Parameters',
						value: '• **message** (required) - Your follow-up message to Lenza AI',
						inline: false
					},
					{
						name: '💡 Examples',
						value: [
							'`/lenza-resume Can you elaborate on that last point?`',
							'`/lenza-resume What about the alternative approach?`',
							'`/lenza-resume Thanks! Can you help me with something else?`'
						].join('\n'),
						inline: false
					},
					{
						name: '⚠️ Notes',
						value: [
							'• Maintains conversation history and context from previous messages',
							'• If no previous conversation exists, it will start a new one',
							'• Each channel maintains its own conversation context',
							'• Use `/lenza-new` if you want to start fresh'
						].join('\n'),
						inline: false
					}
				);
			break;

		case 'lenza-tools':
			helpEmbed = new EmbedBuilder()
				.setTitle('🔧 /lenza-tools Command Help')
				.setDescription('Manage your tool whitelist to control which tools Lenza can use automatically.')
				.setColor(0x00AE86)
				.addFields(
					{
						name: '📝 Usage',
						value: [
							'`/lenza-tools` - View your current tool whitelist',
							'`/lenza-tools add <tool>` - Add a tool to your whitelist',
							'`/lenza-tools remove <tool>` - Remove a tool from your whitelist',
							'`/lenza-tools clear` - Clear your entire whitelist'
						].join('\n'),
						inline: false
					},
					{
						name: '💡 Examples',
						value: [
							'`/lenza-tools` - Show current whitelisted tools',
							'`/lenza-tools add weather` - Allow weather tool automatically',
							'`/lenza-tools remove calculator` - Remove calculator from whitelist',
							'`/lenza-tools clear` - Remove all whitelisted tools'
						].join('\n'),
						inline: false
					},
					{
						name: '⚠️ Notes',
						value: [
							'• Whitelisted tools are approved automatically without prompts',
							'• You can also use "Approve & Trust" buttons during conversations',
							'• Tool names are case-sensitive',
							'• Clearing whitelist requires manual approval for all tools'
						].join('\n'),
						inline: false
					}
				);
			break;

		case 'lenza-status':
			helpEmbed = new EmbedBuilder()
				.setTitle('📊 /lenza-status Command Help')
				.setDescription('Display bot health information and backend connectivity status.')
				.setColor(0x00AE86)
				.addFields(
					{
						name: '📝 Usage',
						value: '`/lenza-status`',
						inline: false
					},
					{
						name: '📋 Information Displayed',
						value: [
							'• Bot online status and configuration',
							'• Backend API connectivity and response time',
							'• Redis cache status (if enabled)',
							'• Tool synchronization health',
							'• Your user and channel information'
						].join('\n'),
						inline: false
					},
					{
						name: '💡 Use Cases',
						value: [
							'• Check if Lenza services are working properly',
							'• Troubleshoot connection issues',
							'• View current bot configuration',
							'• Get technical support information'
						].join('\n'),
						inline: false
					}
				);
			break;

		case 'ping':
			helpEmbed = new EmbedBuilder()
				.setTitle('🏓 /ping Command Help')
				.setDescription('Test bot responsiveness and measure latency.')
				.setColor(0x00AE86)
				.addFields(
					{
						name: '📝 Usage',
						value: '`/ping`',
						inline: false
					},
					{
						name: '📋 Information Displayed',
						value: [
							'• Bot response time',
							'• WebSocket heartbeat latency',
							'• Simple connectivity test'
						].join('\n'),
						inline: false
					}
				);
			break;

		case 'urban-dictionary':
			helpEmbed = new EmbedBuilder()
				.setTitle('📚 /urban-dictionary Command Help')
				.setDescription('Look up definitions and meanings from Urban Dictionary.')
				.setColor(0x00AE86)
				.addFields(
					{
						name: '📝 Usage',
						value: '`/urban-dictionary <term>`',
						inline: false
					},
					{
						name: '📋 Parameters',
						value: '• **term** (required) - The word or phrase to look up',
						inline: false
					},
					{
						name: '💡 Examples',
						value: [
							'`/urban-dictionary yeet`',
							'`/urban-dictionary sus`',
							'`/urban-dictionary based`'
						].join('\n'),
						inline: false
					},
					{
						name: '⚠️ Content Warning',
						value: 'Urban Dictionary may contain explicit or inappropriate content. Use discretion in public channels.',
						inline: false
					}
				);
			break;

		default:
			helpEmbed = new EmbedBuilder()
				.setTitle('❓ Command Not Found')
				.setDescription(`Sorry, I don't have detailed help for the command "${command}".`)
				.setColor(0xFF6B6B)
				.addFields({
					name: '💡 Suggestion',
					value: 'Use `/help` without parameters to see all available commands.',
					inline: false
				});
			break;
	}

	helpEmbed.setTimestamp();
	helpEmbed.setFooter({ 
		text: 'Use /help for general command overview • Developed for Lenza AI integration' 
	});

	await interaction.editReply({ embeds: [helpEmbed] });
}