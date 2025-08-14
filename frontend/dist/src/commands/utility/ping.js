"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const discord_js_1 = require("discord.js");
module.exports = {
    data: new discord_js_1.SlashCommandBuilder()
        .setName('ping')
        .setDescription('Sends Pong!'),
    async execute(interaction) {
        console.log('Ping command: Starting execution.');
        try {
            console.log('Ping command: Attempting deferReply.');
            await interaction.deferReply();
            console.log('Ping command: deferReply successful. Attempting editReply.');
            await interaction.editReply('Pong!');
            console.log('Ping command: editReply successful.');
        }
        catch (error) {
            console.error('Ping command: Error caught during execution:', error);
        }
        console.log('Ping command: Execution finished.');
    },
};
