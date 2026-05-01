# LogSignals

LogSignals is a backend log monitoring and anomaly detection system built with Java, Spring Boot, and PostgreSQL. It enables external applications to securely submit logs using API keys, supports both file-based and live log ingestion, detects anomalies grouped by service and error code, stores analysis history, and sends email alerts when anomalies are detected.

---

## Why This Project

Modern applications generate large volumes of logs, but raw log data alone is difficult to monitor manually. LogSignals was built to solve that by providing:

- secure application-level log ingestion
- anomaly detection based on service and error code patterns
- persistent analysis history for investigation
- email-based alerting for operational visibility

This project demonstrates practical backend engineering concepts including authentication, API key management, relational schema design, persistent storage, request-driven processing with anomaly-triggered email alerts.

---

## Features

- User registration and login
- Automatic application creation during registration
- API key generation and hashed API key storage
- Secure API key based ingestion for client applications
- File-based log analysis
- Live log ingestion
- Anomaly detection grouped by service and error code
- PostgreSQL-backed persistence for users, applications, API keys, analysis runs, logs, and anomalies
- Email alerting for detected anomalies
- Historical tracking of analysis runs and anomaly records

---

## Tech Stack

- Java
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Maven
- REST APIs
- Swagger / OpenAPI
- JUnit
- Lombok
- JavaMailSender

  
---

## System Architecture

```mermaid
flowchart LR
    U[User] --> R[Register / Login]
    U --> C[Owns Client Application]
    R --> K[Generate API Key]
    C -->|Uses API Key| L[Send Logs]
    L --> A[Analyze Logs]
    A --> D[PostgreSQL]
    A --> E[Email Alert]
```
---

##  Registration Flow

```mermaid
sequenceDiagram
    participant U as User
    participant API as Auth API
    participant DB as PostgreSQL

    U->>API: POST /auth/register
    API->>DB: Save user
    API->>DB: Create application
    API->>DB: Save hashed API key
    API-->>U: userId, applicationId, raw API key

```

---

## Database Relationships

```mermaid
erDiagram
    APP_USERS ||--o{ APPLICATIONS : owns
    APPLICATIONS ||--o{ APPLICATION_API_KEYS : has
    APPLICATIONS ||--o{ LOG_ANALYSIS_RUNS : creates
    APPLICATIONS ||--o{ LOGS : contains
    APPLICATIONS ||--o{ ANOMALIES : produces
    LOG_ANALYSIS_RUNS ||--o{ LOGS : stores
    LOG_ANALYSIS_RUNS ||--o{ ANOMALIES : detects

    APP_USERS {
        bigint user_id PK
        text name
        text email
        text password_hash
        timestamptz created_at
    }

    APPLICATIONS {
        bigint app_id PK
        bigint owner_user_id FK
        text name
        text description
        timestamptz created_at
    }

    APPLICATION_API_KEYS {
        bigint id PK
        bigint application_id FK
        text key_hash
        text key_prefix
        text name
        timestamptz revoked_at
        timestamptz created_at
    }

    LOG_ANALYSIS_RUNS {
        bigint log_run_id PK
        bigint application_id FK
        text status
        text message
        int total_lines
        int invalid_lines
        timestamptz created_at
    }

    LOGS {
        bigint log_id PK
        bigint analysis_run_id FK
        bigint application_id FK
        text service_name
        text hostname
        text error_code
        text level
        text message
        timestamptz occurred_at
        text raw_line
        timestamptz created_at
    }

    ANOMALIES {
        bigint anomaly_id PK
        bigint analysis_run_id FK
        bigint application_id FK
        text service_name
        text error_code
        timestamptz occurred_at
        timestamptz created_at
    }

```
---
## Anomaly Detection Logic

LogSignals uses a rolling threshold based anomaly detection approach rather than a fixed error count rule.

The system groups logs by:
- service
- error code
- minute

It then counts how many times the same error occurs in each minute. To decide whether the current count is anomalous, the system compares it with recent previous counts for the same service and error code.

A dynamic threshold is calculated using the recent mean and standard deviation:

`threshold = mean + (k × standard deviation)`

In the current implementation:
- `k = 2`
- minimum standard deviation = `1`

This means the system does not treat every repeated error as anomalous. Instead, it checks whether the current error spike is significantly higher than the recent normal pattern.

