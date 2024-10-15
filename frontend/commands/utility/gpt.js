const { SlashCommandBuilder } = require('discord.js');
const { OpenAI } = require('openai')

module.exports = {
  
	data: new SlashCommandBuilder()
		.setName('gpt')
		.setDescription('Get response from ChatGPT')
    .addStringOption(option =>
      option.setName('prompt')
        .setDescription('The text prompt')
        .setRequired(true)),
	async execute(interaction) {
  const openai = new OpenAI();
    // Initialite defer reply
    await interaction.deferReply();

    // Get user entered term
		const prompt = interaction.options.getString('prompt');
		const response = await openai.chat.completions.create({
      model: "gpt-4o-mini",
      messages: [
        {
          "role": "system",
          "content": [
            {
              "type": "text",
              "text": `
                You are a helpful assistant
              `
            }
          ]
        },
        {
          "role": "user",
          "content": [
            {
              "type": "text",
              "text": prompt
            }
          ]
        }
      ]
    });
    await interaction.editReply(response.choices[0].message);
	},
};
