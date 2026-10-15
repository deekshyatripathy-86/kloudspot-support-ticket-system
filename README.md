# Kloudspot Support Ticket System

A multi-tenant support ticket application with a Spring Boot REST API and a React/Vite frontend. Organizations, users, tickets, comments, and history are persisted in MySQL.

## Requirements

- JDK 17+
- Maven 3.9+
- Node.js 20.19+ or 22.12+
- npm
- Docker Desktop (or a local MySQL 8 instance)

## Run locally

1. Start MySQL from the project root:

   ```bash
   docker compose up -d mysql
   ```

2. Start the API in a terminal from the project root:

   ```bash
   mvn spring-boot:run
   ```

   The API listens on `http://localhost:8080`. Swagger UI is available at
   `http://localhost:8080/swagger-ui/index.html`.

3. In another terminal, start the frontend:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   Open the Vite URL printed in the terminal (normally `http://localhost:5173`).
   The frontend defaults to `http://localhost:8080/api/v1`. To point it at a
   different API, set `VITE_API_URL` before running Vite, for example:

   ```bash
   VITE_API_URL=http://localhost:8080/api/v1 npm run dev
   ```

The API's development CORS configuration permits `localhost` and `127.0.0.1`
origins. Restart the backend after changing server configuration.

## Local demo accounts

Demo data is seeded at API startup:

| Organization ID | Organization | Email | Password | Role |
|---|---|---|---|---|
| 1 | Demo Organization | `admin@demo.com` | `ChangeMe123!` | ADMIN |
| 1 | Demo Organization | `agent@demo.com` | `ChangeMe123!` | AGENT |
| 1 | Demo Organization | `customer@demo.com` | `ChangeMe123!` | CUSTOMER |
| 2 | Second Organization | `customer@second.com` | `ChangeMe123!` | CUSTOMER |

The login request includes the organization ID, email, and password. Organization
ID 1 is prefilled in the UI. Demo credentials are for local evaluation only.

## Configuration

Defaults are in `src/main/resources/application.yml`; override with environment
variables:

- `SERVER_PORT` (default `8080`)
- `DB_URL` (default local MySQL `support_tickets` database)
- `DB_USERNAME` and `DB_PASSWORD` (defaults `root` / `root`)
- `JWT_SECRET` (use a random secret of at least 32 bytes outside local development)
- `JWT_EXPIRATION_MS` (default `3600000`)
- `DEMO_DATA_ENABLED` (default `true`; set to `false` outside local evaluation)

Hibernate uses `ddl-auto: update` for local startup. For production, use database
migrations, validate the schema, use dedicated database credentials, and disable
demo data seeding before exposing the service.

## Features and authorization

- JWT authentication with `ADMIN`, `AGENT`, and `CUSTOMER` roles.
- Ticket list, create, view, update, assignment, status changes, and close.
- Organization-scoped ticket access; customers additionally see only their own
  tickets. The organization is taken from the authenticated principal, not from
  a ticket-create payload.
- Search by subject/description; status, priority, and assigned-agent filters;
  pagination and sort by creation time.
- Ticket comments, internal staff notes hidden from customers, and assignment /
  status history.
- Organization admins can create users and list organization users. Admins and
  agents can view their organization's agent directory for ticket assignment.
- Organization creation is currently seeded for local/demo use; there is no
  organization-provisioning API or administration UI.
- The frontend includes login, ticket creation, ticket listing and filtering,
  ticket details, comments/history, and staff editing/assignment/status management.

## REST API

All endpoints use `/api/v1`. For protected endpoints, send the JWT returned by
login in the HTTP `Authorization` header, using the standard `Bearer` scheme.

Example login request:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"organizationId":1,"email":"agent@demo.com","password":"ChangeMe123!"}'
```

Copy `accessToken` from the login response and include it in the Authorization
header when calling any protected API.

| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/login` | Authenticate |
| GET | `/auth/me` | Current user |
| GET | `/users/agents` | List agents in the caller's organization (staff only) |
| GET | `/users` | List users in the caller's organization (admin only) |
| POST | `/users` | Create an organization user (admin only) |
| POST | `/tickets` | Create ticket |
| GET | `/tickets` | List/search/filter/page/sort tickets |
| GET | `/tickets/{id}` | Ticket detail |
| PATCH | `/tickets/{id}` | Update subject, description, or priority (staff only) |
| PATCH | `/tickets/{id}/assign` | Assign to an agent in the same organization |
| PATCH | `/tickets/{id}/status` | Change status |
| PATCH | `/tickets/{id}/close` | Close ticket |
| GET | `/tickets/{id}/history` | Status/assignment history |
| GET | `/tickets/{id}/comments` | List comments visible to the caller |
| POST | `/tickets/{id}/comments` | Add a public comment or staff-only internal note |

Ticket listing accepts `status`, `priority`, `assignedAgentId`, `search`, `page`,
`size`, `sortBy`, and `sortDir` query parameters. For example:

```text
GET /api/v1/tickets?status=OPEN&priority=HIGH&assignedAgentId=2&page=0&size=10&sortBy=createdAt&sortDir=DESC
```

Example user creation request (organization comes from the admin's token):

```json
{
  "name": "New Support Agent",
  "email": "new.agent@example.com",
  "password": "ChangeMe123!",
  "role": "AGENT"
}
```

Passwords must contain at least 8 characters and no more than 72 UTF-8 bytes
(the limit imposed by BCrypt).

OpenAPI documentation is served by Swagger UI at
`http://localhost:8080/swagger-ui/index.html`.

## Tests and builds

Run backend unit tests:

```bash
mvn test
```

Build the frontend:

```bash
cd frontend
npm run build
```

## Assumptions and limitations

- Organizations are represented in the database and seeded as two tenants; users
  cannot provision new organizations through the application yet.
- User creation and listing are tenant-scoped. Only organization admins may
  create users; only staff can list the agent directory. This starter does not
  implement user deletion, password reset, or organization-level role policy
  configuration.
- Demo data is intended only for local evaluation. Set `DEMO_DATA_ENABLED=false`
  and replace all development secrets and database credentials before deployment.
- No rate limiting, refresh-token flow, email notifications, attachments, or
  optimistic locking is included.
