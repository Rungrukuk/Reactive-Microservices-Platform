# Reactive Microservices Platform

A production-grade microservices architecture demonstrating enterprise-level security protocols (mTLS, TLS, RBAC, CBAC) with optimized inter-service communication using RSocket and Protocol Buffers. This project proves that comprehensive security measures can be implemented without sacrificing performance or introducing unacceptable latency.

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [API Documentation](#api-documentation)
- [Security Model](#security-model)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)

## Architecture Overview

The platform consists of four microservices communicating over mTLS-secured RSocket connections:

<p align="center">
  <img src="images/Microservices.png" width="900"/>
</p>

### Service Responsibilities

#### **API Gateway** (Port 8443)
- Single HTTPS entry point for all client requests
- Validates access tokens and session IDs via Auth Service
- Routes authenticated requests to appropriate services
- Manages HTTP cookies (access token, session ID, refresh token)

#### **Auth Service** (Port 7000)
- Central authentication and authorization authority
- Issues and validates JWT tokens (access, refresh, service)
- Implements Role-Based Access Control (RBAC)
- Manages guest user flows
- Binds refresh tokens to devices using `Client-City` header
- Sends suspicious events to Monitoring Service

#### **User Service** (Port 7001)
- Domain-specific business logic for user management
- Creates and stores user profile details and addresses
- Demonstrates Claim Based Access Control(CBAC) in service-to-service authorization using service tokens
- Validates location data against predefined cities/countries
- Sends suspicious events to Monitoring Service

#### **Monitoring Service** (Port 7002)
- Receives fire-and-forget events from other services
- Logs suspicious activities to PostgreSQL
- Non-blocking event ingestion to avoid performance impact

## Key Features

### Security Features
- **Mutual TLS (mTLS)**: All inter-service communication uses client certificate authentication
- **TLS 1.3**: External HTTPS connections to API Gateway
- **JWT Authentication**: RS512-signed tokens with separate keys for access, refresh, and service tokens
- **Session Management**: Redis-backed sessions with access token + session ID binding
- **RBAC**: Role-based authorization (roles: GUEST_USER, USER)
- **CBAC**: Claim-based authorization using service tokens with destination claims
- **Refresh Token Security**: Device-bound refresh tokens with city-based validation
- **Suspicious Event Monitoring**: Automated logging of security events (failed logins, token theft attempts, etc.)

### Performance Optimizations
- **RSocket Protocol**: Binary, reactive communication with backpressure support
- **Protocol Buffers**: Efficient serialization (smaller payload than JSON)
- **Non-blocking I/O**: Fully reactive Spring WebFlux stack
- **Connection Reuse**: Persistent RSocket connections between services
- **Fire-and-Forget**: Monitoring events don't block request processing

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Runtime | Java | 25 |
| Framework | Spring Boot | 4.0.3 |
| Build Tool | Maven | 3.9.12 |
| Communication | Spring RSocket | - |
| Serialization | Protocol Buffers | 4.28.2 |
| Database | PostgreSQL | 18-alpine |
| Database | Redis | 8.2.4-alpine3.22 |
| Containerization | Docker & Docker Compose | - |
| Security | OpenSSL, Java Keytool | - |

## Prerequisites

Before running this project, ensure you have the following installed:

- **Docker** (version 20.10+)
- **Docker Compose** (version 2.0+)
- **Git** (for cloning the repository)
- **OpenSSL** (for certificate generation via setup.sh)
- **Java** (JDK 17+ or later, required for keytool)
- **curl** or **Postman** (for API testing)

> **Note**: Maven, PostgreSQL, and Redis do not need to be installed locally because they run inside Docker containers.

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/Rungrukuk/Reactive-Microservices-Platform.git
cd Reactive-Microservices-Platform
```

### 2. Generate Certificates and Configuration

The `setup.sh` script generates all required TLS certificates, JWT keys, and environment variables:

```bash
chmod +x setup.sh
./setup.sh
```

**What this does:**
- Creates a Certificate Authority (CA)
- Generates signed certificates for all services (redis, auth, user, api-gateway, monitoring)
- Creates PKCS12 keystores and a shared truststore
- Generates RSA-4096 key pairs for JWT signing (access, refresh, service tokens)
- Writes a `.env` file with all credentials and base64-encoded keys

### 3. Start All Services

```bash
docker compose up --build
```

**First startup will take 3-5 minutes** as Docker:
- Builds images for 4 Spring Boot services
- Pulls PostgreSQL and Redis images
- Initializes 4 databases with schemas and seed data
- Waits for health checks to pass

**You'll see:**
```
✓ postgres           Healthy
✓ redis              Healthy  
✓ monitoring-service Started
✓ auth-service       Started
✓ user-service       Started
✓ api-gateway        Started
```

### 4. Verify Services are Running

```bash
docker compose ps
```

All services should show status `Up` or `healthy`.

### 5. Test the API

The API Gateway is now accessible at `https://localhost:8443`. See [API Documentation](#api-documentation) for examples.

## API Documentation

All requests must include the `Client-City` header (simulates IP geolocation by the frontend). Except `https://localhost:8443/createUserDetails` endpoint cookies can be discarded for all other endpoints as authentication service will generate new guest user and check if guest user has enough permission for the original request.

### Base URL

```
https://localhost:8443
```

> **Note**: The API uses a self-signed certificate. In curl, use the `-k` flag to skip verification. In Postman, disable "SSL certificate verification" in Settings.

---

### 1. Create Guest Session

Creates a new guest user session.

**Endpoint:** `GET /`

**Headers:**
```http
Client-City: Baku
```

**curl Example:**
```bash
curl -k https://localhost:8443/ \
  --header 'Client-City: Baku' \
  -c cookies.txt -v
```

**Response:**
```json
{
  "userStatus": "AUTHORIZED_GUEST_USER"
}
```

**Sets Cookies:**
- `accessToken` - JWT access token (24h expiry)
- `sessionId` - Session identifier
- `refreshToken` - Not set for guest users

---

### 2. Register New User

Registers a new user account and upgrades from guest to authenticated user.

**Endpoint:** `POST /register`

**Headers:**
```http
Content-Type: application/json
Client-City: Baku
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "rePassword": "SecurePass123!"
}
```

**curl Example:**
```bash
curl -k https://localhost:8443/register \
  --header 'Content-Type: application/json' \
  --header 'Client-City: Baku' \
  -c cookies.txt -b cookies.txt \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!",
    "rePassword": "SecurePass123!"
  }'
```

**Response (Success):**
```json
{
  "userStatus": "AUTHORIZED_USER",
  "message": "User created successfully",
  "status": "OK"
}
```

**Sets Cookies:**
- `accessToken` - New JWT for authenticated user
- `sessionId` - New session ID
- `refreshToken` - Long-lived refresh token (7 days)

**Validation Rules:**
- Email must be valid format
- Password must be at least 8 characters, must include at least 1 lower-case, 1 upper-case, 1 special character, and 1 number
- Password and rePassword must match
- Email must be unique

---

### 3. Login Existing User

Authenticates an existing user.

**Endpoint:** `POST /login`

**Headers:**
```http
Content-Type: application/json
Client-City: Baku
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**curl Example:**
```bash
curl -k https://localhost:8443/login \
  --header 'Content-Type: application/json' \
  --header 'Client-City: Baku' \
  -c cookies.txt -b cookies.txt \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!"
  }'
```

**Response (Success):**
```json
{
  "userStatus": "AUTHORIZED_USER",
  "message": "Logged in successfully",
  "status": "OK"
}
```

**Response (Failure):**
```json
{
  "userStatus": "AUTHORIZED_GUEST_USER",
  "message": "Email or password is incorrect",
  "status": "BAD_REQUEST"
}
```

**Sets Cookies:**
- Same as registration (new tokens and session)

---

### 4. Create User Details

Saves user profile and address information. **Requires authentication** (must login or register first).

**Endpoint:** `POST /user-details`

**Headers:**
```http
Content-Type: application/json
Client-City: Baku
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "name": "John",
  "surname": "Doe",
  "phoneNumber": "+994501234567",
  "country": "AZE",
  "state": "",
  "city": "Baku",
  "postalCode": "AZ1000",
  "addressLine1": "123 Main Street",
  "addressLine2": "Apt 4B",
  "isDefault": "true"
}
```

**curl Example:**
```bash
curl -k https://localhost:8443/user-details \
  --header 'Content-Type: application/json' \
  --header 'Client-City: Baku' \
  -b cookies.txt \
  -d '{
    "email": "user@example.com",
    "name": "John",
    "surname": "Doe",
    "phoneNumber": "+994501234567",
    "country": "AZE",
    "state": "",
    "city": "Baku",
    "postalCode": "AZ1000",
    "addressLine1": "123 Main Street",
    "addressLine2": "Apt 4B",
    "isDefault": "true"
  }'
