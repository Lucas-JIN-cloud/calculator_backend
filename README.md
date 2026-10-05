# calculator_backend
Backend service for a Web calculator. It provides REST APIs for a frontend (uni-app / Vue3 H5) covering math expression evaluation, calculation history management, favorites, and base conversion.

Expressions are evaluated with the **exp4j** safe parser (instead of `eval`, preventing script injection), and persistence uses an embedded **SQLite** database — no external database required.



***

## 1. Tech Stack



| Category           | Choice                      | Version    |
| ------------------ | --------------------------- | ---------- |
| Language / Runtime | Java                        | 17         |
| Framework          | Spring Boot                 | 3.2.0      |
| ORM                | MyBatis Spring Boot Starter | 3.0.3      |
| Database           | SQLite (sqlite-jdbc)        | 3.45.1.0   |
| Expression parser  | exp4j                       | 0.4.8      |
| Build tool         | Maven                       | 3.9+       |
| Container          | Docker (multi-stage build)  | Temurin 17 |



***

## 2. Features



* Math expression evaluation: `+ - * / ^ %`, parentheses, functions `sin / cos / tan / sqrt / log`, and constants `pi / e`.

* Calculation history: automatically saves the expression, result, and timestamp for each calculation.

* History management: list all, delete by ID, and clear all.

* Favorites: toggle favorite / unfavorite on a history record.

* Base conversion: convert between any bases from 2 to 36 (powered by `BigInteger`).

* CORS support: configured to allow direct calls from the frontend H5.

* Zero external dependencies: SQLite is a file-based database; tables are auto-created on startup.

* Built-in static resources: the compiled frontend H5 is bundled under `src/main/resources/static`.



***

## 3. Project Structure



```
Caculate/
├── Dockerfile                              # Multi-stage build image
├── pom.xml                                 # Maven dependencies & build config
├── calculator_history.db                   # SQLite database file (created at runtime)
└── src/main/
    ├── java/com/example/caculate/
    │   ├── CaculateApplication.java        # Entry point
    │   ├── CalcController.java             # REST controller (all endpoints)
    │   ├── CalculationService.java         # Business logic (calc / favorite / base conversion)
    │   ├── CalculationHistory.java         # History record entity
    │   ├── CalculationHistoryMapper.java   # MyBatis mapper interface
    │   ├── CorsConfig.java                 # CORS configuration
    │   ├── ContentDispositionFilter.java   # Response header filter
    │   └── Result.java                     # Unified response wrapper
    └── resources/
        ├── application.properties          # Application config
        ├── CalculationHistoryMapper.xml    # MyBatis SQL mapping (table creation / CRUD)
        └── static/                         # Bundled frontend H5
```



***

## 4. Quick Start

### 1. Requirements



* JDK 17 or above

* Maven 3.9+ (or use the bundled `mvnw` / `mvnw.cmd`)

### 2. Run locally



```
# Windows
mvnw.cmd clean package -DskipTests
java -jar target\calculator-backend-0.0.1-SNAPSHOT.jar

# Linux / macOS
./mvnw clean package -DskipTests
java -jar target/calculator-backend-0.0.1-SNAPSHOT.jar
```

By default the server listens on `http://localhost:8080`. Override the port with the `PORT` environment variable or a command-line argument:



```
# Run on port 9000
java -jar target/calculator-backend-0.0.1-SNAPSHOT.jar --server.port=9000
```

### 3. Verify



```
curl http://localhost:8080/api/health
# {"success":true,"status":"ok"}
```



***

## 5. API Documentation

All endpoints are prefixed with `/api` and return JSON.

### 1. Evaluate an expression

`POST /api/calculate`

Request body:



```
{ "expression": "1+2*3" }
```

Success response:



```
{
  "success": true,
  "data": {
    "id": null,
    "expression": "1+2*3",
    "result": "7.0",
    "is_favorite": 0,
    "createdAt": null
  }
}
```

Error response:



```
{ "success": false, "error": "Invalid expression: ..." }
```

> Note: the auto-increment primary key is not returned after insert, so
> `id`
> /
> `createdAt`
> in this response are
> `null`
> ; the frontend fetches the real IDs from the history-list endpoint.

