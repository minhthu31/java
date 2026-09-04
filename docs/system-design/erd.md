# CNPM-19 - Initial Entity Relationship Diagram

## 1. Scope and source of truth

This ERD represents the physical schema created by `V1__create_core_schema.sql`. Flyway remains the executable source of truth. Column-level definitions are recorded in `database-design.md`.

```mermaid
erDiagram
    roles ||--o{ users : assigns
    users ||--o{ user_external_accounts : owns
    users o|--o{ student_groups : leads
    student_groups ||--o{ group_members : contains
    users ||--o{ group_members : joins
    student_groups ||--o{ group_lecturers : supervised_by
    users ||--o{ group_lecturers : lectures
    student_groups ||--o{ projects : owns
    projects ||--o{ integration_configs : configures
    projects ||--o{ requirements : defines
    projects ||--o{ features : groups
    features o|--o{ features : parent_of
    projects ||--o{ sprints : schedules
    projects ||--o{ tasks : contains
    requirements o|--o{ tasks : traced_by
    features o|--o{ tasks : classifies
    sprints o|--o{ tasks : plans
    users o|--o{ tasks : assigned_to
    tasks ||--o| jira_issues : maps_to
    projects ||--o{ github_repositories : integrates
    github_repositories ||--o{ github_commits : contains
    user_external_accounts o|--o{ github_commits : authors
    github_repositories ||--o{ github_pull_requests : contains
    user_external_accounts o|--o{ github_pull_requests : authors
    github_repositories ||--o{ workflow_runs : executes
    github_commits o|--o{ workflow_runs : validates
    tasks ||--o{ task_commit_links : links
    github_commits ||--o{ task_commit_links : linked_to
    users o|--o{ task_commit_links : confirms
    tasks ||--o{ task_pr_links : links
    github_pull_requests ||--o{ task_pr_links : linked_to
    users o|--o{ task_pr_links : confirms
    projects ||--o{ sync_logs : records
    users o|--o{ activity_logs : performs
    student_groups ||--o{ activity_logs : scopes
    projects ||--o{ srs_versions : versions
    users ||--o{ srs_versions : generates
```

## 2. Domain grouping

| Domain | Tables |
|---|---|
| Identity and access | `roles`, `users`, `user_external_accounts` |
| Academic groups | `student_groups`, `group_members`, `group_lecturers` |
| Project planning | `projects`, `requirements`, `features`, `sprints`, `tasks` |
| Jira integration | `integration_configs`, `jira_issues`, `sync_logs` |
| GitHub integration | `github_repositories`, `github_commits`, `github_pull_requests`, `workflow_runs` |
| Traceability | `task_commit_links`, `task_pr_links` |
| Reporting and audit | `activity_logs`, `srs_versions` |

## 3. Cardinality rules

- Every user has exactly one global role; one role can be assigned to many users.
- A student group may have one leader and many active/historical members.
- A lecturer can supervise many groups and a group can have many lecturers.
- Every project belongs to exactly one student group.
- A task belongs to one project and may reference one requirement, feature, sprint and assignee.
- A task has at most one synchronized Jira issue.
- A project can connect to many GitHub repositories.
- Tasks and commits/PRs have many-to-many relationships represented by link tables.
- Synchronization and activity records are append-oriented operational history.

## 4. Consistency constraints

- Provider identifiers and Jira/GitHub keys use unique constraints to make synchronization idempotent.
- Junction tables use composite primary keys to prevent duplicate memberships and links.
- Self-referencing `features.parent_feature_id` supports a feature hierarchy.
- Nullable foreign keys represent optional mappings, not unknown mandatory ownership.
- Deletion is intentionally not cascaded in Sprint 1; services must prevent deleting referenced records or implement an explicit archival policy.

## 5. Acceptance checklist

- [x] Contains every table created by Flyway V1.
- [x] Shows primary relationships and cardinalities.
- [x] Separates identity, planning, integration, traceability and audit domains.
- [x] Defines key uniqueness and optionality decisions.
- [x] Provides an editable draw.io overview and Mermaid source.

