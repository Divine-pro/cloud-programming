# Applied Cloud Programming (ACP) - Coursework Repository

This repository contains all practical coursework and tutorial exercises for the Applied Cloud Programming module at the University of Edinburgh.

## 🛠️ Technology Stack
* **Java 21** / Spring Boot 3.2.3
* **Docker & Docker Compose**
* **Relational Database:** PostgreSQL
* **NoSQL & Object Storage:** AWS DynamoDB & AWS S3 (Simulated via LocalStack)

## 📂 Repository Structure

### Coursework_0: Fundamentals
Introductory tutorials, Docker basics, and Spring Boot demos (Tutorial 1).

---

### Coursework_1: Drone Data Pipeline (Completed)
A cloud-native Spring Boot microservice integrating an external Azure REST API with local and cloud-simulated databases. 

**How to Run Coursework 1:**
1. Open a terminal in the project root (where `docker-compose.yml` is located).
2. Start the database and LocalStack infrastructure: 
   ```bash
   docker-compose up -d
Run the Spring Boot application (Cw1ServiceApplication.java). The server will start on http://localhost:8080.

API Endpoints:

POST /api/v1/acp/process/dump: The main ETL pipeline. Fetches Azure drone data, calculates costs, archives to S3, saves to Postgres, and logs to DynamoDB.

GET /api/v1/acp/drones: Returns all processed drones from PostgreSQL.

GET /api/v1/acp/s3/list: Lists all archived files in the S3 bucket.

GET /api/v1/acp/s3/read/{key}: Retrieves a specific raw JSON file from S3.

GET /api/v1/acp/dynamo/all: Retrieves all system execution logs from DynamoDB.

POST /api/v1/acp/s3/dump: Migrates individual drone records from Postgres to S3.

POST /api/v1/acp/dynamo/dump: Syncs individual drone records from Postgres to DynamoDB.
