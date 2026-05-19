# HRMS Backend Microservices

## Overview

HRMS Backend is a Microservices-based Human Resource Management System developed using Java, Spring Boot, Spring Cloud, and MySQL.

The project manages:

* Employee Management
* Leave Management
* Attendance Tracking
* Payroll Management
* Report Generation
* Internal Service Coordination

The architecture is fully distributed using Spring Boot Microservices.

---

# Technologies Used

## Backend

* Java 17
* Spring Boot 3.x
* Spring Cloud
* Spring Data JPA
* Hibernate
* Maven

## Database

* MySQL 8

## Microservices Tools

* Eureka Server
* API Gateway
* Config Server
* OpenFeign

## Other Libraries

* Lombok
* iText PDF
* Java Mail Sender

---

# Microservices and Ports

| Service          | Port | Database         |
| ---------------- | ---- | ---------------- |
| employeeService  | 8088 | employee_hrm_db  |
| leaveService     | 8087 | leave_hrm_db     |
| attendaceService | 8085 | attendace_hrm_db |
| payrollService   | 8089 | payroll_hrm_db   |
| mappingService   | 8090 | hrm_mapping_db   |
| reportService    | 8091 | report_hrm_db    |

---

# Service Details

## employeeService

### Responsibilities

* Add Employee
* Update Employee
* Delete Employee
* Employee Profile Upload
* Employee Search

### Features

* File Upload Support
* Gmail Notification Integration
* MySQL Integration

### Important Configurations

```properties
server.port=8088
spring.datasource.url=jdbc:mysql://localhost:3306/employee_hrm_db
file.upload-dir=./uploads/profiles
```

---

## leaveService

### Responsibilities

* Leave Apply
* Leave Approval
* Leave Rejection
* Leave Tracking

### Features

* Configurable Leave Types
* Holiday Management
* Monthly Paid Leave Limits

### Important Configurations

```properties
server.port=8087
spring.datasource.url=jdbc:mysql://localhost:3306/leave_hrm_db
```

### Leave Types

```properties
hrm.leave.types=SICK:50,PAID:2,CASUAL:7,UNPAID:365,WFH:365
```

---

## attendaceService

### Responsibilities

* Daily Attendance Tracking
* Attendance History
* Attendance Reports

### Features

* Mail Notifications
* Attendance Database Management

### Important Configurations

```properties
server.port=8085
spring.datasource.url=jdbc:mysql://localhost:3306/attendace_hrm_db
```

---

## payrollService

### Responsibilities

* Payroll Processing
* Salary Management
* Payslip Generation

### Features

* Employee Service Integration
* Mail Notifications
* Payroll Calculations

### Important Configurations

```properties
server.port=8089
spring.datasource.url=jdbc:mysql://localhost:3306/payroll_hrm_db
employee.service.url=http://localhost:8088/api/employees
```

---

## mappingService

### Responsibilities

* Internal Service Mapping
* Service Coordination
* Relationship Management

### Important Configurations

```properties
server.port=8090
spring.datasource.url=jdbc:mysql://localhost:3306/hrm_mapping_db
```

---

## reportService

### Responsibilities

* PDF Report Generation
* Attendance Reports
* Payroll Reports
* Employee Reports

### Features

* iText PDF Integration
* Report Export Functionality

### Important Configurations

```properties
server.port=8091
spring.datasource.url=jdbc:mysql://localhost:3306/report_hrm_db
```

---

# Common Email Configuration

Some services use Gmail SMTP for sending notifications.

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

---

# Project Structure

```text
HRMS-BACKEND/
│
├── employeeService/
├── leaveService/
├── attendaceService/
├── payrollService/
├── mappingService/
├── reportService/
├── api-gateway/
├── config-server/
├── eureka-server/
└── uploads/
```

---

# Prerequisites

Install before running:

* Java 17
* Maven 3+
* MySQL 8
* IntelliJ IDEA / VS Code
* Git

Verify Installation:

```bash
java -version
mvn -version
mysql --version
```

---

# Database Setup

Create all required databases.

```sql
CREATE DATABASE employee_hrm_db;
CREATE DATABASE leave_hrm_db;
CREATE DATABASE attendace_hrm_db;
CREATE DATABASE payroll_hrm_db;
CREATE DATABASE hrm_mapping_db;
CREATE DATABASE report_hrm_db;
```

---

# How to Run the Project

## Step 1: Clone Repository

```bash
git clone <repository-url>
cd HRMS-BACKEND
```

---

# Run Order

Start services in this order:

1. Eureka Server
2. Config Server
3. Business Services
4. API Gateway

---

## Step 2: Start Eureka Server

```bash
cd eureka-server
mvn spring-boot:run
```

### Eureka Dashboard

```text
http://localhost:8761
```

---

## Step 3: Start Config Server

```bash
cd config-server
mvn spring-boot:run
```

---

## Step 4: Start employeeService

```bash
cd employeeService
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8088
```

---

## Step 5: Start leaveService

```bash
cd leaveService
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8087
```

---

## Step 6: Start attendaceService

```bash
cd attendaceService
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8085
```

---

## Step 7: Start payrollService

```bash
cd payrollService
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8089
```

---

## Step 8: Start mappingService

```bash
cd mappingService
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8090
```

---

## Step 9: Start reportService

```bash
cd reportService
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8091
```

---

## Step 10: Start API Gateway

```bash
cd api-gateway
mvn spring-boot:run
```

---

# API Testing

Use Postman.

Example APIs:

```http
GET http://localhost:8088/api/employees
```

```http
POST http://localhost:8087/api/leaves/apply
```

```http
GET http://localhost:8085/api/attendance
```

```http
GET http://localhost:8089/api/payroll
```

---

# Common Issues

## Java Version Error

If you get:

```text
release version 23 not supported
```

Use Java 17 in all pom.xml files.

```xml
<java.version>17</java.version>
```

Also configure IntelliJ SDK to Java 17.

---

## Port Already In Use

Update port in:

```properties
server.port=8082
```

---

## MySQL Connection Error

Check:

* MySQL running
* Database exists
* Username/password correct

---

# Future Improvements

* JWT Authentication
* Role-Based Access Control
* Docker Support
* Kubernetes Deployment
* Kafka Integration
* Redis Caching
* CI/CD Pipeline
* Monitoring with Grafana & Prometheus

---

# Author

Akash Pawale

Java Full Stack Developer

---

# License

This project is for learning and development purposes.
