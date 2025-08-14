"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
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
(async () => {
    // Parse command-line arguments to check for valid commands and options
    if (node_process_1.argv[2] != null) {
        const commandArg = node_process_1.argv[2];
        const optionArg = node_process_1.argv[3];
        if (!args.hasOwnProperty(commandArg)) {
            console.log("Usage: node dist/deploy-commands.js <command> <option>\n\t" +
                "There is no such command: " + commandArg + (optionArg == null ? "" : " " + optionArg));
            return;
        }
        else if (optionArg && !args[commandArg].includes(optionArg)) {
            console.log("Usage: node dist/deploy-commands.js <command> <option>\n\t" +
                "There is no such command: " + commandArg + (optionArg == null ? "" : " " + optionArg));
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
        const commandFiles = node_fs_1.default.readdirSync(commandsPath).filter(file => file.endsWith('.ts') || file.endsWith('.js'));
        // Grab the SlashCommandBuilder#toJSON() output of each command's data for deployment
        for (const file of commandFiles) {
            const filePath = node_path_1.default.join(commandsPath, file);
            const command = await Promise.resolve(`${filePath}`).then(s => __importStar(require(s))); // Changed from require to import
            if ('data' in command.default && 'execute' in command.default) { // Access .default
                commands.push(command.default.data.toJSON());
            }
            else {
                console.log(`[WARNING] The command at ${filePath} is missing a required "data" or "execute" property.`);
            }
        }
    }
    console.log(`Loaded commands: ${commands.map(cmd => cmd.name).join(', ')}`);
    // Initialize the REST module with the bot's token
    const rest = new discord_js_1.REST().setToken(config_json_1.token);
    // Handle the command deletion argument
    if (node_process_1.argv[2] == "-d") {
        if (node_process_1.argv[3] == "guild") {
            // Delete all guild-based commands
            await rest.put(discord_js_1.Routes.applicationGuildCommands(config_json_1.clientId, config_json_1.guildId), { body: [] })
                .then(() => console.log('Successfully deleted all guild commands.'))
                .catch(console.error);
        }
        else if (node_process_1.argv[3] == "global") {
            // Delete all global commands
            await rest.put(discord_js_1.Routes.applicationCommands(config_json_1.clientId), { body: [] })
                .then(() => console.log('Successfully deleted all application commands.'))
                .catch(console.error);
        }
        return; // Exit after handling deletion
    }
    // Deploy the loaded commands
    try {
        console.log(`Started refreshing ${commands.length} application (/) commands.`);
        console.log(`Deploying to GUILD: ${config_json_1.guildId} with CLIENT: ${config_json_1.clientId}`);
        // The put method is used to fully refresh all commands in the guild with the current set
        const data = await rest.put(discord_js_1.Routes.applicationGuildCommands(config_json_1.clientId, config_json_1.guildId), { body: commands }); // Cast to any[] for now to resolve type errors
        console.log(`Successfully reloaded ${data.length} application (/) commands.`);
    }
    catch (error) {
        // And of course, make sure you catch and log any errors!
        console.error(error);
    }
})();
