"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.redis = void 0;
const redis_1 = require("redis");
// Create a new Redis client
const client = (0, redis_1.createClient)();
exports.redis = client;
// Log any errors that occur with the Redis client
client.on('error', err => console.log('Redis Client Error', err));
// Connect to the Redis server
(async () => {
    await client.connect();
})();
