---
name: project_helpdesk_audit_findings
description: Known gaps found in helpdesk.api during the 2026-06-22 compliance audit, for follow-up re-audits
metadata:
  type: project
---

Audited helpdesk.api (Spring Boot 3.5.15 / Java 17, Oracle + H2 fallback) against an 18-item checklist (CRUD, state machine, search/reporting, layering, validation, status codes). 15/18 PASS, 2 PARTIAL, on 2026-06-22.

Key unresolved gaps as of that audit:
- No 409 handling exists for any conflict scenario other than illegal ticket-status transitions. `User.email`/`Agent.email` have DB-level `unique` constraints (`User.java`, `Agent.java`) but no service-layer duplicate check and no `DataIntegrityViolationException` handler in `GlobalExceptionHandler.java` — a duplicate email currently falls through to the generic 500 handler.
- `TicketService.assignAgent` has no guard against reassigning an agent on a RESOLVED/closed ticket — this is intentional per a comment in `TicketControllerIntegrationTest.java` ("independent of the status state machine"), not an oversight, but it means there's no 409 path for that case if a reviewer expects one.
- Avg-resolution-time and overdue-SLA reports both key off `Ticket.createdAt` rather than a separate "became OPEN" event, so a REOPENED ticket's SLA/resolution clock doesn't reset from the reopen point.

**Why this matters:** these are the exact things to re-check if asked to re-audit this repo after fixes are applied, or if the user asks "did you already find X" about this codebase.

**How to apply:** Before trusting this as still-current, re-grep for `DataIntegrityViolationException`/`ConstraintViolationException` in `exception/GlobalExceptionHandler.java` and re-read `TicketService.assignAgent` — these are the two items most likely to change first.
