# Monitoring Setup with Grafana and Prometheus

## Overview
This setup includes Prometheus for metrics collection and Grafana for visualization of your Spring Boot and Python Flask applications.

## Services
- **Prometheus**: `http://localhost:9090` - Metrics collection and storage
- **Grafana**: `http://localhost:3000` - Metrics visualization
- **Spring App**: `http://localhost:8080` - Main application
- **Python Service**: `http://localhost:5000` - Image processing service

## Quick Start

### 1. Start All Services
```bash
docker-compose up -d
```

### 2. Access Grafana
1. Open browser to `http://localhost:3000`
2. Login with:
   - Username: `admin`
   - Password: `admin`
3. You'll be prompted to change password (or skip)

### 3. Configure Prometheus Data Source in Grafana
1. Go to **Configuration** → **Data Sources**
2. Click **Add data source**
3. Select **Prometheus**
4. Set URL to: `http://prometheus:9090`
5. Click **Save & Test**

### 4. Import Dashboards

#### For Spring Boot Application:
1. Go to **Dashboards** → **Import**
2. Enter dashboard ID: `4701` (JVM Micrometer)
3. Select Prometheus data source
4. Click **Import**

Or use dashboard ID `12900` for Spring Boot 2.1+ statistics

#### For Custom Metrics:
Create a new dashboard and add panels with queries like:

**Spring Boot Metrics:**
- `http_server_requests_seconds_count{application="kolor-spring"}` - Request count
- `http_server_requests_seconds_sum{application="kolor-spring"}` - Request duration
- `jvm_memory_used_bytes{application="kolor-spring"}` - Memory usage
- `process_cpu_usage{application="kolor-spring"}` - CPU usage

**Python Service Metrics:**
- `flask_http_request_total{application="kolor-python"}` - Request count
- `flask_http_request_duration_seconds{application="kolor-python"}` - Request duration
- `flask_http_request_exceptions_total{application="kolor-python"}` - Error count

## Available Endpoints

### Spring Boot (Port 8080)
- Application: `http://localhost:8080/`
- Health: `http://localhost:8080/actuator/health`
- Prometheus Metrics: `http://localhost:8080/actuator/prometheus`
- All Metrics: `http://localhost:8080/actuator/metrics`

### Python Service (Port 5000)
- Prometheus Metrics: `http://localhost:5000/metrics`

### Prometheus (Port 9090)
- UI: `http://localhost:9090`
- Targets: `http://localhost:9090/targets`
- Query: `http://localhost:9090/graph`

## Verify Metrics Collection

### Check Prometheus Targets
1. Go to `http://localhost:9090/targets`
2. Verify all targets are **UP**:
   - spring-app (8080)
   - python-service (5000)
   - prometheus (9090)

### Test Queries in Prometheus
Try these queries in Prometheus UI:
```promql
# See all metrics
{job="spring-app"}
{job="python-service"}

# HTTP requests
rate(http_server_requests_seconds_count[5m])
rate(flask_http_request_total[5m])
```

## Troubleshooting

### Targets are DOWN
- Check if services are running: `docker-compose ps`
- Check logs: `docker-compose logs <service-name>`
- Verify network connectivity between containers

### No Data in Grafana
- Verify Prometheus data source is configured correctly
- Check that metrics endpoints are accessible
- Wait a few minutes for data to be collected

### Python Service Not Exporting Metrics
- Rebuild containers: `docker-compose up -d --build`
- Check requirements.txt includes `prometheus-flask-exporter`

## Stopping Services
```bash
docker-compose down
```

## Stopping and Removing Data
```bash
docker-compose down -v
```

## Custom Metrics

### Adding Custom Metrics to Spring Boot
Use Micrometer in your Java code:
```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@RestController
public class MyController {
    private final Counter myCounter;
    
    public MyController(MeterRegistry registry) {
        this.myCounter = Counter.builder("my_custom_metric")
            .description("Description of my metric")
            .register(registry);
    }
    
    @GetMapping("/endpoint")
    public String endpoint() {
        myCounter.increment();
        return "response";
    }
}
```

### Adding Custom Metrics to Python
The prometheus-flask-exporter automatically tracks:
- Request duration
- Request count by endpoint
- HTTP status codes
- Exceptions

For custom metrics, use:
```python
from prometheus_client import Counter, Histogram

# Custom counter
my_counter = Counter('my_custom_count', 'Description')
my_counter.inc()

# Custom histogram
my_histogram = Histogram('my_custom_duration', 'Description')
with my_histogram.time():
    # code to measure
    pass
```

## Useful Grafana Dashboard IDs
- `4701` - JVM (Micrometer)
- `12900` - Spring Boot 2.1 System Monitor
- `11159` - Spring Boot Statistics
- `10280` - Flask Application Monitor

## Advanced Configuration

### Retention Period
Prometheus default retention is 15 days. To change, modify `prometheus.yml` command:
```yaml
command:
  - '--storage.tsdb.retention.time=30d'
```

### Alerting
Configure alerting rules in Prometheus and set up notifications in Grafana.
