"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const discord_js_1 = require("discord.js");
const node_fetch_1 = __importDefault(require("node-fetch"));
const config_json_1 = require("../../config.json");
const redis_1 = require("../redis");
module.exports = {
    name: discord_js_1.Events.InteractionCreate,
    async execute(interaction) {
        // Handle chat input commands
        if (interaction.isChatInputCommand()) {
            const command = interaction.client.commands.get(interaction.commandName);
            if (!command) {
                console.error(`No command matching ${interaction.commandName} was found.`);
                return;
            }
            try {
                await command.execute(interaction);
            }
            catch (error) {
                console.error(error);
                if (interaction.replied || interaction.deferred) {
                    await interaction.followUp({ content: 'There was an error while executing this command!', ephemeral: true });
                }
                else {
                    await interaction.reply({ content: 'There was an error while executing this command!', ephemeral: true });
                }
            }
        }
        else if (interaction.isButton()) {
            // Handle button interactions
            const userId = interaction.user.id;
            // Handle the creation of a verification thread
            if (interaction.customId.startsWith('create_thread_')) {
                const targetUserId = interaction.customId.split('_')[2];
                // Ensure the user is clicking their own button
                if (userId !== targetUserId) {
                    await interaction.reply({ content: 'You can only click your own verification button.', ephemeral: true });
                    return;
                }
                // Check if the user is currently punished
                const punishmentKey = `punished:${userId}`;
                const isPunished = await redis_1.redis.exists(punishmentKey);
                if (isPunished) {
                    const remainingTime = await redis_1.redis.ttl(punishmentKey);
                    await interaction.reply({ content: `You are suspended from answering for ${Math.ceil(remainingTime / 60)} minutes.`, ephemeral: true });
                    return;
                }
                await interaction.deferReply({ ephemeral: true });
                const guild = interaction.guild;
                const member = await guild.members.fetch(userId);
                // Create a new private thread for the verification process
                const thread = await interaction.channel.threads.create({
                    name: `verification-${member.user.username}`,
                    autoArchiveDuration: 60,
                    type: discord_js_1.ChannelType.PrivateThread,
                    reason: 'User verification',
                });
                // Add the user to the thread
                await thread.members.add(userId);
                // Fetch the verification problem from the backend
                const problemResponse = await (0, node_fetch_1.default)('http://localhost:8080/problem');
                const problemData = await problemResponse.json();
                if (!problemData.status) {
                    await thread.send('Failed to fetch the problem data.');
                    return;
                }
                const { problemId, question, choices, image, correctAnswer } = problemData.data;
                // Store the correct answer in Redis for later verification
                await redis_1.redis.set(`problem:${problemId}`, JSON.stringify({ correctAnswer }));
                // Display the question and answer choices as buttons
                let content = `**Question:** ${question}`;
                if (image) {
                    content += `
${image}`;
                }
                await thread.send({ content });
                const choiceRows = choices.map((choice) => {
                    return new discord_js_1.ActionRowBuilder().addComponents(new discord_js_1.ButtonBuilder()
                        .setCustomId(`answer_${problemId}_${choice}`)
                        .setLabel(choice)
                        .setStyle(discord_js_1.ButtonStyle.Secondary));
                });
                const message = await thread.send({ content: 'Choose your answer:', components: choiceRows });
                // Set a 60-second timer for the user to answer
                const collector = message.createMessageComponentCollector({ time: 60000 });
                collector.on('end', async (collected) => {
                    if (collected.size === 0) {
                        // If the user runs out of time, apply a punishment
                        const punishmentKey = `punished:${userId}`;
                        await redis_1.redis.set(punishmentKey, 'true', { EX: 3 * 60 * 60 }); // 3-hour punishment
                        await thread.send('You ran out of time to answer the question.');
                        setTimeout(() => thread.delete(), 3 * 60 * 1000); // Delete the thread after 3 minutes
                    }
                });
                await interaction.followUp({ content: `Verification started in ${thread}.`, ephemeral: true });
            }
            else if (interaction.customId.startsWith('answer_')) {
                // Handle the user's answer submission
                const [, problemId, answer] = interaction.customId.split('_');
                // Retrieve the correct answer from Redis
                const problemData = await redis_1.redis.get(`problem:${problemId}`);
                const { correctAnswer } = JSON.parse(problemData);
                const isCorrect = answer === correctAnswer;
                if (isCorrect) {
                    // If the answer is correct, grant the verified role
                    const guild = interaction.guild;
                    const member = await guild.members.fetch(userId);
                    const role = await guild.roles.fetch(config_json_1.verifiedRoleId);
                    await member.roles.add(role);
                    await interaction.reply({ content: `Correct! You have been verified. Please check out the <#${config_json_1.rulesChannelId}> channel.`, ephemeral: true });
                    await interaction.message.delete(); // Remove the answer buttons
                    await interaction.channel.delete(); // Delete the thread
                }
                else {
                    // If the answer is incorrect, apply a punishment
                    const punishmentKey = `punished:${userId}`;
                    await redis_1.redis.set(punishmentKey, 'true', { EX: 3 * 60 * 60 }); // 3-hour punishment
                    await interaction.reply({ content: 'Incorrect. You have been suspended from answering for 3 hours.', ephemeral: true });
                    await interaction.message.delete(); // Remove the answer buttons
                    setTimeout(() => interaction.channel.delete(), 3 * 60 * 1000); // Delete the thread after 3 minutes
                }
            }
        }
    }
};
