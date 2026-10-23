# iPOS

iPOS is a point-of-sale and stock management web application for a small shop in Rwanda. It covers the till, receipts and invoices in the RRA EBM style, products, customers, suppliers, purchases, imports, expenses, stock periods, reports, users, settings and audit logging.

## Stack

- Java 21, Spring Boot 3.4, Spring Security, Spring Data JPA
- Thymeleaf templates, vanilla JavaScript and Chart.js
- PostgreSQL 16 with Flyway migrations
- OpenPDF for PDF receipts and invoices, ZXing for barcodes
- JUnit 5, MockMvc and Testcontainers

## Getting started

Prerequisites: Java 21, Docker Desktop (recommended), or a PostgreSQL 16 installation. Maven Wrapper is included.

1. Start PostgreSQL and Mailpit:

   ```powershell
   docker compose up -d postgres mailpit
   ```

2. Copy `.env.example` to `.env` and change the development secrets. The default database port is `5437`.

3. Run the application:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

4. Open <http://localhost:8081>. Mailpit is at <http://localhost:8025>.

Default development login: `admin` / `admin123`. Change it immediately outside local development. The administrator is created only when the `app_users` table is empty.

The `dev` profile seeds a demonstration catalogue, customers and suppliers when `IPOS_DEMO_DATA=true`. Set `IPOS_DEMO_DATA=false` to start with an empty shop.

### Running without Docker

Create a PostgreSQL database named `ipos` and provide the connection through environment variables:

```powershell
$env:IPOS_DB_HOST = "localhost"
$env:IPOS_DB_PORT = "5432"
$env:IPOS_DB_NAME = "ipos"
$env:IPOS_DB_USER = "postgres"
$env:IPOS_DB_PASSWORD = "your-password"
.\mvnw.cmd spring-boot:run
```

Flyway creates and upgrades the schema automatically. No manual SQL is required.

## Profiles

- `dev` (default): Thymeleaf cache disabled, debug application logging, demo data enabled by default.
- `prod`: template cache enabled, quieter logging, demo data disabled. Select it with `IPOS_PROFILE=prod`.

Common environment variables are documented in `.env.example`. Uploaded receipt attachments and product images are written to `IPOS_UPLOAD_DIR`.

## Receipts and email

Sales receipts and invoices support browser printing and PDF download. SMTP credentials are configured in **Settings > Email**; SMTP settings are stored in the database and the password is encrypted with `IPOS_ENCRYPTION_KEY`. Email delivery failures are recorded in **Email log**, with a resend action. In development, Mailpit captures all messages. Email delivery is disabled until SMTP is configured.

## Tests

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean verify
```

Tests require a working Docker environment; they start a disposable `postgres:16-alpine` container through Testcontainers. The test profile enables a small demo catalogue for page and import-flow coverage. No H2 dependency is used.

If Docker is unavailable, the tests fall back to a configured PostgreSQL test database via `IPOS_TEST_DB_URL`, `IPOS_TEST_DB_USER` and `IPOS_TEST_DB_PASSWORD`. The default fallback is `jdbc:postgresql://localhost:5437/ipos_test` with `postgres` / `postgres`. Create that database before running the fallback.

## Project layout

- `src/main/java/com/ntaganira/heritier/ipos/backoffice` - configuration, entities, repositories, services and controllers
- `src/main/resources/templates` - Thymeleaf pages by module
- `src/main/resources/static` - CSS, JavaScript and packaged vendor assets
- `src/main/resources/db/migration` - Flyway schema and reference-data migrations
- `src/test` - integration and unit tests

Assumptions and design trade-offs are recorded in [DECISIONS.md](DECISIONS.md).
