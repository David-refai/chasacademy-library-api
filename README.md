# Library API

A secure, high-performance REST API for library management built with Spring Boot 3.

## Features

| Feature | Technology |
|---------|-----------|
| Authentication | JWT (JSON Web Tokens) |
| Security | Spring Security + CORS |
| Input Validation | Bean Validation (@NotNull, @Size, @Pattern) |
| Secrets Management | Spring Vault (HashiCorp Vault) |
| Caching | Redis + Spring Cache (@Cacheable) |
| Pagination | Spring Data Pageable |
| Rate Limiting | Bucket4j (Token Bucket Algorithm) |
| Circuit Breaker (VG) | Resilience4j |
| External API (VG) | Open Library API |

---

## Requirements

- Java 21+
- Maven 3.9+
- Docker + Docker Compose (for Redis and Vault)

---

## How to Run

### Step 1: Start Redis and Vault

```bash
docker-compose up -d
```

### Step 2: Store JWT Secret in Vault (optional)

If Vault is running, store the JWT secret there instead of using the default value:

```bash
export VAULT_ADDR='http://localhost:8200'
export VAULT_TOKEN='dev-root-token'

vault kv put secret/library-api \
  app.jwt.secret="my-super-secret-key-at-least-256-bits-long-for-security"
```

> If you skip this step, the app uses the default secret from `application.yml`.

### Step 3: Start the Application

```bash
mvn spring-boot:run
```

Application runs at: `http://localhost:8080`

---

## Default Users

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin1234` | ROLE_ADMIN |
| `user1` | `user1234` | ROLE_USER |

---

## API Usage

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin1234"}'
```

**Response:** Contains a `token` — copy it for use in all following requests.

### Get all books (with pagination)

```bash
curl "http://localhost:8080/api/books?page=0&size=5" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Get book by ID (cached in Redis after first call)

```bash
curl http://localhost:8080/api/books/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Add a book (admin only)

```bash
curl -X POST http://localhost:8080/api/books \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Java Concurrency in Practice",
    "author": "Brian Goetz",
    "isbn": "9780321349606",
    "publishedYear": 2006
  }'
```

### Borrow a book

```bash
curl -X POST http://localhost:8080/api/loans \
  -H "Authorization: Bearer USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"bookId": 1, "dueDate": "2026-06-01"}'
```

### Return a book

```bash
curl -X PATCH http://localhost:8080/api/loans/1/return \
  -H "Authorization: Bearer USER_TOKEN"
```

### External book info via Open Library (VG — Circuit Breaker)

```bash
curl http://localhost:8080/api/books/9780132350884/external-info \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## Performance Benchmarking (Redis Caching)

The `GET /api/books/{id}` endpoint uses `@Cacheable`. The first request hits the database;
all subsequent requests for the same ID are served from Redis without any DB query.

### Benchmark Commands (curl)

```bash
# Request 1 — hits the database (cold start)
time curl -s http://localhost:8080/api/books/1 \
  -H "Authorization: Bearer YOUR_TOKEN" -o /dev/null -w "Time: %{time_total}s\n"

# Request 2 — served from Redis cache (warm)
time curl -s http://localhost:8080/api/books/1 \
  -H "Authorization: Bearer YOUR_TOKEN" -o /dev/null -w "Time: %{time_total}s\n"
```

### ✅ Actual Measured Results (2026-05-24)

| Request | Source | Response Time |
|---------|--------|---------------|
| Request 1 | 🗄️ Database (cold) | **559 ms** |
| Request 2 | ⚡ Redis Cache | **17 ms** |
| Request 3 | ⚡ Redis Cache | **9 ms** |

**Performance improvement: 96.7% faster** — from 559 ms (DB) down to ~13 ms average (Redis cache).

This confirms `@Cacheable` is working correctly: the database is only queried once per book ID,
and all subsequent requests are served directly from Redis.

---

## Rate Limiting Test

```bash
# Send 65 requests quickly — the last 5 will be rejected with HTTP 429
for i in {1..65}; do
  curl -s -o /dev/null -w "%{http_code}\n" \
    http://localhost:8080/api/books/1 \
    -H "Authorization: Bearer YOUR_TOKEN"
done
```

Limit: **60 requests per minute per IP address**.

---

## Input Validation Test

```bash
# Rejected: ISBN must be exactly 13 digits
curl -X POST http://localhost:8080/api/books \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "Test", "author": "Author", "isbn": "123", "publishedYear": 2024}'
```

---

## H2 Console (Database Inspector)

- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:librarydb`
- Username: `sa`
- Password: _(leave blank)_

---

## Run Tests

```bash
mvn test
```

---

## Project Structure

```
src/main/java/com/library/api/
├── config/
│   ├── SecurityConfig.java       # Spring Security + CORS policy
│   ├── RedisConfig.java          # Redis Cache configuration
│   ├── RestTemplateConfig.java   # HTTP client for external APIs
│   └── DataInitializer.java      # Seeds test users and books on startup
├── controller/
│   ├── AuthController.java       # POST /api/auth/login, /register
│   ├── BookController.java       # CRUD /api/books/**
│   └── LoanController.java       # POST/GET /api/loans/**
├── dto/                          # Request/Response objects with validation
├── entity/                       # JPA entities (database tables)
├── exception/
│   └── GlobalExceptionHandler    # Centralized error handling
├── filter/
│   └── RateLimitFilter.java      # Rate limiting via Bucket4j
├── repository/                   # Spring Data JPA interfaces
├── security/
│   ├── JwtUtil.java              # JWT generation and validation
│   ├── JwtFilter.java            # JWT authentication filter
│   └── CustomUserDetailsService  # Loads user from database
└── service/
    ├── AuthService.java          # Login and registration logic
    ├── BookService.java          # Book CRUD + @Cacheable
    ├── LoanService.java          # Borrow and return books
    └── ExternalBookService.java  # Open Library API + Circuit Breaker (VG)
```
