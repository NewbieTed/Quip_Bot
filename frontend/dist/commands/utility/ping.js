"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const discord_js_1 = require("discord.js");
module.exports = {
    data: new discord_js_1.SlashCommandBuilder()
        .setName('ping')
        .setDescription('Sends Pong!'),
    async execute(interaction) {
        // Reply with "Pong!" when the command is executed
        await interaction.reply('Pong!');
    },
};
