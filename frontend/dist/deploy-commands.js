"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const discord_js_1 = require("discord.js");
const config_json_1 = require("../config.json");
const node_fs_1 = __importDefault(require("node:fs"));
const node_path_1 = __importDefault(require("node:path"));
const node_process_1 = require("node:process");
// Define the valid command-line arguments
const args = { "-d": ["guild", "global"] };
// Parse command-line arguments to check for valid commands and options
if (node_process_1.argv[2] != null) {
    if (!args.hasOwnProperty(node_process_1.argv[2])) {
        console.log("Usage: node dist/deploy-commands.js <command> <option>\n\t" +
            "There is no such command: " + node_process_1.argv[2] + (node_process_1.argv[3] == null ? "" : " " + node_process_1.argv[3]));
        return;
    }
    else if (!args[node_process_1.argv[2]].includes(node_process_1.argv[3])) {
        console.log("Usage: node dist/deploy-commands.js <command> <option>\n\t" +
            "There is no such command: " + node_process_1.argv[2] + (node_process_1.argv[3] == null ? "" : " " + node_process_1.argv[3]));
        return;
    }
}
const commands = [];
// Load all command files from the commands directory
const foldersPath = node_path_1.default.join(__dirname, 'commands');
const commandFolders = node_fs_1.default.readdirSync(foldersPath);
for (const folder of commandFolders) {
    // Grab all the command files from the commands directory
    const commandsPath = node_path_1.default.join(foldersPath, folder);
    const commandFiles = node_fs_1.default.readdirSync(commandsPath).filter(file => file.endsWith('.ts'));
    // Grab the SlashCommandBuilder#toJSON() output of each command's data for deployment
    for (const file of commandFiles) {
        const filePath = node_path_1.default.join(commandsPath, file);
        const command = require(filePath);
        if ('data' in command && 'execute' in command) {
            commands.push(command.data.toJSON());
        }
        else {
            console.log(`[WARNING] The command at ${filePath} is missing a required "data" or "execute" property.`);
        }
    }
}
// Initialize the REST module with the bot's token
const rest = new discord_js_1.REST().setToken(config_json_1.token);
// Handle the command deletion argument
if (node_process_1.argv[2] == "-d") {
    if (node_process_1.argv[3] == "guild") {
        // Delete all guild-based commands
        rest.put(discord_js_1.Routes.applicationGuildCommands(config_json_1.clientId, config_json_1.guildId), { body: [] })
            .then(() => console.log('Successfully deleted all guild commands.'))
            .catch(console.error);
    }
    else if (node_process_1.argv[3] == "global") {
        // Delete all global commands
        rest.put(discord_js_1.Routes.applicationCommands(config_json_1.clientId), { body: [] })
            .then(() => console.log('Successfully deleted all application commands.'))
            .catch(console.error);
    }
    return;
}
// Deploy the loaded commands
(async () => {
    try {
        console.log(`Started refreshing ${commands.length} application (/) commands.`);
        // The put method is used to fully refresh all commands in the guild with the current set
        // // for global commands
        // const data = await rest.put(
        // 	Routes.applicationCommands(clientId),
        //   { body: commands },
        // );
        // The put method is used to fully refresh all commands in the guild with the current set
        const data = await rest.put(discord_js_1.Routes.applicationGuildCommands(config_json_1.clientId, config_json_1.guildId), { body: commands });
        console.log(`Successfully reloaded ${data.length} application (/) commands.`);
    }
    catch (error) {
        // And of course, make sure you catch and log any errors!
        console.error(error);
    }
})();
