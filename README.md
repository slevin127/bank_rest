# Bank Cards Management API

Spring Boot service for managing bank cards, cardholders, and internal transfers. The project ships with JWT based security, Liquibase migrations, comprehensive validation, and a Docker powered dev environment.

## Capabilities
- Register and authenticate users with hashed passwords and JWT tokens
- Role based access for `ADMIN` and `USER`
- Admin user management (create, update, delete, list) through protected `/api/admin/users` endpoints
- CRUD operations for bank cards with masked numbers, encrypted storage, and admin status controls
- User self-service card filters (status, masked number, balance range) plus block requests for their cards
- Transfers between a user's own cards with concurrency protection
- Swagger/OpenAPI documentation and actuator health endpoints

## Technology Stack
- Java 17, Spring Boot 3, Spring Data JPA, Spring Security
- PostgreSQL with Liquibase database migrations
- JWT (jjwt) for access and refresh tokens
- Docker Compose for local infrastructure
- JUnit 5, Mockito for automated tests

## Getting Started
1. **Prerequisites**
   - Java 17+
   - Maven 3.9+
   - Docker and Docker Compose (for local PostgreSQL)
2. **Clone and install dependencies**
   ```bash
   mvn clean install -DskipTests
   ```
3. **Run PostgreSQL**
   ```bash
   docker compose up postgres -d
   ```
4. **Start the application**
   ```bash
   mvn spring-boot:run
   ```
5. **Access Swagger UI**: http://localhost:8080/swagger-ui.html

## Configuration
All runtime settings live in `src/main/resources/application.yml` and can be overridden via environment variables.

Important variables:
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `APP_JWT_SECRET`, `APP_JWT_ACCESS_EXP_MINUTES`, `APP_JWT_REFRESH_EXP_DAYS`
- `APP_CRYPTO_SECRET` (32+ characters for AES key)
- `APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD`, `APP_ADMIN_EMAIL`

A default admin account is provisioned on startup using the `app.init.admin` properties. Change these values before deploying to production.

## Database
- Liquibase migrations live in `src/main/resources/db/migration`
- Schema is validated at startup (`spring.jpa.hibernate.ddl-auto=validate`)
- To add changes, create a new changelog file and include it from `db.changelog-master.yaml`

## Testing
Run the test suite with:
```bash
mvn test
```
Key business logic is covered by unit tests under `src/test/java/com/example/bankcards/service`.

## Project Structure
```
src/main/java/com/example/bankcards
  ??? config        # Security, OpenAPI, configuration properties, data init
  ??? controller    # REST controllers and request handling
  ??? controller/advice # Global error handling
  ??? dto           # DTOs for requests and responses
  ??? entity        # JPA entities and enums
  ??? exception     # Custom domain exceptions
  ??? mapper        # Mapping helpers
  ??? repository    # Spring Data repositories and specifications
  ??? security      # JWT services and authentication filter
  ??? service       # Business logic and application services
  ??? util          # Utility helpers
```

## API Overview
Generated OpenAPI definition is exposed at `/v3/api-docs`. Use Swagger UI or import the specification (`docs/openapi.yaml`) into your API client.

## Additional Commands
- Build a runnable jar: `mvn clean package`
- Stop PostgreSQL: `docker compose down`

## License
MIT

