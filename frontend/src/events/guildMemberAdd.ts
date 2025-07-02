import { Events, ActionRowBuilder, ButtonBuilder, ButtonStyle, GuildMember, ChannelType, TextChannel } from 'discord.js';
import { verificationChannelId } from '../../config.json'; // Assuming this exists in config.json

module.exports = {
    name: Events.GuildMemberAdd,
    async execute(member: GuildMember) {
        const guild = member.guild;
        if (!guild) return;

        const verificationChannel = await guild.channels.fetch(verificationChannelId) as TextChannel;

        if (!verificationChannel || verificationChannel.type !== ChannelType.GuildText) {
            console.error('Verification channel not found or is not a text channel.');
            return;
        }

        try {
            // Create a new private thread for the user
            const thread = await verificationChannel.threads.create({
                name: `verification-${member.user.username}`,
                autoArchiveDuration: 60,
                type: ChannelType.PrivateThread,
                reason: 'User verification quiz',
            });

            // Add the user to the thread (they are automatically added if they create it, but good to be explicit)
            await thread.members.add(member.id);

            // Create a button to start the quiz
            const row = new ActionRowBuilder<ButtonBuilder>().addComponents(
                new ButtonBuilder()
                    .setCustomId(`start_quiz_${member.id}`)
                    .setLabel('Start Quiz')
                    .setStyle(ButtonStyle.Primary)
            );

            // Send a welcome message with the quiz button in the new thread
            await thread.send({
                content: `Welcome, ${member}! To gain access to the server, please click the button below to start a short quiz.`,
                components: [row.toJSON()]
            });

            console.log(`Created private verification thread ${thread.id} for user ${member.id}`);

        } catch (error) {
            console.error(`Failed to create verification thread for ${member.id}:`, error);
        }
    },
};
