# FreshMeal Server

## Local Requirements

- JDK 17
- Maven 3.9.x
- MySQL 8 on `127.0.0.1:3306`
- Redis 7 on `127.0.0.1:6379`

## Start

```powershell
$env:JWT_SECRET_KEY = '<replace-with-a-long-random-secret>'
mvn spring-boot:run
```

Flyway applies migrations from `src/main/resources/db/migration` during startup.

## Seed Accounts

| Role | Username | Initial Password |
|---|---|---|
| User | `user01` | `123456` |
| Admin | `admin` | `123456` |
| Rider | `rider01` | `123456` |

Change the initial passwords after the first login.
