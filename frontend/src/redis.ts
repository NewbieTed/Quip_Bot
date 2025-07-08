import Redis from 'ioredis';

// Create a new Redis client
const client = new Redis();

// Log any errors that occur with the Redis client
client.on('error', (err: Error) => console.error('Redis Client Error', err));

// Export the Redis client for use in other modules
export { client as redis };