### 2. List all history

`GET /api/history`



```
{
  "success": true,
  "data": [
    {
      "id": 1,
      "expression": "5*5",
      "result": "25.0",
      "is_favorite": 0,
      "createdAt": "2026-10-05T14:34:44"
    }
  ]
}
```

### 3. Delete a single record

`DELETE /api/history/{id}`



```
{ "success": true }
```

### 4. Clear all history

`DELETE /api/history/all`



```
{ "success": true }
```

> The literal path
> `/history/all`
> takes priority over
> `/history/{id}`
> , so it is not parsed as an ID.

### 5. Toggle favorite

`POST /api/history/{id}/favorite`



```
{ "success": true }
```

The backend flips `is_favorite` between `1` and `0` (`CASE WHEN is_favorite = 1 THEN 0 ELSE 1 END`).

### 6. Base conversion

`POST /api/convert/base`

Request body:



```
{ "value": "10", "fromBase": 10, "toBase": 2 }
```

Success response:



```
{ "success": true, "result": "1010" }
```

Error response (returns both `message` and `error` so different frontend modules can read either):



```
{ "success": false, "message": "Value does not match the selected base", "error": "Value does not match the selected base" }
```

### 7. Health check

`GET /api/health`



```
{ "success": true, "status": "ok" }
```

### Endpoint Summary



| Method | Path                         | Description       |
| ------ | ---------------------------- | ----------------- |
| POST   | `/api/calculate`             | Evaluate and save |
| GET    | `/api/history`               | List all history  |
| DELETE | `/api/history/{id}`          | Delete one        |
| DELETE | `/api/history/all`           | Clear all         |
| POST   | `/api/history/{id}/favorite` | Toggle favorite   |
| POST   | `/api/convert/base`          | Base conversion   |
| GET    | `/api/health`                | Health check      |



***

## 6. Database Design

The SQLite table `calculation_history` is auto-created on startup via `CREATE TABLE IF NOT EXISTS`:



| Column        | Type     | Description                                |
| ------------- | -------- | ------------------------------------------ |
| `id`          | INTEGER  | Primary key, auto-increment                |
| `expression`  | TEXT     | Expression, not null                       |
| `result`      | TEXT     | Calculation result, not null               |
| `is_favorite` | INTEGER  | Favorite flag, default `0`                 |
| `created_at`  | DATETIME | Creation time, default `CURRENT_TIMESTAMP` |

For older databases, the startup routine runs `ALTER TABLE ... ADD COLUMN is_favorite` to upgrade the schema; if the column already exists, the error is caught and ignored so startup is unaffected.



***

## 7. Deployment

### Docker



```
docker build -t calculator-backend .
docker run -d -p 8080:8080 --name calculator-backend calculator-backend
```

The `Dockerfile` uses a multi-stage build: it packages the jar in a Maven image, then copies it into the slim `eclipse-temurin:17-jre-alpine` runtime image.

### Alibaba Cloud Function Compute (FC)



* Runtime: custom runtime Debian 12 / Java 17

* Listening port: `9000`

* Start command:



```
java -jar /code/calculator-backend-0.0.1-SNAPSHOT.jar --server.port=9000
```

Deployment: run `mvn clean package -DskipTests` to build the jar → zip the jar (jar at the archive root) → in the FC console use "Upload code → Upload ZIP → Save and deploy".



***

## 8. Security Notes



* **No&#x20;**`eval`: expressions are parsed by exp4j and only allow math operations, so arbitrary code cannot be executed.

* **CORS**: `/api/**` allows cross-origin requests (`allowedOriginPatterns("*")`, methods `GET/POST/DELETE/OPTIONS`).

* **Input validation**: empty expressions, base range (2–36), and value/base mismatch are all validated with clear error messages.



***

## 9. Notes



* The package and entry-point class names keep the historical spelling `caculate` (not `calculate`); do not rename the directories/files unless you also update the package and class names consistently.

* The database file `calculator_history.db` lives in the working directory. Deleting it wipes the history; it is recreated automatically on next startup.

* The frontend is a separate project (uni-app / Vue3) and talks to this service through the `/api/*` endpoints above.
