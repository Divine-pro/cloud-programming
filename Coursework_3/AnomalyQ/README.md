# AnomalyQ — Real-time Algorithmic Trading Risk Monitor
### ACP Coursework 3 | s2845427

AnomalyQ is a real-time financial risk monitoring system built as a microservices architecture. It simulates live stock price feeds, detects anomalies using statistical thresholds and cross-stock correlation analysis, and displays everything on a live WebSocket dashboard.

---

## Architecture

```
simulator-service  →  Kafka (price-feed)  →  processor-service
                                                    ↓
                                          Redis (latest prices)
                                          DynamoDB (price history)
                                          RabbitMQ (alerts)
                                                    ↓
                                          dashboard-service
                                                    ↓
                                          WebSocket → Browser UI
```

**Three Spring Boot microservices:**
- **simulator-service** — generates realistic stock prices every second for AAPL, GOOGL, NVDA, MSFT, AMZN and publishes to Kafka
- **processor-service** — consumes Kafka stream, detects anomalies, stores in Redis/DynamoDB, sends alerts to RabbitMQ
- **dashboard-service** — reads Redis prices, listens to RabbitMQ alerts, pushes to browser via WebSocket

---

## Prerequisites

- Java 21
- Maven 3.8+
- Docker Desktop

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Divine-pro/cloud-programming.git
cd cloud-programming/Coursework_3/AnomalyQ
```

### 2. Build all services

```bash
mvn package
```

This builds all three services at once from the parent pom.

### 3. Start the application

```bash
docker-compose up --build
```

This will:
- Pull and start Kafka, Zookeeper, RabbitMQ, Redis and LocalStack (DynamoDB)
- Build and start all three microservices
- Wire everything together automatically

**First run takes 2-3 minutes** to pull Docker images.

### 4. Open the dashboard

Once all containers are running, open your browser and go to:

```
http://localhost:8082
```

You should see the live AnomalyQ dashboard with:
- Real-time stock price cards updating every second
- Live price history chart
- Alerts streaming in on the right panel
- SPIKE, CRASH, FLASH_CRASH and MARKET_RALLY events detected automatically

---

## Verifying Everything is Running

Check all containers are up:

```bash
docker ps
```

You should see 8 containers running:
- `anomalyq-simulator-service-1`
- `anomalyq-processor-service-1`
- `anomalyq-dashboard-service-1`
- `anomalyq-kafka-1`
- `anomalyq-zookeeper-1`
- `anomalyq-rabbitmq-1`
- `anomalyq-redis-1`
- `anomalyq-localstack-1`

Check logs for any service:

```bash
docker logs anomalyq-simulator-service-1
docker logs anomalyq-processor-service-1
docker logs anomalyq-dashboard-service-1
```

---

## Useful URLs

| Service | URL |
|---------|-----|
| Live Dashboard | http://localhost:8082 |
| RabbitMQ Management UI | http://localhost:15672 (guest/guest) |
| LocalStack (DynamoDB) | http://localhost:4566 |

---

## Stopping the Application

```bash
docker-compose down
```

To remove all data volumes as well:

```bash
docker-compose down -v
```

---

## Project Structure

```
AnomalyQ/
  docker-compose.yml
  pom.xml                          ← parent pom
  simulator-service/
    src/main/java/.../
      simulator/StockSimulator.java    ← price generation logic
      model/StockPrice.java
      config/SimulatorConfig.java
  processor-service/
    src/main/java/.../
      service/StockPriceProcessor.java ← Kafka consumer + main pipeline
      service/AnomalyDetector.java     ← detection logic
      service/DynamoDBService.java     ← persistence
      config/                          ← RabbitMQ, Redis, DynamoDB config
  dashboard-service/
    src/main/java/.../
      service/AlertListenerService.java ← RabbitMQ → WebSocket bridge
      controller/DashboardController.java ← REST API for prices
      config/WebSocketConfig.java
    src/main/resources/static/
      index.html                        ← live dashboard UI
```

---

## Technologies Used

- **Java 21** + **Spring Boot 3.5.13**
- **Apache Kafka** — real-time price streaming
- **RabbitMQ** — alert message delivery
- **Redis** — latest price caching
- **AWS DynamoDB** (via LocalStack) — price history storage
- **WebSocket** (STOMP) — live browser updates
- **Chart.js** — real-time price charts
- **Docker Compose** — full local orchestration

---

## AI Usage

Claude (Anthropic) was used as a coding assistant throughout development for code generation, debugging and architecture guidance. All design decisions, problem framing and implementation choices were made by the student.