For example, if a service had 2 repeated errors in an earlier minute and later shows 4 repeated errors, the later minute may be flagged as anomalous if it crosses the calculated rolling threshold.

---

## API Endpoints
1.Authentication
- POST /auth/register
- POST /auth/login
  
2.Logs
- POST /logs/analyze
- POST /logs/ingest

## Sample Requests
# Register
```json
{
  "name": "Nirmala",
  "email": "nirmala@example.com",
  "password": "password123",
  "applicationName": "Payment Service",
  "applicationDescription": "Logs from payment service"
}
```
# Live Log Ingestion
Header:
```text
X-API-Key: <your-api-key>
```
Body:
```json
{
  "timestamp": "2026-04-28T10:00:00Z",
  "level": "ERROR",
  "service": "payment-service",
  "errorCode": "ERR_500",
  "message": "Payment failed"
}
```
## Project Structure
```text
src/main/java/com/nirmala/logsense
├── config
├── controller
├── dto
├── entity
├── model
├── repository
├── service
└── util
```
---

# How to Run

## Requirements
- Java 25
- Maven
- PostgreSQL

## Optional Tools
- IntelliJ IDEA (or any IDE)
- pgAdmin / psql (for database management)

# Steps
1. Clone the repository
``` bash
git clone https://github.com/nirmala-sharma/LogSignals.git
```
2. Create database
``` sql
CREATE DATABASE log_monitoring;
```
3. Run the SQL query from
``` text
database/schema.sql
```
4. Configure application.properties
``` properties
 spring.datasource.url=jdbc:postgresql://localhost:5432/log_monitoring
 spring.datasource.username=your_db_username
 spring.datasource.password=your_db_password

 spring.datasource.driver-class-name=org.postgresql.Driver

 spring.jpa.hibernate.ddl-auto=validate
 spring.jpa.show-sql=true
 spring.jpa.properties.hibernate.format_sql=true

 spring.mail.host=smtp.gmail.com
 spring.mail.port=587
 spring.mail.username=${MAIL_USERNAME}
 spring.mail.password=${MAIL_PASSWORD}
 spring.mail.properties.mail.smtp.auth=true
 spring.mail.properties.mail.smtp.starttls.enable=true
```
5. Set IntelliJ environment variables for the mail
``` text
MAIL_USERNAME=your_email@gmail.com;MAIL_PASSWORD=your_google_app_password
```
6. Run the Spring Boot application
   For that first select the main app which is LogSenseApplication and then hit Run button.

## How to test
1. Register user

2. Copy returned API key

3. Login user

4. Test /logs/ingest

5. Test /logs/analyze

6. Check PostgreSQL tables

7. Check email inbox

---
## Demo Screenshots

### Swagger UI

![Swagger UI](docs/images/swagger_ui.png)


### Register API Response

![Register Response](docs/images/register_api.png)


### Live Log Ingestion Response

![Live Ingestion Response](docs/images/live_ingest_response.png)


### Email Alert
![Email Alert](docs/images/email_alert.png)


## Learning Opportunities

This project can serve as a practical learning resource for beginner backend developers. By exploring or contributing to LogSignals, contributors can gain hands-on experience with:

- layered Spring Boot backend architecture
- REST API design and request/response handling
- JPA entity mapping and database persistence
- relational schema design with PostgreSQL
- API key based application authentication
- dependency injection in Spring Boot
- global exception handling
- unit testing for core backend logic
- log parsing, aggregation, and anomaly detection workflows
- email integration and environment-based configuration

It is a good project for understanding how multiple backend components work together in a real-world monitoring workflow.

## Contribution Ideas

Contributions are welcome in both beginner-friendly and advanced areas.

### Beginner-Friendly

- improve README and project documentation
- add more unit tests
- improve validation and error messages
- clean up naming and code readability
- add more sample log datasets
- improve Swagger/OpenAPI documentation

### Intermediate

- add history retrieval APIs for logs and anomalies
- improve severity classification logic
- add better filtering and search support
- enhance email alert formatting
- add alert throttling to avoid duplicate notifications

### Advanced

- add JWT-based authentication
- support multiple alert recipients per application
- make anomaly thresholds configurable per service
- add a dashboard or frontend UI
- containerize the project using Docker
- add role-based access control
- integrate asynchronous alert processing


   

  

