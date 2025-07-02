import { REST, Routes } from 'discord.js';
import { clientId, guildId, token } from '../config.json';
import fs from 'node:fs';
import path from 'node:path';
import { argv } from 'node:process';

// Define the valid command-line arguments
const args: { [key: string]: string[] } = {"-d": ["guild", "global"]};

(async () => {
  // Parse command-line arguments to check for valid commands and options
  if (argv[2] != null) {
    const commandArg = argv[2];
    const optionArg = argv[3];

    if (!args.hasOwnProperty(commandArg)) {
      console.log("Usage: node dist/deploy-commands.js <command> <option>\n\t" +
        "There is no such command: " + commandArg + (optionArg == null ?  "" : " " + optionArg));
      return;
    } else if (optionArg && !args[commandArg].includes(optionArg)) {
      console.log("Usage: node dist/deploy-commands.js <command> <option>\n\t" +
        "There is no such command: " + commandArg + (optionArg == null ?  "" : " " + optionArg));
      return;
    }
  }

  const commands = [];
  // Load all command files from the commands directory
  const foldersPath = path.join(__dirname, 'commands');
  const commandFolders = fs.readdirSync(foldersPath);

  for (const folder of commandFolders) {
    // Grab all the command files from the commands directory
    const commandsPath = path.join(foldersPath, folder);
    const commandFiles = fs.readdirSync(commandsPath).filter(file => file.endsWith('.ts') || file.endsWith('.js'));
    // Grab the SlashCommandBuilder#toJSON() output of each command's data for deployment
    for (const file of commandFiles) {
      const filePath = path.join(commandsPath, file);
      const command = await import(filePath); // Changed from require to import
      if ('data' in command.default && 'execute' in command.default) { // Access .default
        commands.push(command.default.data.toJSON());
      } else {
        console.log(`[WARNING] The command at ${filePath} is missing a required "data" or "execute" property.`);
      }
    }
  }

  console.log(`Loaded commands: ${commands.map(cmd => cmd.name).join(', ')}`);

  // Initialize the REST module with the bot's token
  const rest = new REST().setToken(token);

  // Handle the command deletion argument
  if (argv[2] == "-d") {
    if (argv[3] == "guild") {
      // Delete all guild-based commands
      await rest.put(Routes.applicationGuildCommands(clientId, guildId), { body: [] })
          .then(() => console.log('Successfully deleted all guild commands.'))
          .catch(console.error);
    } else if (argv[3] == "global") {
      // Delete all global commands
      await rest.put(Routes.applicationCommands(clientId), { body: [] })
          .then(() => console.log('Successfully deleted all application commands.'))
          .catch(console.error);
    }
    return; // Exit after handling deletion
  }

  // Deploy the loaded commands
  try {
    console.log(`Started refreshing ${commands.length} application (/) commands.`);
    console.log(`Deploying to GUILD: ${guildId} with CLIENT: ${clientId}`);

    // The put method is used to fully refresh all commands in the guild with the current set
    const data = await rest.put(
      Routes.applicationGuildCommands(clientId, guildId),
      { body: commands },
    ) as any[]; // Cast to any[] for now to resolve type errors

    console.log(`Successfully reloaded ${data.length} application (/) commands.`);
  } catch (error) {
    // And of course, make sure you catch and log any errors!
    console.error(error);
  }
})();