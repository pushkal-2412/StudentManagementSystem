# Student Management System (Java + Spring Boot + MySQL)

A browser-based Student Management System built with Java, Spring Boot, MySQL 8
and JDBC. It provides authenticated academic administration at
`http://localhost:8081`, reusing the existing service and DAO layers.

## Features

| Module | What it does |
|---|---|
| Authentication | BCrypt-hashed admin login, browser sessions, logout, change password |
| Dashboard | Student/course/department totals, attendance, fees and recent registrations |
| Students | Create, edit, delete, search and filter records |
| Courses | Create, edit, delete, assign students and view course rosters |
| Departments | Create, edit and delete departments with database relationship protection |
| Attendance | Bulk or individual daily marking and monthly attendance summaries |
| Examinations | Manage exams, enter marks and review results and grades |
| Fees | Fee balances, part payments, payment history and status |
| Reports | Institute, department, course, attendance, examination and fee reports; CSV/PDF export |

## Requirements

- Java 21 or newer (LTS)
- Maven 3.9+
- MySQL 8 with the `student_management` database initialized

## Database

The application reads `db.url`, `db.user` and `db.password` from
`src/main/resources/application.properties`. Override credentials using
`DB_USERNAME` and `DB_PASSWORD` (or optionally `DB_URL` for another server).
Legacy `SMS_DB_*` environment variables remain supported.

For a new local database, the MySQL command-line client can run the included
schema script:

```powershell
mysql -u root -p -e "source database/student_management.sql"
```

**Warning:** this script drops and recreates `student_management`. Do not run it
against an existing database or valuable data. The application is configured
to use MySQL 8; it does not connect to PostgreSQL. The PostgreSQL schema file
in `database/` is legacy and is not used by the application. If migrating data
from an existing PostgreSQL installation, back it up and perform a deliberate
data/schema migration.

To start the application from PowerShell without putting the password in a
tracked file or command history, set environment variables for the current
shell (enter the password at the hidden prompt):

```powershell
$env:DB_USERNAME = 'root'
$securePassword = Read-Host 'MySQL password' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
java -jar target/StudentManagementSystem.jar
Remove-Item Env:DB_USERNAME, Env:DB_PASSWORD
```

## Build and run

From the project directory:

```powershell
mvn clean package
java -jar target/StudentManagementSystem.jar
```

The Maven Wrapper is not included in this repository; install Maven 3.9+ or
use an IDE with Maven support to build the project.

Then open **http://localhost:8081** in a browser. The server checks its
MySQL connection during startup and exits with an explicit error if it
cannot connect.

In an IDE, run `web.WebApplication` (or the compatibility `Main` class).

## Demo accounts

| Username | Password | Role |
|---|---|---|
| admin | admin123 | ADMIN |
| staff | staff123 | STAFF |

Passwords are stored as BCrypt hashes. Change the demo password immediately;
do not use the demo credentials on an internet-facing deployment.

## Architecture

```text
src/main/java/
├── web/          Spring Boot entry point, API, session and CSRF handling
├── controller/   Existing dashboard/report controllers
├── dao/          JDBC data access
├── service/      Validation, business rules and transactions
├── model/        Domain models
├── database/     MySQL connection configuration
├── utils/        Password, validation, date, money and export helpers
└── view/         Legacy Swing screens

src/main/resources/static/  Browser interface (HTML, CSS and JavaScript)
database/                   MySQL seed script and legacy PostgreSQL schema
```

## Security notes

- API requests use a session-bound CSRF token; protected routes require an
  authenticated browser session.
- Session cookies are HttpOnly and SameSite=Lax.
- The default HTTP port is 8081. Configure HTTPS and production-grade secrets
  before exposing the server beyond localhost.
