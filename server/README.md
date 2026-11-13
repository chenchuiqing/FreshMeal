# FreshMeal Server

## Local Requirements

- JDK 17
- Maven 3.9.x
- MySQL 8 on `127.0.0.1:3306`
- Redis 7 on `127.0.0.1:6379`

## Start

Create the database first; Flyway creates the tables and seed data on first startup:

```sql
CREATE DATABASE freshmeal DEFAULT CHARACTER SET utf8mb4;
```

```powershell
$env:JWT_SECRET_KEY = '<replace-with-a-long-random-secret>'
mvn spring-boot:run
```

Flyway applies migrations from `src/main/resources/db/migration` during startup. Full startup steps are in the repository root `README.md`.

## Seed Accounts

| Role | Username | Initial Password |
|---|---|---|
| User | `user01` | `123456` |
| Admin | `admin` | `123456` |
| Rider | `rider01` | `123456` |

Change the initial passwords after the first login.
