# AI instructions

## Before you start
- Understand the request and make the smallest complete change.
- Check for existing solutions first. Follow established standards and patterns in the codebase instead of creating new ones.

## Working on changes
- Preserve existing contracts, validation, access control and error handling.
- Treat investigation and recommendations as read-only; change the repository when the user asks for implementation.
- Keep changes in scope.

## README and documentation
- Read documentation in [`docs/`](../docs/) that is relevant to the task. Check relevant existing documentation before adding or changing it.
- When a change depends on an external requirement, check the relevant [specification](../docs/architecture.md#specification-sources) and identify the version you used.
- Write new and changed text in English. Follow [ASD-STE100 Issue 9, 2025-01-15](https://www.asd-ste100.org/assets/files/ASD-STE100_ISSUE9.pdf). Check relevant rules and dictionary entries. Preserve meaning, conditions and technical terms, and report gaps. Short sentences alone do not prove compliance.
- README: a short introduction to purpose, prerequisites, setup and operation. Change it only when these change or the user asks.
- Document stable behavior, domain rules, lifecycle constraints and operational guidance that readers need beyond a specific code change. Keep implementation details with the code, and avoid duplicating information that the code or API docs already state.
- Add topic-based pages under `docs/` when they serve a clear reader need. Update related pages when behavior changes, and remove information that is no longer true.
- Use the C4 model at the level that answers the reader's question. Apply strict salience filtering to architecture documents and include only facts that matter at system level. Keep code comments, TODOs and local development details out of high-level summaries.
- In the project README, do not include implementation details, framework mechanisms or change history.

## Architecture decision records
- Follow the [ADR instructions in `docs/adr.md`](../docs/adr.md). They define when to record a decision, the status values and ASD-STE100 requirements.

## Tests
- Start from the rule the test must protect, and check the observable response or stored state with the smallest case that would catch a regression.
- Give each JUnit test a `@DisplayName` starting with `Should` that states the outcome and condition and matches its `should...` method name.

## Before delivery
- Verify the change and review the diff.
- Check that affected documentation matches the change and that decisions follow the ADR instructions, including status updates.

## Branch and PR
- Ask for missing JIRA IDs; never invent them.
- Create PRs, with necessary commits and pushes, only on user request, including the app's "Create PR" button.
- Branch: `<jira-id>`, with the supplied casing; if taken, use `<jira-id>-2`, then `<jira-id>-3`.
- PR title: `<JIRA-ID>: <Norwegian title>`.
- PR description: 1–2 short Norwegian sentences about the actual change, with concrete wording from the request and no assumed benefits. Include the need only to explain the change. Include technical explanations, implementation details, validation, error codes, unchanged behaviour or tests only when needed for review.
