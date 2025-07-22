// Extend discord.js Client type to include 'commands' property
declare module 'discord.js' {
    interface Client {
        commands: Collection<string, { data: SlashCommandBuilder; execute: (interaction: CommandInteraction) => Promise<void> }>;
    }
}

import * as dotenv from 'dotenv'; 
import fs from 'node:fs';
import path from 'node:path';

dotenv.config({ path: path.resolve(__dirname, '../.env') });
import { logger } from './utils/logger';
logger.info('OPENAI_API_KEY configured: ' + (process.env.OPENAI_API_KEY ? 'Yes' : 'No'));        
import { Client, Collection, GatewayIntentBits, SlashCommandBuilder, CommandInteraction, Events, ChatInputCommandInteraction } from 'discord.js';
import { token } from '../config.json';

// Initialize the Discord client with necessary intents so the bot can respond to commands and events
const client = new Client({
    intents: [
        GatewayIntentBits.Guilds,
        GatewayIntentBits.GuildMessages,
        GatewayIntentBits.MessageContent, // Required to read message content
        GatewayIntentBits.GuildMembers // Required to listen for guildMemberAdd event
    ]
});

// Load all command files from the commands directory
client.commands = new Collection<string, { data: SlashCommandBuilder; execute: (interaction: CommandInteraction) => Promise<void> }>();
const commandsPath = path.join(__dirname, 'commands');
const commandFolders = fs.readdirSync(commandsPath);

for (const folder of commandFolders) {
    const folderPath = path.join(commandsPath, folder);
    const commandFiles = fs.readdirSync(folderPath).filter(file => file.endsWith('.ts'));
    for (const file of commandFiles) {
        const filePath = path.join(folderPath, file);
        import(filePath).then(command => {
            // Set the command in the client.commands collection
            if ('data' in command && 'execute' in command) {
                client.commands.set(command.data.name, command);
            } else {
                console.log(`[WARNING] The command at ${filePath} is missing a required "data" or "execute" property.`);
            }
        });
    }
}


// Load all event files from the events directory
const eventsPath = path.join(__dirname, 'events');
const eventFiles = fs.readdirSync(eventsPath).filter(file => file.endsWith('.ts'));

for (const file of eventFiles) {
    const filePath = path.join(eventsPath, file);
    import(filePath).then(event => {
        // Attach the appropriate event listener (once or on) based on the event file
        if (event.name && event.execute) {
            if (event.once) {
                client.once(event.name, (...args) => event.execute(...args));
            } else {
                client.on(event.name, (...args) => event.execute(...args));
            }
        }
    });
}



// Log in to Discord with the bot's token
client.login(token).then(() => {
    console.log('Bot logged in successfully');
}).catch(error => {
    console.error('Login failed:', error);
});