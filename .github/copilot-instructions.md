# AI instructions

## Before you start
- Read [`docs/`](../docs/).
- Load [`proposal`](skills/proposal/SKILL.md) before every repository change. Follow it through planning, implementation, verification and review.

## README and documentation
- Write new and changed text in English. Follow [ASD-STE100 Issue 9, 2025-01-15](https://www.asd-ste100.org/assets/files/ASD-STE100_ISSUE9.pdf). Check relevant rules and dictionary entries. Preserve meaning, conditions and technical terms, and report gaps. Short sentences alone do not prove compliance.
- README: a short introduction to purpose, prerequisites, setup and operation. Change it only when these change or the user asks.
- Do not include implementation details, framework mechanisms or change history. Add `docs/` content only for a specific need.

## Branch and PR
- Ask for missing JIRA IDs; never invent them.
- Create PRs, with necessary commits and pushes, only on user request, including the app's "Create PR" button.
- Branch: `<jira-id>`, with the supplied casing; if taken, use `<jira-id>-2`, then `<jira-id>-3`.
- PR title: `<JIRA-ID>: <Norwegian title>`.
- PR description: 1–2 short Norwegian sentences about the actual change, with concrete wording from the request and no assumed benefits. Include the need only to explain the change. Include technical explanations, implementation details, validation, error codes, unchanged behaviour or tests only when needed for review.