"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const node_fs_1 = __importDefault(require("node:fs"));
const node_path_1 = __importDefault(require("node:path"));
const discord_js_1 = require("discord.js");
const config_json_1 = require("../config.json");
// Initialize the Discord client with necessary intents so the bot can respond to commands and events
const client = new discord_js_1.Client({
    intents: [
        discord_js_1.GatewayIntentBits.Guilds,
        discord_js_1.GatewayIntentBits.GuildMessages,
        discord_js_1.GatewayIntentBits.MessageContent, // Required to read message content
        discord_js_1.GatewayIntentBits.GuildMembers // Required to listen for the guildMemberAdd event
    ]
});
// Load all command files from the commands directory
client.commands = new discord_js_1.Collection();
const commandsPath = node_path_1.default.join(__dirname, 'commands');
const commandFolders = node_fs_1.default.readdirSync(commandsPath);
for (const folder of commandFolders) {
    const folderPath = node_path_1.default.join(commandsPath, folder);
    const commandFiles = node_fs_1.default.readdirSync(folderPath).filter(file => file.endsWith('.ts'));
    for (const file of commandFiles) {
        const filePath = node_path_1.default.join(folderPath, file);
        const command = require(filePath);
        // Set the command in the client.commands collection
        if ('data' in command && 'execute' in command) {
            client.commands.set(command.data.name, command);
        }
        else {
            console.log(`[WARNING] The command at ${filePath} is missing a required "data" or "execute" property.`);
        }
    }
}
// Load all event files from the events directory
const eventsPath = node_path_1.default.join(__dirname, 'events');
const eventFiles = node_fs_1.default.readdirSync(eventsPath).filter(file => file.endsWith('.ts'));
for (const file of eventFiles) {
    const filePath = node_path_1.default.join(eventsPath, file);
    const event = require(filePath);
    // Attach the appropriate event listener (once or on) based on the event file
    if (event.name && event.execute) {
        if (event.once) {
            client.once(event.name, (...args) => event.execute(...args));
        }
        else {
            client.on(event.name, (...args) => event.execute(...args));
        }
    }
}
// Log in to Discord with the bot's token
client.login(config_json_1.token).then(() => {
    console.log('Bot logged in successfully');
}).catch(error => {
    console.error('Login failed:', error);
});
