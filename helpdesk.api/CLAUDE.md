# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

This is a freshly scaffolded Spring Boot 3.5.15 (Java 17) application — `com.helpdesk:helpdesk.api`. There is no business logic yet: only the generated `Application` main class and a placeholder test. There is no database, no web layer beyond `spring-boot-starter-web`, and no `pom.xml` packaging customizations.

## Commands

Run all commands from the repo root (where `pom.xml`, `mvnw`, and `mvnw.cmd` live). Use the Maven wrapper, not a system `mvn`.

- Build: `./mvnw clean package` (PowerShell: `./mvnw.cmd clean package`)
- Run the app: `./mvnw spring-boot:run`
- Run all tests: `./mvnw test`
- Run a single test class: `./mvnw test -Dtest=ApplicationTests`
- Run a single test method: `./mvnw test -Dtest=ApplicationTests#contextLoads`

## Architecture

- Base package: `com.helpdesk.helpdesk.api` (note the doubled `helpdesk` segment — this comes from groupId `com.helpdesk` + artifactId `helpdesk.api`). Place new classes under this package, mirrored under `src/test/java` for tests.
- `Application.java` is the `@SpringBootApplication` entry point — standard Spring Boot auto-configuration applies (no custom configuration classes exist yet).
- `src/main/resources/application.properties` currently only sets `spring.application.name`. Add Spring configuration (server port, datasource, etc.) here as the project grows.
