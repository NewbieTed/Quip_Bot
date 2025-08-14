import { Events, ActionRowBuilder, ButtonBuilder, ButtonStyle, ChannelType, Interaction, Collection, MessageComponentInteraction } from 'discord.js';
import fetch from 'node-fetch';
import { verifiedRoleId, rulesChannelId } from '../../config.json';
import { redis } from '../redis';
import { ToolApprovalHandler } from '../services/tool-approval-handler';

// Initialize tool approval handler
const toolApprovalHandler = new ToolApprovalHandler();

module.exports = {
    name: Events.InteractionCreate,
    async execute(interaction: Interaction) {
        // Handle slash commands
        if (interaction.isChatInputCommand()) {
            const command = interaction.client.commands.get(interaction.commandName);
            if (!command) {
                console.error(`No command matching ${interaction.commandName} was found.`);
                return;
            }
            try {
                await command.execute(interaction);
            } catch (error) {
                console.error('Error executing command:', error);
            }
            return;
        }

        // Only proceed if interaction is a button click
        if (!interaction.isButton()) return;

        // Handle tool approval buttons
        if (interaction.customId.startsWith('tool_approval_')) {
            try {
                await toolApprovalHandler.handleApprovalResponse(interaction);
            } catch (error) {
                console.error('Error handling tool approval:', error);
            }
            return;
        }

        // Handle help command buttons
        if (interaction.customId.startsWith('help_')) {
            try {
                await handleHelpButtons(interaction);
            } catch (error) {
                console.error('Error handling help button:', error);
            }
            return;
        }

        const userId = interaction.user.id;

        // Handle verification thread creation button
        if (interaction.customId.startsWith('create_thread_')) {
            const targetUserId = interaction.customId.split('_')[2];
            // Ensure the user clicks their own verification button
            if (userId !== targetUserId) {
                await interaction.reply({ content: 'You can only click your own verification button.', ephemeral: true });
                return;
            }

            const punishmentKey = `punished:${userId}`;
            try {
                // Check if user is currently punished (suspended from answering)
                const isPunished = await redis.exists(punishmentKey);
                if (isPunished) {
                    const remainingTime = await redis.ttl(punishmentKey);
                    await interaction.reply({ content: `You are suspended from answering for ${Math.ceil(remainingTime / 60)} minutes.`, ephemeral: true });
                    return;
                }
            } catch (err) {
                console.error('Redis error checking punishment:', err);
                await interaction.reply({ content: 'Internal error occurred. Please try again later.', ephemeral: true });
                return;
            }

            // Defer reply to allow time for processing
            await interaction.deferReply({ ephemeral: true });

            const guild = interaction.guild;
            if (!guild) {
                console.error("Guild not found.");
                await interaction.editReply({ content: 'Guild not found. Cannot proceed with verification.' });
                return;
            }

            const channel = interaction.channel;
            if (!channel || channel.type !== ChannelType.GuildText) {
                console.error("Channel not found or is not a GuildText channel.");
                await interaction.editReply({ content: 'Invalid channel. Cannot proceed with verification.' });
                return;
            }

            let member;
            try {
                // Fetch the guild member to verify user exists in the guild
                member = await guild.members.fetch(userId);
            } catch (err) {
                console.error(`Failed to fetch member ${userId}:`, err);
                await interaction.editReply({ content: 'Failed to fetch your member information.' });
                return;
            }

            let thread;
            try {
                // Create a private thread for the user verification process
                thread = await channel.threads.create({
                    name: `verification-${member.user.username}`,
                    autoArchiveDuration: 60,
                    type: ChannelType.PrivateThread,
                    reason: 'User verification',
                });
                console.log(`Created verification thread ${thread.id} for user ${userId}`);
                // Add the user to the thread members
                await thread.members.add(userId);
            } catch (err) {
                console.error('Failed to create or add member to thread:', err);
                await interaction.editReply({ content: 'Failed to create verification thread.' });
                return;
            }

            let problemData;
            try {
                // Fetch a verification problem/question from external service
                const response = await fetch('http://localhost:8080/problem');
                problemData = await response.json() as {
                    status: boolean;
                    data: {
                        problemId: string;
                        question: string;
                        choices: string[];
                        image: string;
                        correctAnswer: string;
                    };
                };
            } catch (err) {
                console.error('Failed to fetch problem data:', err);
                await thread.send('Failed to fetch the problem data.');
                return;
            }

            if (!problemData.status) {
                // Handle failure in fetching problem data gracefully
                await thread.send('Failed to fetch the problem data.');
                return;
            }

            const { problemId, question, choices, image, correctAnswer } = problemData.data;

            try {
                // Store the correct answer in Redis for later verification
                await redis.set(`problem:${problemId}`, JSON.stringify({ correctAnswer }));
            } catch (err) {
                console.error('Failed to set problem data in Redis:', err);
                await thread.send('Internal error occurred. Please try again later.');
                return;
            }

            // Compose and send the verification question (with optional image)
            let content = `**Question:** ${question}`;
            if (image) content += `\n${image}`;

            await thread.send({ content });

            // Create buttons for each answer choice
            const choiceRows = choices.map(choice =>
                new ActionRowBuilder<ButtonBuilder>().addComponents(
                    new ButtonBuilder()
                        .setCustomId(`answer_${problemId}_${choice}`)
                        .setLabel(choice)
                        .setStyle(ButtonStyle.Secondary)
                )
            );

            // Send message with answer choices as buttons
            const message = await thread.send({ content: 'Choose your answer:', components: choiceRows });

            // Set up a collector to handle button interactions for answers within 60 seconds
            const collector = message.createMessageComponentCollector({ time: 60000 });

            collector.on('end', async (collected: Collection<string, MessageComponentInteraction>) => {
                // If no answer was selected within time limit, punish user and clean up
                if (collected.size === 0) {
                    try {
                        // Set punishment key with 3-hour expiration
                        await redis.set(punishmentKey, 'true', 'EX', 3 * 60 * 60);
                        await thread.send('You ran out of time to answer the question.');
                        console.log(`User ${userId} ran out of time and was punished.`);
                        // Schedule thread deletion 3 minutes after timeout notification
                        setTimeout(async () => {
                            try {
                                await thread.delete();
                                console.log(`Deleted thread ${thread.id} after timeout.`);
                            } catch (err) {
                                console.error(`Failed to delete thread ${thread.id}:`, err);
                            }
                        }, 3 * 60 * 1000);
                    } catch (err) {
                        console.error('Error handling timeout punishment:', err);
                    }
                }
            });

            // Inform the user that verification has started in the thread
            await interaction.editReply({ content: `Verification started in ${thread}.` });
            return;
        }

        // Handle answer button clicks during verification
        if (interaction.customId.startsWith('answer_')) {
            const [, problemId, answer] = interaction.customId.split('_');

            let problemDataRaw;
            try {
                // Retrieve stored problem data from Redis to verify answer
                problemDataRaw = await redis.get(`problem:${problemId}`);
            } catch (err) {
                console.error('Redis error fetching problem data:', err);
                await interaction.reply({ content: 'Internal error occurred. Please try again later.', ephemeral: true });
                return;
            }

            if (!problemDataRaw) {
                // Handle missing problem data scenario
                console.error(`No problem data found in Redis for problemId: ${problemId}`);
                await interaction.reply({ content: 'Problem data not found. Please try again.', ephemeral: true });
                return;
            }

            const { correctAnswer } = JSON.parse(problemDataRaw);
            const isCorrect = answer === correctAnswer;

            const guild = interaction.guild;
            if (!guild) {
                console.error("Guild not found.");
                await interaction.reply({ content: 'Guild not found. Cannot process your answer.', ephemeral: true });
                return;
            }

            let member;
            try {
                // Fetch member to assign roles if answer is correct
                member = await guild.members.fetch(userId);
            } catch (err) {
                console.error(`Failed to fetch member ${userId}:`, err);
                await interaction.reply({ content: 'Failed to fetch your member info.', ephemeral: true });
                return;
            }

            if (isCorrect) {
                // Correct answer: assign verified role and clean up verification thread
                try {
                    const role = await guild.roles.fetch(verifiedRoleId);
                    if (role) {
                        await member.roles.add(role);
                        console.log(`Granted verified role to user ${userId}`);
                    } else {
                        console.error("Verified role not found.");
                    }
                    await interaction.reply({ content: `Correct! You have been verified. Please check out the <#${rulesChannelId}> channel.`, ephemeral: true });
                    // Remove the answer message to keep thread clean
                    await interaction.message.delete();
                    // Delete the verification thread after successful verification
                    if (interaction.channel) {
                        await interaction.channel.delete();
                        console.log(`Deleted verification thread for user ${userId} after correct answer.`);
                    }
                } catch (err) {
                    console.error('Error processing correct answer:', err);
                    await interaction.reply({ content: 'Internal error occurred. Please try again later.', ephemeral: true });
                }
            } else {
                // Incorrect answer: punish user and schedule thread deletion
                const punishmentKey = `punished:${userId}`;
                try {
                    // Set punishment with 3-hour expiration
                    await redis.set(punishmentKey, 'true', 'EX', 3 * 60 * 60);
                    await interaction.reply({ content: 'Incorrect. You have been suspended from answering for 3 hours.', ephemeral: true });
                    // Remove the answer message to keep thread clean
                    await interaction.message.delete();
                    console.log(`User ${userId} answered incorrectly and was punished.`);
                    if (interaction.channel) {
                        // Schedule thread deletion 3 minutes after punishment notification
                        setTimeout(async () => {
                            try {
                                await interaction.channel?.delete();
                                console.log(`Deleted verification thread for user ${userId} after punishment.`);
                            } catch (err) {
                                console.error(`Failed to delete thread for user ${userId}:`, err);
                            }
                        }, 3 * 60 * 1000);
                    }
                } catch (err) {
                    console.error('Error handling incorrect answer punishment:', err);
                    await interaction.reply({ content: 'Internal error occurred. Please try again later.', ephemeral: true });
                }
            }
        }
    }
};

/**
 * Handle help command button interactions
 */
async function handleHelpButtons(interaction: MessageComponentInteraction) {
    if (!interaction.isButton()) return;

    switch (interaction.customId) {
        case 'help_status_button':
            await interaction.reply({
                content: '📊 Use `/lenza-status` to check bot health and backend connectivity status.',
                ephemeral: true
            });
            break;
        
        case 'help_tools_button':
            await interaction.reply({
                content: '🔧 Use `/lenza-tools` to manage your tool whitelist. You can view, add, remove, or clear whitelisted tools that Lenza can use automatically.',
                ephemeral: true
            });
            break;
        
        default:
            await interaction.reply({
                content: '❓ Unknown help button. Please try again.',
                ephemeral: true
            });
            break;
    }
}