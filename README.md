# Flut Trading

A Spring Boot REST service that solves the "Flut Trading" optimization problem: given one or
more piles ("Schuurs") of fluts, each with an ordered list of prices, it computes the maximum
achievable profit and every possible number of fluts that achieves it. The service accepts
input either as JSON or as the original assignment text-file format.

## 📋 Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [The Flut Trading Problem](#the-flut-trading-problem)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Docker](#docker)
- [Troubleshooting](#troubleshooting)

## ✨ Features

### Core Functionality
- **Two input formats**: JSON request or the original assignment text file — both routed
  through the same domain model, service, and solver.
- **Optimization solver**: computes the maximum profit across all piles and every possible
  total number of fluts that achieves it.
- **Bean validation**: request-level validation on JSON input (non-empty Schuurs/prices,
  positive prices).
- **Per-Schuur error isolation** (file input): a malformed pile line only fails that one
  Schuur — the rest of the file is still processed and returned.
- **Global exception handling**: consistent `400`/`500` responses, no leaked stack traces.

## 🏗️ Architecture

```
                     JSON Request                    Text File
                          │                               │
                          ▼                               ▼
                 ┌─────────────────────────────────────────────────┐
                 │              FlutTradingController              │
                 │  POST /api/v1/fluts/trade       (JSON)          │
                 │  POST /api/v1/fluts/trade/file  (multipart)     │
                 └───────────┬──────────────────────────┬──────────┘
                             │                           │
                             │                  ┌────────▼───────────┐
                             │                  │ FlutInputFileParser│
                             │                  │  (text → domain)   │
                             │                  └────────┬───────────┘
                             │                           │
                             ▼                           ▼
                 ┌────────────────────────────────────────────────────┐
                 │                 FlutTradingService                 │
                 │   maps request → List<FlutPile>, calls the solver  │
                 └───────────────────────┬────────────────────────────┘
                                         │
                                         ▼
                 ┌────────────────────────────────────────────────────┐
                 │                 FlutTradingSolver                  │
                 │  pure domain logic — no HTTP/JSON/Spring MVC       │
                 │  → TradingResult(maximumProfit, possibleFlutCounts)│
                 └───────────────────────┬────────────────────────────┘
                                         │
                                         ▼
                              FlutTradingResponse
                        (JSON, or formatted plain text)
```

### Component Structure
```
src/main/java/com/niki/fluttrading/
├── controller/          # FlutTradingController — thin, no business logic
├── contract/            # API DTOs: FlutTradingRequest, SchuurRequest, FluteRequest, FlutTradingResponse
├── domain/               # FlutPile, TradingResult — independent of REST/JSON
├── service/              # FlutTradingService, FlutInputFileParser, FlutTradingResponseFormatter
│   └── impl/
├── solver/               # FlutTradingSolver — the optimization algorithm
│   ├── contract/
│   └── impl/
└── exception/            # GlobalExceptionHandler
```

## 🔧 Prerequisites

- **Java**: 21 or higher
- **Maven**: 3.9 or higher (or use the bundled `./mvnw` wrapper — no local Maven needed)
- **Docker**: optional, only needed to run the app as a container

## 🚀 Quick Start

### Option 1: Run locally with Maven

1. **Build the application**
   ```bash
   ./mvnw clean install
   ```

2. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Verify it's running**
   ```bash
   curl -X POST http://localhost:8080/api/v1/fluts/trade \
     -H "Content-Type: application/json" \
     -d '{"schuurs":[{"schuur":1,"flutes":[{"prices":[7,3,11,9,10]}]}]}'
   ```

### Option 2: Run with Docker

1. **Build the image**
   ```bash
   docker build -t flut-trading:latest .
   ```

2. **Run the container**
   ```bash
   docker run --rm -p 8080:8080 --name flut-trading flut-trading:latest
   ```

3. **Stop it**
   ```bash
   docker stop flut-trading
   ```

See [Docker](#docker) below for the full guide, including configuration overrides.

## ⚙️ Configuration

Key configuration options in `src/main/resources/application.properties`:

```properties
spring.application.name=flut-trading

server.port=8080

# The price each flut can be sold for; profit per flut = sale-price - buy-price
flut-trading.sale-price=10

logging.level.root=INFO
logging.level.com.niki.fluttrading=DEBUG
```

### Environment Variables

Override configuration at runtime using Spring Boot's relaxed environment variable binding:

```bash
export SERVER_PORT=9090
export FLUT_TRADING_SALE_PRICE=12
```

## 🧩 The Flut Trading Problem

Given one or more Schuurs (pile-groups), each containing one or more flute piles with prices
ordered top to bottom, you may only buy a **prefix** of each pile (i.e. buy the top `n` fluts,
you cannot skip an item and buy the one below it). Every flut bought can later be sold at a
fixed sale price. The service determines:

- the **maximum total profit** achievable across all piles, and
- **every possible total number of fluts** (across all piles) that achieves that maximum
  profit — capped at the 10 smallest counts if more than 10 exist.

### Example

Pile: `[7, 3, 11, 9, 10]` with a sale price of `10` →
maximum profit `10`, achieved by buying either `2`, `4`, or `5` fluts.

## 📊 API Documentation

### `POST /api/v1/fluts/trade` — JSON input

**Request:**
```json
{
  "schuurs": [
    {
      "schuur": 1,
      "flutes": [
        { "prices": [7, 3, 11, 9, 10] }
      ]
    },
    {
      "schuur": 2,
      "flutes": [
        { "prices": [1, 2, 3, 4, 10, 16, 10, 4, 16] }
      ]
    }
  ]
}
```

**Response:**
```json
{
  "result": [
    "schuurs 1",
    "Maximum profit is 10.",
    "Number of fluts to buy: 2, 4, 5",
    "schuurs 2",
    "Maximum profit is 30.",
    "Number of fluts to buy: 4, 5, 8"
  ]
}
```

### `POST /api/v1/fluts/trade/file` — original text-file input

Accepts the original assignment input format as a `multipart/form-data` file upload. The
file format is:

```text
2
5 7 3 11 9 10
9 1 2 3 4 10 16 10 4 16
0
```

- The first line of each test case is the number of piles in that Schuur.
- Each following pile line starts with the count of prices, followed by that many prices.

The response is a plain-text body with the same content as the JSON `result` array, one line
at a time.

### Trying the API with Postman

Import the ready-made collection at
[`FlutTrading.postman_collection.json`](resources/FlutTrading.postman_collection.json)
into Postman — it already contains configured requests for both endpoints (`Trade (JSON)` and
`Trade (File upload)`) with example bodies and a `baseUrl` variable
(defaults to `http://localhost:8080`).

To import: open Postman → **File → Import** → select
`FlutTrading.postman_collection.json`.

### Sending a file with curl

```bash
curl -X POST http://localhost:8080/api/v1/fluts/trade/file \
  -F "file=@path/to/input.txt"
```

- `-F "file=@path/to/input.txt"` uploads the file as a `multipart/form-data` part named
  `file`, matching the `@RequestParam("file")` on the endpoint.
- Replace `path/to/input.txt` with the path to your own input file (the original assignment
  format described above).

## 🧪 Testing

### Run All Tests

```bash
./mvnw verify
```

This runs both unit tests (`./mvnw test`) and integration tests (Maven Failsafe).

### Run a Specific Test Class

```bash
./mvnw test -Dtest=FlutTradingSolverImplTest
```

### Test Coverage

The project includes **54 tests** across:
- `FlutTradingSolverImplTest` — the optimization algorithm (single/multiple piles,
  buy-nothing, buy-everything, negative intermediate profits, >10 optimal counts, etc.)
- `FlutInputFileParserImplTest` — text-file parsing, including malformed lines
- `FlutTradingControllerJsonIT` / `FlutTradingControllerFileIT` / `FlutTradingControllerSmokeIT`
  — full HTTP request/response contract for both endpoints, valid and invalid scenarios

## 🐳 Docker

The project ships with a multi-stage `Dockerfile`:

```text
Stage 1 (build)    eclipse-temurin:21-jdk
    │  copies .mvn/, mvnw, pom.xml first (cached dependency layer)
    │  ./mvnw dependency:go-offline
    │  copies src/, then ./mvnw clean package -DskipTests
    ▼
Stage 2 (runtime)  eclipse-temurin:21-jre (slim, no build tools)
    │  runs as a non-root "app" user
    │  copies only the built jar from stage 1
    └─ ENTRYPOINT: java -jar app.jar
```

Tests are **not** run during the image build (`-DskipTests`) to keep builds fast; run
`./mvnw verify` locally/in CI before building the image. A `.dockerignore` excludes
`target/`, `.git/`, and `.idea/` from the build context.

### Build

```bash
docker build -t flut-trading:latest .
```

### Run

```bash
docker run --rm -p 8080:8080 --name flut-trading flut-trading:latest
```

Run in the background instead by adding `-d`:

```bash
docker run -d --rm -p 8080:8080 --name flut-trading flut-trading:latest
```

Stop it with:

```bash
docker stop flut-trading
```

### Override configuration at runtime

```bash
docker run --rm -p 9090:9090 \
  -e SERVER_PORT=9090 \
  -e FLUT_TRADING_SALE_PRICE=12 \
  flut-trading:latest
```

(Remember to also change the `-p` host mapping if you override `SERVER_PORT`.)

### Clean up

```bash
docker rmi flut-trading:latest
```

## 🔍 Troubleshooting

- **`docker build` fails with `'docker buildx build' requires 1 argument`** — you forgot the
  build context path. Run `docker build -t flut-trading:latest .` (note the trailing `.`).
- **File upload returns `400 Bad Request`** — check that the file is not empty and that the
  multipart field is named `file` (see the curl example above).
- **JSON request returns `400 Bad Request`** — check that `schuurs` is present and non-empty,
  and that every price is a positive integer.
