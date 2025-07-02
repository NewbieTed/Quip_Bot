"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const discord_js_1 = require("discord.js");
const openai_1 = require("openai");
module.exports = {
    data: new discord_js_1.SlashCommandBuilder()
        .setName('gpt')
        .setDescription('Get response from ChatGPT')
        .addStringOption(option => option.setName('prompt')
        .setDescription('The text prompt')
        .setRequired(true)),
    async execute(interaction) {
        const openai = new openai_1.OpenAI();
        // Defer the reply to prevent the interaction from timing out
        await interaction.deferReply();
        // Get the user's prompt
        const prompt = interaction.options.getString('prompt', true);
        // Send the prompt to the OpenAI API and get a response
        const response = await openai.chat.completions.create({
            model: "4o-mini",
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
        // Send the response back to the user
        if (response.choices[0].message?.content) {
            await interaction.editReply(response.choices[0].message.content);
        }
        else {
            await interaction.editReply('Could not get a response from ChatGPT.');
        }
    },
};
