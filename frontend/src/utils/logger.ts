import fs from 'fs';
import path from 'path';

class Logger {
    private logDir: string;
    private logFile: string;

    constructor() {
        this.logDir = path.join(__dirname, '../../logs');
        this.logFile = path.join(this.logDir, 'frontend.log');
        
        // Ensure logs directory exists
        if (!fs.existsSync(this.logDir)) {
            fs.mkdirSync(this.logDir, { recursive: true });
        }
    }

    private formatMessage(level: string, message: string): string {
        const timestamp = new Date().toISOString();
        return `${timestamp} [${level}] ${message}\n`;
    }

    private writeToFile(level: string, message: string): void {
        const formattedMessage = this.formatMessage(level, message);
        fs.appendFileSync(this.logFile, formattedMessage);
    }

    info(message: string): void {
        console.log(message);
        this.writeToFile('INFO', message);
    }

    error(message: string, error?: any): void {
        const errorMessage = error ? `${message} - ${error}` : message;
        console.error(errorMessage);
        this.writeToFile('ERROR', errorMessage);
    }

    warn(message: string): void {
        console.warn(message);
        this.writeToFile('WARN', message);
    }

    debug(message: string): void {
        console.log(message);
        this.writeToFile('DEBUG', message);
    }
}

export const logger = new Logger();