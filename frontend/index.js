const fs = require('node:fs');
const path = require('node:path');
const { Client, GatewayIntentBits } = require('discord.js');
const { token } = require('./config.json'); 
const { sendButtonMessage } = require('./events/buttonHandler'); // Import sendButtonMessage

// Initialize client with required intents
const client = new Client({ intents: [GatewayIntentBits.Guilds] });

// Load events from the events folder
const eventsPath = path.join(__dirname, 'events');
const eventFiles = fs.readdirSync(eventsPath).filter(file => file.endsWith('.js'));

for (const file of eventFiles) {
    const filePath = path.join(eventsPath, file);
    const event = require(filePath);

    // Attach event listener if event name and execute function are defined
    if (event.name && event.execute) {
        client.on(event.name, (...args) => event.execute(...args));
    }
}

// When the bot is ready, send the initial button message
client.once('ready', () => {
    console.log(`Logged in as ${client.user.tag}!`);
    sendButtonMessage(client); // Send the button message on startup
});

// Log in to Discord with the token from config.json
client.login(token).then(() => {
    console.log('Bot logged in successfully');
}).catch(error => {
    console.error('Login failed:', error);
});
