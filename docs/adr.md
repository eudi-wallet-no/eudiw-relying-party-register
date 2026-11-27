# Architecture decision records

Add an ADR only on user request. You can recommend one for a decision with important effects.

Use one short table row per selected decision. Write from the user's point of view: say what the user does and what the system does next. In **Decision**, state the outcome. In **Why**, explain why the team chose it. Use **References / notes** for consequences, caveats, or sources. Add the date, a unique number, and status. Use `planned` until the code uses the decision. If you find that the code already uses a `planned` decision, verify it and update the status to `implemented`.

Write ADR text in English. Obey [ASD-STE100 Issue 9](https://www.asd-ste100.org/assets/files/ASD-STE100_ISSUE9.pdf), including its dictionary. Use technical terms consistently. Report any language checks you cannot complete.

| ID | Date | Decision | Why | Status | References / notes |
| --- | --- | --- | --- | --- | --- |
| 0001 | 2026-11-09 | Registration certificates expire after 24 hours. The service issues replacement certificates automatically. | A short validity period removes the need for a status list for long-lived registration certificates. | planned | |
| 0002 | 2026-11-19 | When an admin or self-service user submits an ordinary form, the register service saves the submitted values. The latest save replaces the stored values. | The frontend and register service do not check whether the form contains old values. This keeps form updates simple. | implemented | A user can overwrite newer data by saving an older form. |
| 0003 | 2026-11-25 | Each service has a maximum of one active `IntendedUse`. When a user adds a new `IntendedUse` for a service, the register service archives the previous active `IntendedUse`. | Each service must have only one active `IntendedUse`. Archiving keeps previous records available. | planned | At this time, the V2 API lets a service have more than one active `IntendedUse`. |
