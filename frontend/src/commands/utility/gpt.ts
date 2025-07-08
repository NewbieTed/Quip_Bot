import { SlashCommandBuilder, ChatInputCommandInteraction, CacheType } from 'discord.js';
import { OpenAI } from 'openai';

module.exports = {
  
	data: new SlashCommandBuilder()
		.setName('gpt')
		.setDescription('Get response from ChatGPT')
    .addStringOption(option =>
      option.setName('prompt')
        .setDescription('The text prompt')
        .setRequired(true)),
	async execute(interaction: ChatInputCommandInteraction<CacheType>) {
    const openai = new OpenAI({
      apiKey: process.env.OPENAI_API_KEY,
    });
    // Defer the reply to prevent the interaction from timing out
    await interaction.deferReply();

    // Get the user's prompt
	const prompt = interaction.options.getString('prompt', true);

    // Send the prompt to the OpenAI API and get a response
		const response = await openai.chat.completions.create({
      model: "o4-mini",
      messages: [
        {
          "role": "system",
          "content": [
            {
              "type": "text",
              "text": `
                You are a helpful assistant, and your advantage is that you use concise language that does not exceed 2000 characters. 
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

    // Send the response back to the user
    if (response.choices[0].message?.content) {
      await interaction.editReply(response.choices[0].message.content);
    } else {
      await interaction.editReply('Could not get a response from ChatGPT.');
    }
	},
};
