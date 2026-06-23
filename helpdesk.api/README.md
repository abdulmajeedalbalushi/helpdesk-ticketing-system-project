# Helpdesk Ticketing API

A Spring Boot REST API for an IT helpdesk: employees raise tickets, tickets get assigned to support agents, status moves through a fixed lifecycle, and either party can comment. Backed by Oracle Database via Spring Data JPA.

## Tech stack

- Java 17, Spring Boot 3.5.15
- Spring Data JPA / Hibernate
- Oracle Database (primary) 
- Maven (wrapper included, no local Maven install needed)

## Running the project locally

### Prerequisites

- Java 17 JDK installed
- An Oracle Database instance reachable from your machine (host, port, service name, username, password).

### Run against real Oracle

1. Update `src/main/resources/application.properties` with your real connection details:
   ```properties
   spring.datasource.url=jdbc:oracle:thin:@//HOST:PORT/SERVICE_NAME
   spring.datasource.username=YOUR_USERNAME
   spring.datasource.password=YOUR_PASSWORD
   spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
   spring.jpa.database-platform=org.hibernate.dialect.OracleDialect
   ```
2. Make sure that user has table-creation rights and tablespace quota (`ALTER USER your_user QUOTA UNLIMITED ON USERS;`) — without quota, table creation succeeds but every insert fails with `ORA-01950`.
3. Run:
   ```bash
   ./mvnw spring-boot:run
   ```
4. On first run, Hibernate (`spring.jpa.hibernate.ddl-auto=update`) automatically creates the `app_user`, `agent`, `ticket`, and `ticket_comment` tables from the entity classes — no manual DDL needed.

### Running the test suite

```bash
./mvnw test
```
Tests run against an isolated in-memory H2 database (configured in `src/test/resources/application.properties`), so they never touch your real Oracle instance and don't require Oracle to be running at all.

### Testing the API

Once running, hit `http://localhost:8080/api/...` with Postman or curl. Typical flow:
```
POST /api/users          -> create the person raising tickets
POST /api/agents         -> create the support staff
POST /api/tickets        -> raise a ticket (raisedByUserId required)
POST /api/tickets/{id}/assign      -> assign an agent
PATCH /api/tickets/{id}/status     -> move status forward
POST /api/tickets/{id}/comments    -> add a comment
GET /api/tickets/{id}               -> full detail incl. comments
```

## API surface

| Method | Path | Notes | Example request body                                                                                                                     |
|---|---|---|------------------------------------------------------------------------------------------------------------------------------------------|
| POST | `/api/users` | Create a user (employee). 409 on duplicate email. | `{"name":"Ahmed","email":"ahmed@example.com"}`                                                                                           |
| GET | `/api/users`, `/api/users/{id}` | List / get a user. | —                                                                                                                                        |
| POST | `/api/agents` | Create an agent. 409 on duplicate email. | `{"name":"khald","email":"khald@example.com"}`                                                                                           |
| GET | `/api/agents`, `/api/agents/{id}` | List / get an agent. | —                                                                                                                                        |
| POST | `/api/tickets` | Create a ticket (defaults to status `OPEN`). | `{"title":"VPN not working","description":"Cannot connect from home","priority":"HIGH","category":"NETWORK_SUPPORT","raisedByUserId":1}` |
| GET | `/api/tickets/{id}` | Full ticket detail, including comments. | —                                                                                                                                        |
| GET | `/api/tickets?status=&priority=&category=&assignedAgentId=` | Search with any combination of filters, all optional. | —                                                                                                                                        |
| POST | `/api/tickets/{id}/assign` | Assign an agent to a ticket. | `{"agentId":1}`                                                                                                                          |
| PATCH | `/api/tickets/{id}/status` | Move ticket status (validated against the state machine below). | `{"status":"IN_PROGRESS"}`                                                                                                               |
| POST | `/api/tickets/{id}/comments` | Add a comment from a user or an agent. | `{"authorType":"USER","authorId":1,"message":"Still happening after reboot."}`                                                           |
| GET | `/api/tickets/reports/avg-resolution-time?agentId=&category=` | Average time from OPEN to RESOLVED, optionally filtered. | —                                                                                                                                        |
| GET | `/api/tickets/reports/overdue` | Unresolved tickets that have breached their SLA. | —                                                                                                                                        |

## Business rules and the reasoning behind them

### Ticket status is a strict 4-state machine

Allowed transitions, enforced server-side, are exactly:
```
OPEN -> IN_PROGRESS
IN_PROGRESS -> RESOLVED
RESOLVED -> REOPENED
REOPENED -> IN_PROGRESS
```
Any other request (including no-op transitions to the same status) is rejected with `409 Conflict` and a message like `Cannot transition from RESOLVED to IN_PROGRESS`. This is implemented as a `Map<TicketStatus, Set<TicketStatus>>` lookup table in `TicketService`, not as `if`/`else` branches — the rule is data, so adding or auditing transitions never means touching control-flow code.

**Why no self-loop (`status -> same status`) is allowed:** the spec defines transitions, not "no-ops." Treating `OPEN -> OPEN` as valid would mean any client could "successfully" call the status endpoint with a meaningless no-op; rejecting it surfaces a 409 if a client's state assumption is stale, which is more useful for catching client-side bugs than silently no-opping.

