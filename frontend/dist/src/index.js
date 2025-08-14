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
const dotenv = __importStar(require("dotenv"));
const node_fs_1 = __importDefault(require("node:fs"));
const node_path_1 = __importDefault(require("node:path"));
dotenv.config({ path: node_path_1.default.resolve(__dirname, '../.env') });
const discord_js_1 = require("discord.js");
const config_json_1 = require("../config.json");
// Initialize the Discord client with necessary intents so the bot can respond to commands and events
const client = new discord_js_1.Client({
    intents: [
        discord_js_1.GatewayIntentBits.Guilds,
        discord_js_1.GatewayIntentBits.GuildMessages,
        discord_js_1.GatewayIntentBits.MessageContent, // Required to read message content
        discord_js_1.GatewayIntentBits.GuildMembers // Required to listen for guildMemberAdd event
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
        Promise.resolve(`${filePath}`).then(s => __importStar(require(s))).then(command => {
            // Set the command in the client.commands collection
            if ('data' in command && 'execute' in command) {
                client.commands.set(command.data.name, command);
            }
            else {
                console.log(`[WARNING] The command at ${filePath} is missing a required "data" or "execute" property.`);
            }
        });
    }
}
// Load all event files from the events directory
const eventsPath = node_path_1.default.join(__dirname, 'events');
const eventFiles = node_fs_1.default.readdirSync(eventsPath).filter(file => file.endsWith('.ts'));
for (const file of eventFiles) {
    const filePath = node_path_1.default.join(eventsPath, file);
    Promise.resolve(`${filePath}`).then(s => __importStar(require(s))).then(event => {
        // Attach the appropriate event listener (once or on) based on the event file
        if (event.name && event.execute) {
            if (event.once) {
                client.once(event.name, (...args) => event.execute(...args));
            }
            else {
                client.on(event.name, (...args) => event.execute(...args));
            }
        }
    });
}
// Import tool approval handler for cleanup
const tool_approval_handler_1 = require("./services/tool-approval-handler");
// Initialize tool approval handler for periodic cleanup
const toolApprovalHandler = new tool_approval_handler_1.ToolApprovalHandler();
// Set up periodic cleanup of expired approvals (every 5 minutes)
setInterval(() => {
    toolApprovalHandler.cleanupExpiredApprovals();
}, 5 * 60 * 1000);
// Log in to Discord with the bot's token
client.login(config_json_1.token).then(() => {
    console.log('Bot logged in successfully');
}).catch(error => {
    console.error('Login failed:', error);
});
