"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.redis = void 0;
const ioredis_1 = __importDefault(require("ioredis"));
// Create a new Redis client
const client = new ioredis_1.default();
exports.redis = client;
// Log any errors that occur with the Redis client
client.on('error', (err) => console.error('Redis Client Error', err));