```

**Response (Success):**
```json
{
  "userStatus": "AUTHORIZED_USER",
  "message": "User details created successfully",
  "status": "OK"
}
```

**Response (Unauthorized):**
```json
{
  "userStatus": "UNAUTHORIZED_GUEST_USER"
}
```

**Available Locations:**

| Country | ISO3 | Cities | Postal Codes |
|---------|------|--------|--------------|
| Azerbaijan | AZE | Baku, Khirdalan, Ganja | AZ1000, AZ1001, AZ0101, AZ2000 |
| Turkey | TUR | Istanbul | TR34000, TR34010 |

---

### Request Flow Example

Complete workflow for a new user:

```bash
# 1. Create guest session
curl -k https://localhost:8443/ \
  --header 'Client-City: Baku' \
  -c cookies.txt

# 2. Register new account
curl -k https://localhost:8443/register \
  --header 'Content-Type: application/json' \
  --header 'Client-City: Baku' \
  -c cookies.txt -b cookies.txt \
  -d '{"email":"test@test.com","password":"Pass123!","rePassword":"Pass123!"}'

# 3. Create user details
curl -k https://localhost:8443/user-details \
  --header 'Content-Type: application/json' \
  --header 'Client-City: Baku' \
  -b cookies.txt \
  -d '{"email":"test@test.com","name":"Test","surname":"User","phoneNumber":"+994501234567","country":"AZE","city":"Baku","postalCode":"AZ1000","addressLine1":"Street 1"}'
