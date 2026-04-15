# Patient Appointment and Vaccination Portal

A RESTful Spring Boot backend for managing patients, clinic appointments, vaccinations, and address validation.

---

## Prerequisites

| Tool  | Version                                   |
| ----- | ----------------------------------------- |
| Java  | 21                                        |
| Maven | 3.9+ (or use the included `mvnw` wrapper) |
| MySQL | 8.0+                                      |

---

## Local Setup

### 1. Configure local secrets

Copy the template and fill in your values:

```bash
cp school-service-spring/application-local.properties.template \
   school-service-spring/application-local.properties
```

Then edit `application-local.properties`:

```properties
# Geoapify — free key at myprojects.geoapify.com
geoapify.api-key=YOUR_GEOAPIFY_API_KEY

db.host=localhost
db.port=3306
db.name=health_portal_db

spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD

jwt.secret=YOUR_JWT_SECRET
```

`application-local.properties` is gitignored — never commit it.

### 2. Start MySQL

Make sure MySQL is running and the configured user has permission to create databases. The application will create `health_portal_db` (or whatever `db.name` is set to) automatically on first start.

### 3. Run the application

```bash
cd school-service-spring
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080/api/v1`.

---

## API Documentation

Swagger UI is available at `http://localhost:8080/api/v1/swagger-ui.html` once the server is running.

The OpenAPI contract is at [`openapi.yaml`](openapi.yaml) in the project root.

---

## Design Decisions

See [DESIGN.md](DESIGN.md) for architectural decisions and rationale.
