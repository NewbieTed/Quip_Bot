const { REST, Routes } = require('discord.js');
const { clientId, guildId, token } = require('./config.json');
const fs = require('node:fs');
const path = require('node:path');
const { argv } = require('node:process');

const args = {"-d": ["guild", "global"]}

if (argv[2] != null) {
  if (!args.hasOwnProperty(argv[2])) {
    console.log("Usage: node deploy-commands.js <command> <option>\n\t" +
      "There is no such command: " + argv[2] + (argv[3] == null ?  "" : " " + argv[3]));
    return;
  } else if (!args[argv[2]].includes(argv[3])) {
    console.log("Usage: node deploy-commands.js <command> <option>\n\t" +
      "There is no such command: " + argv[2] + (argv[3] == null ?  "" : " " + argv[3]));
    return;
  }
}



const commands = [];
// Grab all the command folders from the commands directory you created earlier
const foldersPath = path.join(__dirname, 'commands');
const commandFolders = fs.readdirSync(foldersPath);

for (const folder of commandFolders) {
	// Grab all the command files from the commands directory you created earlier
	const commandsPath = path.join(foldersPath, folder);
	const commandFiles = fs.readdirSync(commandsPath).filter(file => file.endsWith('.js'));
	// Grab the SlashCommandBuilder#toJSON() output of each command's data for deployment
	for (const file of commandFiles) {
		const filePath = path.join(commandsPath, file);
		const command = require(filePath);
		if ('data' in command && 'execute' in command) {
			commands.push(command.data.toJSON());
		} else {
			console.log(`[WARNING] The command at ${filePath} is missing a required "data" or "execute" property.`);
		}
	}
}

// Construct and prepare an instance of the REST module
const rest = new REST().setToken(token);

if (argv[2] == "-d") {
  if (argv[3] == "guild") {
    // for guild-based commands
    rest.put(Routes.applicationGuildCommands(clientId, guildId), { body: [] })
        .then(() => console.log('Successfully deleted all guild commands.'))
        .catch(console.error);
  } else if (argv[3] == "global") {
    // for global commands
    rest.put(Routes.applicationCommands(clientId), { body: [] })
        .then(() => console.log('Successfully deleted all application commands.'))
        .catch(console.error);
  }
  return;
}

// and deploy your commands!
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
		const data = await rest.put(
			Routes.applicationGuildCommands(clientId, guildId),
			{ body: commands },
		);

		console.log(`Successfully reloaded ${data.length} application (/) commands.`);
	} catch (error) {
		// And of course, make sure you catch and log any errors!
		console.error(error);
	}
})();
