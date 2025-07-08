import { Client, Events } from 'discord.js';

module.exports = {
	name: Events.ClientReady,
	once: true,
	execute(client: Client) {
		// Log a message to the console when the bot is ready
		if (client.user) {
			console.log(`Ready! Logged in as ${client.user.tag}`);
		} else {
			console.log('Ready! Client user is null.');
		}
	},
};
