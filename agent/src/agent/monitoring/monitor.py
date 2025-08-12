#!/usr/bin/env python3
"""
Monitoring script for the agent tool synchronization system.

This script can be run periodically to log system metrics and health status.
It's designed to be used by monitoring systems or cron jobs to track
the health and performance of the agent's tool synchronization features.

Usage:
    python -m src.agent.monitoring.monitor [--metrics] [--health] [--reset]
"""

import argparse
import asyncio
import logging
import sys
from pathlib import Path

# Add the src directory to the path so we can import modules
sys.path.insert(0, str(Path(__file__).parent.parent.parent))

from src.agent.monitoring.metrics_service import get_metrics_service
from src.agent.monitoring.health_check import get_health_checker, log_system_metrics, log_system_health

logger = logging.getLogger(__name__)


async def main():
    """Main monitoring function."""
    parser = argparse.ArgumentParser(description="Agent tool synchronization monitoring")
    parser.add_argument("--metrics", action="store_true", help="Log system metrics summary")
    parser.add_argument("--health", action="store_true", help="Log system health status")
    parser.add_argument("--reset", action="store_true", help="Reset all metrics counters")
    parser.add_argument("--full-check", action="store_true", help="Perform full health check")
    parser.add_argument("--verbose", "-v", action="store_true", help="Enable verbose logging")
    
    args = parser.parse_args()
    
    # Configure logging
    log_level = logging.DEBUG if args.verbose else logging.INFO
    logging.basicConfig(
        level=log_level,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    # If no specific action is requested, do metrics and health
    if not any([args.metrics, args.health, args.reset, args.full_check]):
        args.metrics = True
        args.health = True
    
    try:
        if args.reset:
            logger.info("Resetting system metrics")
            metrics_service = get_metrics_service()
            metrics_service.reset_metrics()
            logger.info("Metrics reset completed")
        
        if args.metrics:
            logger.info("Logging system metrics")
            log_system_metrics()
        
        if args.health:
            logger.info("Logging system health")
            log_system_health()
        
        if args.full_check:
            logger.info("Performing full health check")
            health_checker = get_health_checker()
            health_results = await health_checker.perform_health_check()
            
            print(f"Overall Status: {health_results['overall_status']}")
            print(f"Check Duration: {health_results['check_duration_ms']:.2f}ms")
            
            if health_results['issues']:
                print(f"Issues: {', '.join(health_results['issues'])}")
            
            print("\nComponent Status:")
            for component, status in health_results['components'].items():
                print(f"  {component}: {status['status']}")
                if status['status'] != 'healthy' and 'error' in status:
                    print(f"    Error: {status['error']}")
            
            # Print key metrics
            metrics = health_results['metrics']
            if 'system_metrics' in metrics:
                sys_metrics = metrics['system_metrics']
                print(f"\nKey Metrics:")
                print(f"  Tool discoveries: {sys_metrics.get('tool_discoveries_total', 0)}")
                print(f"  Tools added: {sys_metrics.get('tools_added_total', 0)}")
                print(f"  Tools removed: {sys_metrics.get('tools_removed_total', 0)}")
                print(f"  Redis messages published: {sys_metrics.get('redis_messages_published', 0)}")
                print(f"  Redis messages failed: {sys_metrics.get('redis_messages_failed', 0)}")
        
        logger.info("Monitoring completed successfully")
        
    except Exception as e:
        logger.error("Error during monitoring", exc_info=True)
        sys.exit(1)


if __name__ == "__main__":
    asyncio.run(main())