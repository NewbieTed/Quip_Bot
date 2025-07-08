"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const discord_js_1 = require("discord.js");
const undici_1 = require("undici");
module.exports = {
    data: new discord_js_1.SlashCommandBuilder()
        .setName('urban')
        .setDescription('Looks up the given term from Urban Dictionary')
        .addStringOption(option => option.setName('term')
        .setDescription('The word to look up')
        .setRequired(true)),
    async execute(interaction) {
        // Defer the reply to prevent the interaction from timing out
        await interaction.deferReply();
        // Get the user's search term
        const term = interaction.options.getString('term', true);
        // Query the Urban Dictionary API
        const query = new URLSearchParams({ term });
        const dictResult = await (0, undici_1.request)(`https://api.urbandictionary.com/v0/define?${query}`);
        const { list } = await dictResult.body.json();
        // If no results are found, send a message to the user
        if (!list.length) {
            return interaction.editReply(`No results found for **${term}**.`);
        }
        const [answer] = list;
        // Create an embed to display the definition
        const embed = new discord_js_1.EmbedBuilder()
            .setColor(0xEFFF00)
            .setTitle(answer.word)
            .setURL(answer.permalink)
            .addFields({ name: 'Definition', value: trim(answer.definition, 1024) }, { name: 'Example', value: trim(answer.example, 1024) });
        // Send the embed back to the user
        interaction.editReply({ embeds: [embed] });
    },
};
// Trim a string to a maximum length
const trim = (str, max) => (str.length > max ? `${str.slice(0, max - 3)}...` : str);
