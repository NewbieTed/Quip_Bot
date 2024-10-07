const { SlashCommandBuilder } = require('discord.js');
const { EmbedBuilder } = require('discord.js');
const { request } = require('undici');

module.exports = {
	data: new SlashCommandBuilder()
		.setName('urban')
		.setDescription('Sends Pong!')
    .addStringOption(option =>
      option.setName('term')
        .setDescription('The word to look up')
        .setRequired(true)),
	async execute(interaction) {
    await interaction.deferReply();
		const term = interaction.options.getString('term');
		const query = new URLSearchParams({ term });

		const dictResult = await request(`https://api.urbandictionary.com/v0/define?${query}`);
		const { list } = await dictResult.body.json();
    if (!list.length) {
      return interaction.editReply(`No results found for **${term}**.`);
    }
  
    const [answer] = list;

    const embed = new EmbedBuilder()
	    .setColor(0xEFFF00)
	    .setTitle(answer.word)
	    .setURL(answer.permalink)
	    .addFields({ name: 'Definition', value: trim(answer.definition, 1_024) }, { name: 'Example', value: trim(answer.example, 1_024) });

    interaction.editReply({ embeds: [embed] });
	},
};

const trim = (str, max) => (str.length > max ? `${str.slice(0, max - 3)}...` : str);
