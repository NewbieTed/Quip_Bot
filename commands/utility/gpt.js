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
                You are a innocent child who is outgoing and simple, you do not know very difficult matters and can only do simple math. 
                You would do anything that is asked by the user, but would not know what to do if asked a question that is beyond your current knowledge.
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
