# System and Database Design

This folder is the review entry point for Sprint 1 design deliverables.

| Jira task | Deliverable | Source of truth |
|---|---|---|
| CNPM-18 | System architecture | [`architecture.md`](architecture.md), [`architecture.drawio`](architecture.drawio) |
| CNPM-19 | Initial ERD | [`erd.md`](erd.md), [`erd.drawio`](erd.drawio) |
| CNPM-20 | Data Dictionary | [`database-design.md`](database-design.md) |
| CNPM-21 | Spring Boot skeleton | Project root, `pom.xml`, `src/main`, `src/test`, `docs/DEVELOPMENT.md` |

## Design rules

- Jira remains the source of truth for requirements, issues, tasks and sprint state after synchronization.
- GitHub remains the source of truth for repositories, commits, pull requests and workflow runs.
- MySQL stores accounts, authorization, group mappings, integration configuration, synchronized snapshots, reports and audit data.
- Flyway migrations in `src/main/resources/db/migration` are the physical database source of truth.
- Secrets are supplied through environment variables and are never committed in plaintext.