### Comment authorship uses a "soft" reference, not a real foreign key

A comment can come from either the raising `User` or the assigned `Agent` — two different tables. Rather than modeling this with JPA inheritance/polymorphic associations (which would force `User` and `Agent` into a shared base table), `Comment` stores `authorType` (`USER`/`AGENT`) plus a plain `authorId` long. The database has no FK constraint on `authorId`; the service layer validates the referenced user/agent actually exists before saving, based on which `authorType` was given.

The spec describes creating a user and creating an agent as two distinct actions on two distinct concepts. A single `Person` table with a `role` column would reduce duplication today (both tables currently have identical `name`/`email`/`createdAt` shapes), but would force every future agent-only field (e.g. department, specialization) to be a nullable column that's meaningless for users, or push toward JPA table-per-class inheritance. Two independent tables avoid that complexity and match the spec's own vocabulary.

### `resolvedAt` is a derived field, added to make reporting possible

Nothing in the original spec mentioned storing *when* a ticket became resolved — only that the avg-resolution-time report needs "average time from OPEN to RESOLVED." Without a captured resolution timestamp, that number is uncomputable from `createdAt`/`updatedAt` alone (`updatedAt` changes on every field edit, not specifically on becoming resolved). **Assumption:** `Ticket.resolvedAt` is set the moment status becomes `RESOLVED`, and cleared (`null`) the moment it becomes `REOPENED`. This means:
- A ticket resolved once, then reopened, **does not count** toward the average until it's resolved again — the average is meant to reflect *currently and successfully resolved* tickets, not abandoned/reopened resolution attempts.
- If you need historical resolution durations across reopen cycles, that would require a full status-change audit log, which isn't part of this spec.

### Average resolution time math uses Java, not Oracle SQL date functions

The average is computed by fetching matching resolved tickets and averaging `Duration.between(createdAt, resolvedAt)` in the service layer, rather than via a SQL `AVG(resolved_at - created_at)`-style query. **Reasoning:** Oracle have different date-arithmetic semantics, and a SQL-side computation would behave differently — or simply fail. Computing it in Java keeps the result identical regardless of backing database, at the cost of pulling full rows into memory rather than letting the database do the aggregation. For the ticket volumes a system like this realistically handles, that tradeoff is reasonable.

### Overdue calculation is based on ticket age since creation, not time in current status

The spec gives SLA durations per priority (CRITICAL 4h, HIGH 1d, MEDIUM 3d, LOW 7d) and says to list "open tickets exceeding their SLA." **Assumption:** "open" means *not yet resolved* (`status != RESOLVED`), and the SLA clock starts at `createdAt` (when the ticket was first raised), not at whenever it last changed status. This was chosen because the spec frames SLA around ticket age/urgency from the customer's perspective (a CRITICAL ticket sitting unresolved for 5 hours is overdue regardless of how many times its status flickered between `IN_PROGRESS` and `REOPENED` along the way). Like the state-machine map, the SLA durations are stored as a `Map<Priority, Duration>`, not as conditional logic.

### Combined search filters use one dynamic query, not chained single-filter lookups

`GET /api/tickets` accepts `status`, `priority`, `category`, and `assignedAgentId`, all optional, in any combination. This is implemented as a single JPQL query with `(:param IS NULL OR field = :param)` per filter, rather than branching to a different repository method per filter combination (which would require 2⁴ = 16 method variants to truly cover "any combination"). Passing zero filters returns everything; passing all four ANDs them together.

### Validation and error handling

- Malformed JSON or an invalid enum value (e.g. `"priority": "super-high"`) returns `400 Bad Request`, not `500` — Jackson deserialization failures are caught explicitly (`HttpMessageNotReadableException`) rather than falling through to the generic exception handler.
- Duplicate email on user/agent creation returns `409 Conflict`, not `500` — checked explicitly in the service layer (`UserService`/`AgentService`) before insert, even though the database also enforces a `UNIQUE` constraint as a backstop.
- Looking up a nonexistent user/agent/ticket id (whether directly, or indirectly — e.g. assigning a ticket to an agent id that doesn't exist) returns `404 Not Found`.
- All of the above are centralized in one `@RestControllerAdvice` (`GlobalExceptionHandler`) rather than scattered `try`/`catch` blocks per controller method.

## Other assumptions

- **No authentication/authorization.** The spec describes *what* the system does (raise, assign, comment, report) but not *who* is allowed to do it or how callers identify themselves. Every endpoint is open; `raisedByUserId`/`authorId`/`agentId` are trusted as given in the request body. Adding auth would be a natural next step but is out of scope here.
- **Full CRUD on users/agents beyond create+list+get** (update, delete) was not required by the spec and isn't implemented — only what the ticket workflow actually needs.
- **`ddl-auto=update`** is used so the schema evolves automatically from the entity classes during development. This is explicitly not a production-safe setting (it can't handle destructive changes like column renames) — a real deployment should switch to a migration tool (Flyway/Liquibase) once the schema stabilizes.
- **IDENTITY-based primary keys** (`GenerationType.IDENTITY`) were chosen over Oracle's traditional `SEQUENCE` + trigger pattern, since Oracle 12c+ supports native identity columns and Hibernate handles them transparently — simpler for this scale, at the minor cost of disabling JDBC batch inserts (not a concern here).
