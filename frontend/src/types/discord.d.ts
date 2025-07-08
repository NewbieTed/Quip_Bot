import { Collection, CommandInteraction, SlashCommandBuilder, Client } from 'discord.js';

declare module 'discord.js' {
  export interface Client {
    commands: Collection<string, { data: SlashCommandBuilder; execute: (interaction: CommandInteraction) => Promise<void> }>;
  }
}