```

## Security Model

### Authentication Flow

```
┌──────┐                    ┌─────────────┐                  ┌──────────────┐     ┌────────────────┐
│Client│                    │ API Gateway │                  │ Auth Service │     │ Domain Service │
└──┬───┘                    └──────┬──────┘                  └──────┬───────┘     └───────┬────────┘
   │                               │                                │                     │
   │  1. HTTPS Request             │                                │                     │
   │ + accessToken cookie          │                                │                     │
   │ + sessionId cookie            │                                │                     │
   │ + Client-City header          │                                │                     │
   │──────────────────────────────>│                                │                     │
   │                               │                                │                     │
   │                               │  2. Validate Token + Session   │                     │
   │                               │  (RSocket mTLS)                │                     │
   │                               │───────────────────────────────>│                     │
   │                               │                                │                     │
   │                               │  3. AuthResponse               │                     │
   │                               │     + serviceToken             │                     │
   │                               │<───────────────────────────────│                     │
   │                               │                                                      │
   │                               │                4. Forward to Service                 │
   │                               │                    (RSocket mTLS)                    │
   │                               │─────────────────────────────────────────────────────>│
   │                               │                     Response                         │
   │  5. Response + Set-Cookie     │<─────────────────────────────────────────────────────│
   │<──────────────────────────────│                                                      
