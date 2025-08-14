import { SlashCommandBuilder, ChatInputCommandInteraction, EmbedBuilder } from 'discord.js';
import { getBotConfig } from '../../config/bot-config';
import { healthMonitoringService } from '../../services/health-monitoring-service';
import { errorRecoveryService } from '../../services/error-recovery-service';
import { logger } from '../../utils/logger';

interface HealthStatus {
  status: string;
  service?: string;
  redis?: any;
  toolSync?: any;
}

module.exports = {
	data: new SlashCommandBuilder()
		.setName('lenza-status')
		.setDescription('Check Lenza AI system status and health')
		.addBooleanOption(option =>
			option.setName('detailed')
				.setDescription('Show detailed health information')
				.setRequired(false)
		),
	
	async execute(interaction: ChatInputCommandInteraction) {
		logger.userAction(
			'lenza-status',
			interaction.user.id,
			interaction.user.username,
			interaction.channel?.id,
			interaction.guild?.id
		);
		
		try {
			await interaction.deferReply();

			const config = getBotConfig();
			const detailed = interaction.options.getBoolean('detailed') || false;
			const uptime = process.uptime();
			const uptimeHours = Math.floor(uptime / 3600);
			const uptimeMinutes = Math.floor((uptime % 3600) / 60);
			const uptimeSeconds = Math.floor(uptime % 60);
			
			// Get health status
			const healthStatus = healthMonitoringService.getHealthStatus();
			const recoveryStats = errorRecoveryService.getRecoveryStats();
			
			// Determine overall status color
			const statusColor = this.getStatusColor(healthStatus.overall);
			const statusEmoji = this.getStatusEmoji(healthStatus.overall);
			
			const embed = new EmbedBuilder()
				.setColor(statusColor)
				.setTitle(`${statusEmoji} Lenza AI System Status`)
				.setDescription(healthMonitoringService.getHealthMessage())
				.addFields(
					{ 
						name: '⏱️ System Uptime', 
						value: `${uptimeHours}h ${uptimeMinutes}m ${uptimeSeconds}s`, 
						inline: true 
					},
					{ 
						name: '🔗 Backend Connection', 
						value: this.formatServiceStatus(healthStatus.services.backend), 
						inline: true 
					},
					{ 
						name: '🌐 Network Health', 
						value: this.formatServiceStatus(healthStatus.services.network), 
						inline: true 
					},
					{ 
						name: '💬 Discord API', 
						value: this.formatServiceStatus(healthStatus.services.discord), 
						inline: true 
					},
					{ 
						name: '🔄 Error Recovery', 
						value: `${recoveryStats.activeRecoveries} active\n${recoveryStats.totalAttempts} total attempts`, 
						inline: true 
					},
					{ 
						name: '⚙️ Configuration', 
						value: `Timeout: ${config.backendApiTimeout}ms\nRetries: ${config.retryAttempts}\nMax Length: ${config.maxMessageLength}`, 
						inline: true 
					}
				)
				.setTimestamp()
				.setFooter({ text: 'Lenza AI Discord Bot • System Health Check' });

			// Add detailed information if requested
			if (detailed) {
				const detailedReport = healthMonitoringService.getDetailedHealthReport();
				
				// Split detailed report if it's too long for a single field
				if (detailedReport.length > 1024) {
					const chunks = this.splitText(detailedReport, 1024);
					chunks.forEach((chunk: string, index: number) => {
						embed.addFields({
							name: index === 0 ? '📊 Detailed Health Report' : '📊 Detailed Health Report (continued)',
							value: `\`\`\`\n${chunk}\n\`\`\``,
							inline: false
						});
					});
				} else {
					embed.addFields({
						name: '📊 Detailed Health Report',
						value: `\`\`\`\n${detailedReport}\n\`\`\``,
						inline: false
					});
				}
			}

			// Add performance tips based on health status
			if (healthStatus.overall !== 'healthy') {
				embed.addFields({
					name: '💡 Performance Tips',
					value: this.getPerformanceTips(healthStatus),
					inline: false
				});
			}

			await interaction.editReply({ embeds: [embed] });
			
			logger.info('Lenza-status command: Execution completed successfully', {
				detailed,
				overallHealth: healthStatus.overall,
				userId: interaction.user.id
			});

		} catch (error) {
			logger.error('Lenza-status command: Error during execution', error as Error, {
				userId: interaction.user.id,
				detailed: interaction.options.getBoolean('detailed')
			});
			
			// Use error recovery service
			await errorRecoveryService.handleError(error as Error, interaction, {
				operation: 'lenza-status',
				userId: interaction.user.id,
				username: interaction.user.username,
				channelId: interaction.channel?.id,
				guildId: interaction.guild?.id
			});
		}
	},

	/**
	 * Get status color based on health status
	 */
	getStatusColor(status: string): number {
		switch (status) {
			case 'healthy': return 0x00FF00; // Green
			case 'degraded': return 0xFFFF00; // Yellow
			case 'unhealthy': return 0xFF8000; // Orange
			case 'critical': return 0xFF0000; // Red
			default: return 0x808080; // Gray
		}
	},

	/**
	 * Get status emoji based on health status
	 */
	getStatusEmoji(status: string): string {
		switch (status) {
			case 'healthy': return '🟢';
			case 'degraded': return '🟡';
			case 'unhealthy': return '🟠';
			case 'critical': return '🔴';
			default: return '❓';
		}
	},

	/**
	 * Format service status for display
	 */
	formatServiceStatus(service: any): string {
		const emoji = this.getStatusEmoji(service.status);
		const responseTime = service.responseTime ? ` (${service.responseTime}ms)` : '';
		const errorRate = service.errorRate ? `\nError Rate: ${service.errorRate.toFixed(1)}%` : '';
		
		return `${emoji} ${service.status.toUpperCase()}${responseTime}${errorRate}`;
	},

	/**
	 * Get performance tips based on health status
	 */
	getPerformanceTips(healthStatus: any): string {
		const tips = [];
		
		if (healthStatus.services.backend.status !== 'healthy') {
			tips.push('• Backend issues detected - responses may be slower');
		}
		
		if (healthStatus.services.network.status !== 'healthy') {
			tips.push('• Network connectivity issues - try again in a moment');
		}
		
		if (healthStatus.services.discord.status !== 'healthy') {
			tips.push('• Discord API issues - some features may be limited');
		}
		
		if (healthStatus.overall === 'critical') {
			tips.push('• System experiencing critical issues - contact support if problems persist');
		}
		
		return tips.length > 0 ? tips.join('\n') : 'System is operating normally';
	},

	/**
	 * Split text into chunks that fit Discord field limits
	 */
	splitText(text: string, maxLength: number): string[] {
		const chunks = [];
		let currentChunk = '';
		
		const lines = text.split('\n');
		
		for (const line of lines) {
			if (currentChunk.length + line.length + 1 <= maxLength) {
				currentChunk += (currentChunk ? '\n' : '') + line;
			} else {
				if (currentChunk) {
					chunks.push(currentChunk);
				}
				currentChunk = line;
			}
		}
		
		if (currentChunk) {
			chunks.push(currentChunk);
		}
		
		return chunks;
	}
};