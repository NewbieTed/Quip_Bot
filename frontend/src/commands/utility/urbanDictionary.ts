import { SlashCommandBuilder, EmbedBuilder, CacheType, ChatInputCommandInteraction } from 'discord.js';
import { request } from 'undici';

interface UrbanDictionaryEntry {
  word: string;
  permalink: string;
  definition: string;
  example: string;
}

module.exports = {
	data: new SlashCommandBuilder()
		.setName('urban')
		.setDescription('Looks up the given term from Urban Dictionary')
    .addStringOption(option =>
      option.setName('term')
        .setDescription('The word to look up')
        .setRequired(true)),
	async execute(interaction: ChatInputCommandInteraction<CacheType>) {
    // Defer the reply to prevent the interaction from timing out
    await interaction.deferReply();

    // Get the user's search term
		const term = interaction.options.getString('term', true);

    // Query the Urban Dictionary API
		const query = new URLSearchParams({ term });
		const dictResult = await request(`https://api.urbandictionary.com/v0/define?${query}`);
		    const { list }: { list: UrbanDictionaryEntry[] } = await dictResult.body.json() as { list: UrbanDictionaryEntry[] };

    // If no results are found, send a message to the user
    if (!list.length) {
      return interaction.editReply(`No results found for **${term}**.`);
    }
  
    const [answer] = list;

    // Create an embed to display the definition
    const embed = new EmbedBuilder()
	    .setColor(0xEFFF00)
	    .setTitle(answer.word)
	    .setURL(answer.permalink)
	    .addFields({ name: 'Definition', value: trim(answer.definition, 1_024) }, { name: 'Example', value: trim(answer.example, 1_024) });

    // Send the embed back to the user
    interaction.editReply({ embeds: [embed] });
	},
};

// Trim a string to a maximum length
const trim = (str: string, max: number): string => (str.length > max ? `${str.slice(0, max - 3)}...` : str);
