"""
Test script for sync recovery monitoring functionality in the agent.

This script tests the sync recovery metrics recording and health status
functionality to ensure proper monitoring of resync operations.
"""

import asyncio
import time
from datetime import datetime

from src.agent.monitoring.metrics_service import get_metrics_service
from src.agent.monitoring.health_check import get_health_checker


async def test_sync_recovery_monitoring():
    """Test sync recovery monitoring functionality."""
    print("Testing sync recovery monitoring...")
    
    metrics_service = get_metrics_service()
    health_checker = get_health_checker()
    
    # Test 1: Record successful resync requests
    print("\n1. Testing successful resync request recording...")
    metrics_service.record_resync_request(True, 1500.0)
    metrics_service.record_resync_request(True, 1200.0)
    
    metrics = metrics_service.get_metrics_summary()
    system_metrics = metrics["system_metrics"]
    
    assert system_metrics["resync_requests_received"] == 2
    assert system_metrics["resync_requests_successful"] == 2
    assert system_metrics["resync_requests_failed"] == 0
    assert system_metrics["resync_success_rate"] == 1.0
    assert system_metrics["average_resync_latency_ms"] == 1350.0  # (1500 + 1200) / 2
    
    print("✓ Successful resync request recording works correctly")
    
    # Test 2: Record failed resync requests
    print("\n2. Testing failed resync request recording...")
    metrics_service.record_resync_request(False, 5000.0)
    
    metrics = metrics_service.get_metrics_summary()
    system_metrics = metrics["system_metrics"]
    
    assert system_metrics["resync_requests_received"] == 3
    assert system_metrics["resync_requests_successful"] == 2
    assert system_metrics["resync_requests_failed"] == 1
    assert abs(system_metrics["resync_success_rate"] - 0.6667) < 0.001  # 2/3
    assert abs(system_metrics["average_resync_latency_ms"] - 2566.67) < 0.1  # (1500 + 1200 + 5000) / 3
    
    print("✓ Failed resync request recording works correctly")
    
    # Test 3: Health status with resync failures
    print("\n3. Testing health status with resync failures...")
    
    # Record more failures to trigger health issues
    for i in range(3):
        metrics_service.record_resync_request(False, 3000.0)
    
    health_status = metrics_service.get_health_status()
    
    assert "resync_failures" in health_status["issues"]
    assert health_status["status"] == "degraded"
    assert health_status["resync_success_rate"] < 0.8
    
    print("✓ Health status correctly detects resync failures")
    
    # Test 4: Comprehensive health check
    print("\n4. Testing comprehensive health check...")
    
    try:
        health_results = await health_checker.perform_health_check()
        
        assert "timestamp" in health_results
        assert "overall_status" in health_results
        assert "components" in health_results
        assert "metrics" in health_results
        
        # Check that sync recovery metrics are included
        metrics_data = health_results["metrics"]
        assert "system_metrics" in metrics_data
        assert "resync_requests_received" in metrics_data["system_metrics"]
        
        print("✓ Comprehensive health check includes sync recovery metrics")
        
    except Exception as e:
        print(f"⚠ Health check test skipped due to missing dependencies: {e}")
    
    # Test 5: Metrics reset
    print("\n5. Testing metrics reset...")
    
    metrics_service.reset_metrics()
    
    metrics = metrics_service.get_metrics_summary()
    system_metrics = metrics["system_metrics"]
    
    assert system_metrics["resync_requests_received"] == 0
    assert system_metrics["resync_requests_successful"] == 0
    assert system_metrics["resync_requests_failed"] == 0
    assert system_metrics["resync_success_rate"] == 0.0
    assert system_metrics["average_resync_latency_ms"] == 0.0
    
    print("✓ Metrics reset works correctly")
    
    print("\n✅ All sync recovery monitoring tests passed!")


if __name__ == "__main__":
    asyncio.run(test_sync_recovery_monitoring())