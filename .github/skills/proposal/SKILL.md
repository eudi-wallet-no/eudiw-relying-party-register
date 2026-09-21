---
name: proposal
description: "Plan multi-file changes, unclear requests, new patterns and architecture decisions. Get approval before editing."
license: Digitaliseringsdirektoratet
---

# Proposal

Agree on a plan before editing. Scale its detail to the risk; a clear, small change needs only a goal, changed files and verification.

## Workflow

1. Read relevant files, project instructions and recent history in the current workspace. Check the branch and uncommitted changes. Fetch `main` and bring this branch up to date before planning; do not overwrite user work. Ask if changes conflict.
2. Map the impact before designing a solution. Trace affected user and system flows end to end: entry points, callers, dependencies, data and state changes, downstream consumers and failure paths. Read the surrounding code, not only the edit locations. Identify direct and indirect consequences, including shared behavior and contracts that must remain compatible. Continue until affected flows and their boundaries are understood; resolve blocking unknowns before choosing a solution.
3. Find existing solutions and tests. Read relevant skills before choosing an approach. If the request conflicts with requirements or rests on a false assumption, explain the evidence and ask before proceeding.
4. Choose the smallest complete solution that covers the mapped consequences. Prefer reuse over new dependencies, abstractions or parallel implementations. Exclude unrelated changes.
5. Present the goal, acceptance criteria, scope, assumptions, realistic options and recommendation. Include affected flows, consequences and remaining uncertainties. Identify affected locations with `file:line` from files you read. List tasks in dependency order with completion criteria.
6. Name verification commands found in the project and their expected results. Cover the affected flows and preserved behavior, not just the edited code. For authentication, payments, migrations or cross-service changes, cover affected callers, dependency failures, how failures are detected and where secrets are stored.
7. Challenge the plan for missed edge cases. For multi-file changes, new patterns or architecture decisions, seek an independent review when available; otherwise make a separate critical pass yourself. Change the plan based on evidence.
8. Wait for approval. Then run relevant baseline checks, implement the tasks, update affected documentation and verify the result. Report pre-existing failures without fixing unrelated issues.

Ask instead of guessing after two failed attempts to find a file or answer. Batch independent reads and searches.
