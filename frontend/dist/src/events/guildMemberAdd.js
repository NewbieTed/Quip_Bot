"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const discord_js_1 = require("discord.js");
const config_json_1 = require("../../config.json");
module.exports = {
    name: discord_js_1.Events.GuildMemberAdd,
    async execute(member) {
        // Get the welcome channel from the server
        const welcomeChannel = await member.guild.channels.fetch(config_json_1.welcomeChannelId);
        // Create a button to start the verification process
        const row = new discord_js_1.ActionRowBuilder().addComponents(new discord_js_1.ButtonBuilder()
            .setCustomId(`create_thread_${member.id}`)
            .setLabel('Start Verification')
            .setStyle(discord_js_1.ButtonStyle.Primary));
        // Send a welcome message with the verification button
        if (welcomeChannel?.isTextBased()) {
            await welcomeChannel.send({
                content: `Welcome, ${member}! Please click the button below to start the verification process.`,
                components: [row.toJSON()]
            });
        }
    }
};
