# To-Do REST API (Spring Boot + MySQL)

A layered REST API for managing to-do items, built with **Java 21, Spring Boot 3, Spring Data JPA (Hibernate) and MySQL**.

## Features

- Full CRUD for todos (create, read, update, delete) plus a one-call "toggle completed"
- Priority levels using a Java **enum** (`LOW`, `MEDIUM`, `HIGH`), stored as text in MySQL
- Filtering by completion status and priority, case-insensitive title search
- Pagination and sorting (`page`, `size`, `sort`)
- Input validation with clear error messages
- Global exception handling with RFC 7807 `ProblemDetail` responses
- Interactive API docs with Swagger UI
- Integration tests with MockMvc and an in-memory H2 database (tests do not need MySQL)

## Tech stack
[src](src)
| Layer | Technology |
|---|---|
| Language / build | Java 21, Maven |
| Framework | Spring Boot 3.5 (Web, Data JPA, Validation) |
| Database | MySQL 8 |
| API docs | springdoc-openapi (Swagger UI) |
| Tests | JUnit 5, MockMvc, H2 |

## Architecture

```
Client (Postman / Swagger)
        |
        v
TodoController      -> HTTP only: routes, status codes, @Valid
        |
        v
TodoService         -> business logic, @Transactional
        |
        v
TodoRepository      -> Spring Data JPA (JpaRepository + custom @Query)
        |
        v
MySQL (table: todos)
```

```
src/main/java/com/example/todo
├── TodoApplication.java          entry point (@SpringBootApplication)
├── controller/TodoController     REST endpoints
├── service/TodoService           business logic
├── repository/TodoRepository     database access
├── model/Todo, Priority          JPA entity and enum
├── dto/TodoRequest, TodoResponse request/response records (entity is never exposed)
└── exception/                    custom exception + @RestControllerAdvice handler
```

## Run it

### 1. Prerequisites

- JDK 21 (or 17+ after changing `java.version` in `pom.xml`)
- Maven 3.9+ (or open the folder in IntelliJ IDEA / VS Code and run `TodoApplication`)
- MySQL 8 running locally

### 2. Create the database

```sql
CREATE DATABASE tododb;
```

Tables are created automatically on first start (`spring.jpa.hibernate.ddl-auto=update`).

### 3. Set your MySQL credentials

Defaults are `root` / `root`. Change them in `src/main/resources/application.properties`, or use environment variables:

```bash
export DB_USERNAME=root
export DB_PASSWORD=your_password
# optional: export DB_URL="jdbc:mysql://localhost:3306/tododb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
```

### 4. Start the app

```bash
mvn spring-boot:run
```

The API runs on `http://localhost:8080`. Swagger UI: `http://localhost:8080/swagger-ui.html`.

### 5. Run the tests

```bash
mvn test
```

## API

| Method | Endpoint | Description | Success |
|---|---|---|---|
| POST | `/api/todos` | Create a todo | 201 + `Location` header |
| GET | `/api/todos` | List todos (filters + pagination) | 200 |
| GET | `/api/todos/{id}` | Get one todo | 200 |
| PUT | `/api/todos/{id}` | Update a todo | 200 |
| PATCH | `/api/todos/{id}/toggle` | Flip completed true/false | 200 |
| DELETE | `/api/todos/{id}` | Delete a todo | 204 |

**List query parameters:** `completed` (true/false), `priority` (LOW/MEDIUM/HIGH), `search` (text in title), `page` (default 0), `size` (default 10), `sort` (e.g. `dueDate,asc`; default newest first).

### Request body (create / update)

```json
{
  "title": "Finish Spring Boot project",
  "description": "Add README and push to GitHub",
  "priority": "HIGH",
  "dueDate": "2026-10-30",
  "completed": false
}
```

Only `title` is required. `priority` defaults to `MEDIUM`.

### Example calls

```bash
# create
curl -X POST http://localhost:8080/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"Learn JPA","priority":"HIGH","dueDate":"2026-10-30"}'

# list: high priority, not completed, title contains "learn", first page
curl "http://localhost:8080/api/todos?priority=HIGH&completed=false&search=learn&page=0&size=5"

# toggle completed
curl -X PATCH http://localhost:8080/api/todos/1/toggle

# delete
curl -X DELETE http://localhost:8080/api/todos/1
```

### Error response example (validation)

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "errors": { "title": "title must not be blank" }
}
```

## Design decisions (good to explain in interviews)

- **Layered architecture**: the controller handles HTTP, the service holds business rules and transactions, and the repository talks to the database.
- **Constructor injection** instead of field `@Autowired`: dependencies are explicit, final and easy to test.
- **DTOs (Java records)**: the API contract is separate from the database entity, so internal changes don't break clients.
- **`@Enumerated(EnumType.STRING)`**: stores `HIGH` instead of `2`, so reordering enum constants never corrupts data.
- **`@Transactional`**: the service is `readOnly = true` by default, and write methods override it. In `update` and `toggle`, Hibernate's dirty checking saves changes at commit, so no explicit `save()` is needed.
- **JPA lifecycle callbacks** (`@PrePersist`, `@PreUpdate`) maintain `createdAt` and `updatedAt`.
- **Optional filters in one JPQL query**: `(:param IS NULL OR column = :param)` skips a filter when the parameter is null.
- **`@RestControllerAdvice`**: one place maps exceptions to consistent JSON errors (404 not found, 400 validation or malformed input).
- **Tests use H2** in MySQL mode, so they run anywhere. A production-grade setup would use Testcontainers with real MySQL.

## Ideas to extend it

1. Add users and JWT login with Spring Security, so each user sees only their own todos
2. Replace `ddl-auto=update` with Flyway migrations
3. Add categories or tags (one-to-many and many-to-many relationships)
4. Add Docker and docker-compose (app + MySQL)
5. Add a simple frontend (React or plain HTML/JS) that calls this API
