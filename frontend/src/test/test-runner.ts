#!/usr/bin/env ts-node

/**
 * Comprehensive test runner for Discord bot frontend
 * Runs all test suites and generates coverage reports
 */

import { execSync } from 'child_process';
import { existsSync } from 'fs';
import path from 'path';

interface TestSuite {
  name: string;
  pattern: string;
  description: string;
}

const testSuites: TestSuite[] = [
  {
    name: 'Unit Tests',
    pattern: 'src/**/*.test.ts',
    description: 'Individual component and service tests'
  },
  {
    name: 'Integration Tests',
    pattern: 'src/test/integration/**/*.test.ts',
    description: 'End-to-end workflow tests'
  },
  {
    name: 'Performance Tests',
    pattern: 'src/test/performance/**/*.test.ts',
    description: 'Performance and load tests'
  }
];

class TestRunner {
  private verbose: boolean;
  private coverage: boolean;
  private watch: boolean;

  constructor(options: { verbose?: boolean; coverage?: boolean; watch?: boolean } = {}) {
    this.verbose = options.verbose || false;
    this.coverage = options.coverage || false;
    this.watch = options.watch || false;
  }

  async runAllTests(): Promise<void> {
    console.log('🧪 Starting comprehensive test suite...\n');

    if (this.coverage) {
      console.log('📊 Coverage reporting enabled\n');
    }

    let totalPassed = 0;
    let totalFailed = 0;
    const results: Array<{ suite: string; passed: boolean; error?: string }> = [];

    for (const suite of testSuites) {
      console.log(`\n🔍 Running ${suite.name}...`);
      console.log(`   ${suite.description}`);
      console.log(`   Pattern: ${suite.pattern}\n`);

      try {
        const result = await this.runTestSuite(suite);
        results.push({ suite: suite.name, passed: true });
        totalPassed++;
        console.log(`✅ ${suite.name} completed successfully\n`);
      } catch (error) {
        results.push({ 
          suite: suite.name, 
          passed: false, 
          error: error instanceof Error ? error.message : String(error)
        });
        totalFailed++;
        console.log(`❌ ${suite.name} failed: ${error}\n`);
      }
    }

    this.printSummary(results, totalPassed, totalFailed);

    if (totalFailed > 0) {
      process.exit(1);
    }
  }

  private async runTestSuite(suite: TestSuite): Promise<void> {
    const command = this.buildVitestCommand(suite.pattern);
    
    if (this.verbose) {
      console.log(`   Command: ${command}`);
    }

    try {
      const output = execSync(command, { 
        cwd: process.cwd(),
        encoding: 'utf8',
        stdio: this.verbose ? 'inherit' : 'pipe'
      });

      if (!this.verbose && output) {
        // Show summary even in non-verbose mode
        const lines = output.split('\n');
        const summaryLines = lines.filter(line => 
          line.includes('Test Files') || 
          line.includes('Tests') || 
          line.includes('passed') ||
          line.includes('failed')
        );
        summaryLines.forEach(line => console.log(`   ${line}`));
      }
    } catch (error: any) {
      if (error.stdout) {
        console.log(error.stdout);
      }
      if (error.stderr) {
        console.error(error.stderr);
      }
      throw new Error(`Test suite failed with exit code ${error.status}`);
    }
  }

  private buildVitestCommand(pattern: string): string {
    let command = 'npx vitest';
    
    if (!this.watch) {
      command += ' --run';
    }

    if (this.coverage) {
      command += ' --coverage';
    }

    // Add pattern
    command += ` "${pattern}"`;

    // Add additional options
    command += ' --reporter=verbose';
    
    return command;
  }

  private printSummary(
    results: Array<{ suite: string; passed: boolean; error?: string }>,
    totalPassed: number,
    totalFailed: number
  ): void {
    console.log('\n' + '='.repeat(60));
    console.log('📋 TEST SUMMARY');
    console.log('='.repeat(60));

    results.forEach(result => {
      const status = result.passed ? '✅' : '❌';
      console.log(`${status} ${result.suite}`);
      if (!result.passed && result.error) {
        console.log(`   Error: ${result.error}`);
      }
    });

    console.log('\n' + '-'.repeat(60));
    console.log(`Total Test Suites: ${results.length}`);
    console.log(`Passed: ${totalPassed}`);
    console.log(`Failed: ${totalFailed}`);
    console.log('-'.repeat(60));

    if (totalFailed === 0) {
      console.log('🎉 All tests passed!');
    } else {
      console.log(`💥 ${totalFailed} test suite(s) failed`);
    }

    if (this.coverage) {
      console.log('\n📊 Coverage report generated in coverage/ directory');
    }
  }

  async runSpecificTests(patterns: string[]): Promise<void> {
    console.log(`🧪 Running specific test patterns: ${patterns.join(', ')}\n`);

    for (const pattern of patterns) {
      console.log(`\n🔍 Running tests matching: ${pattern}`);
      
      try {
        const command = this.buildVitestCommand(pattern);
        execSync(command, { 
          cwd: process.cwd(),
          stdio: 'inherit'
        });
        console.log(`✅ Tests for ${pattern} completed successfully\n`);
      } catch (error) {
        console.log(`❌ Tests for ${pattern} failed\n`);
        throw error;
      }
    }
  }

  async validateTestEnvironment(): Promise<void> {
    console.log('🔧 Validating test environment...\n');

    // Check if vitest is installed
    try {
      execSync('npx vitest --version', { stdio: 'pipe' });
      console.log('✅ Vitest is installed');
    } catch (error) {
      throw new Error('❌ Vitest is not installed. Run: npm install');
    }

    // Check if test files exist
    const testFiles = [
      'src/services/backend-api-client.test.ts',
      'src/services/streaming-response-handler.test.ts',
      'src/services/tool-approval-handler.test.ts',
      'src/services/conversation-manager.test.ts',
      'src/commands/utility/lenza-new.test.ts',
      'src/commands/utility/lenza-resume.test.ts',
      'src/errors/agent-error.test.ts',
      'src/utils/network-error-handler.test.ts'
    ];

    let missingFiles = 0;
    testFiles.forEach(file => {
      if (existsSync(path.join(process.cwd(), file))) {
        console.log(`✅ ${file}`);
      } else {
        console.log(`❌ ${file} (missing)`);
        missingFiles++;
      }
    });

    if (missingFiles > 0) {
      throw new Error(`❌ ${missingFiles} test files are missing`);
    }

    console.log('\n✅ Test environment validation completed\n');
  }
}

// CLI interface
async function main() {
  const args = process.argv.slice(2);
  const options = {
    verbose: args.includes('--verbose') || args.includes('-v'),
    coverage: args.includes('--coverage') || args.includes('-c'),
    watch: args.includes('--watch') || args.includes('-w')
  };

  const runner = new TestRunner(options);

  try {
    if (args.includes('--validate')) {
      await runner.validateTestEnvironment();
      return;
    }

    // Check for specific test patterns
    const patternArgs = args.filter(arg => !arg.startsWith('--') && !arg.startsWith('-'));
    
    if (patternArgs.length > 0) {
      await runner.runSpecificTests(patternArgs);
    } else {
      await runner.validateTestEnvironment();
      await runner.runAllTests();
    }
  } catch (error) {
    console.error('\n💥 Test runner failed:', error);
    process.exit(1);
  }
}

// Run if called directly
if (require.main === module) {
  main();
}

export { TestRunner };