```

### Token Types

| Token Type | Lifetime | Purpose | Storage |
|------------|----------|---------|---------|
| **Access Token** | 24 hours | Authenticates user to API Gateway | HTTP-only cookie |
| **Session ID** | 24 hours | Binds to access token in Redis | HTTP-only cookie |
| **Refresh Token** | 7 days | Rotates access token when expired | HTTP-only cookie |
| **Service Token** | 5 minutes | Authorizes inter-service calls | Generated on-the-fly |

### Service Token Claims

Service tokens are short-lived JWT tokens created by Auth Service and sent to downstream services. They contain:

```json
{
  "sub": "user-uuid",
  "role": "USER",
  "services": ["USER_SERVICE"],
  "destinations": ["CREATE_USER_DETAILS"],
  "iat": 1234567890,
  "exp": 1234568190
}
```

The target service validates the service token to ensure the request is authorized for that specific operation.

### Refresh Token Device Binding

Refresh tokens are bound to a device using:
- `userId` - The authenticated user
- `userAgent` - Browser/client identifier
- `clientCity` - Geographic location from IP address

If any of these change, the refresh token is invalidated and the user must re-authenticate. This prevents stolen refresh tokens from being used on different devices or locations.

### Role-Based Access Control (RBAC)

| Role | Permissions |
|------|-------------|
| **GUEST_USER** | Can access `/`, `/register`, `/login` |
| **USER** | Can access `/`, `/user-details` |

Permissions are stored in PostgreSQL and checked by Auth Service before issuing service tokens.

### Mutual TLS (mTLS)

All services authenticate to each other using X.509 certificates:

1. Each service has a private key and certificate signed by a common CA
2. Services present their certificate during connection
3. Peer certificates are validated against the truststore containing the CA certificate
4. Connections fail if certificate validation fails (`client-auth: need`)

This prevents unauthorized services from joining the network.

## Project Structure

```
Microservices-Platform/
├── api_gateway/
│   ├── .mvn/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/platform/api_gateway/
│   │   │   │   ├── config/
│   │   │   │   ├── controller/
│   │   │   │   ├── security/
│   │   │   │   ├── service/
│   │   │   │   ├── util/
│   │   │   │   └── ApiGatewayApplication.java
│   │   │   ├── proto/
│   │   │   └── resources/
│   │   └── test/
│   ├── .gitignore
│   ├── Dockerfile
│   ├── mvnw
│   ├── mvnw.cmd
│   └── pom.xml
│
├── auth_service/
│   ├── .mvn/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/platform/auth_service/
│   │   │   │   ├── config/
│   │   │   │   ├── controller/
│   │   │   │   ├── domain/
│   │   │   │   ├── dto/
│   │   │   │   ├── repository/
│   │   │   │   ├── security/
│   │   │   │   ├── service/
│   │   │   │   ├── util/
│   │   │   │   └── AuthServiceApplication.java
│   │   │   ├── proto/
│   │   │   └── resources/
│   │   └── test/
│   ├── .gitignore
│   ├── Dockerfile
│   ├── mvnw
│   ├── mvnw.cmd
│   └── pom.xml
│
├── monitoring_service/
│   ├── .mvn/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/platform/monitoring_service/
│   │   │   │   ├── config/
│   │   │   │   ├── controller/
│   │   │   │   ├── domain/
│   │   │   │   ├── repository/
│   │   │   │   ├── service/
│   │   │   │   ├── util/
│   │   │   │   └── MonitoringServiceApplication.java
│   │   │   ├── proto/
│   │   │   └── resources/
│   │   └── test/
│   ├── .gitignore
│   ├── Dockerfile
│   ├── mvnw
│   ├── mvnw.cmd
│   └── pom.xml
│
├── user_service/
│   ├── .mvn/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/platform/user_service/
│   │   │   │   ├── config/
│   │   │   │   ├── controller/
│   │   │   │   ├── domain/
│   │   │   │   ├── dto/
│   │   │   │   ├── repository/
│   │   │   │   ├── service/
│   │   │   │   ├── util/
│   │   │   │   └── UserServiceApplication.java
│   │   │   ├── proto/
│   │   │   └── resources/
│   │   └── test/
│   ├── .gitignore
│   ├── Dockerfile
│   ├── mvnw
│   ├── mvnw.cmd
│   └── pom.xml
│
├── docker/
│   ├── postgres/
│   │   ├── 01-init-auth.sql
│   │   ├── 02-init-user.sql
│   │   ├── 03-init-location.sql
│   │   └── 04-init-log.sql
│   └── redis/
│       └── redis.conf
├── images/
│   └── Microservices.png
│
├── .gitignore
├── LICENSE.txt
├── docker-compose.yml
├── Readme.md
└── setup.sh
```

## Database Schema

The project uses **4 PostgreSQL databases**:

### 1. `auth` Database

Stores authentication and authorization data.

**Tables:**
- `users` - User accounts (email, hashed password, role)
- `roles` - Available roles (GUEST_USER, USER)
- `permissions` - Granular permissions (service + destination pairs)
- `role_permissions` - Many-to-many mapping of roles to permissions
- `refresh_tokens` - Refresh tokens bound to user + device + location

### 2. `user` Database

Stores user profile and address data.

**Tables:**
- `users` - User profiles (name, surname, phone)
- `addresses` - Physical addresses
- `user_addresses` - Links users to addresses (supports multiple addresses)

### 3. `location` Database

Reference data for locations (read-only seed data).

**Tables:**
- `countries` - Country list with ISO codes
- `states` - States/provinces per country
- `cities` - Cities per state
- `postal_codes` - Valid postal codes per city

### 4. `log` Database

Stores security monitoring events.

**Tables:**
- `monitoring_events` - Suspicious activities logged by services
  - Indexed by: event_type, service_name, user_id, timestamp

## Contributing

This is an academic research project. For questions or suggestions, please open an issue.

## License

MIT License

## Acknowledgments

Built with Spring Boot, RSocket, Protocol Buffers, PostgreSQL, and Redis.
