---
name: proposal
description: "Use before every repository change and follow through planning, implementation, verification and review. Resolve uncertainty, keep solutions simple, limit changes to the request and verify the result."
license: Digitaliseringsdirektoratet
---

# Proposal

**Understand the request. Make the smallest complete change. Verify the result.**

Load this skill before any repository change, including code, tests, configuration and documentation. Follow it until delivery. Scale the work to the risk; a clear, small request does not need a separate plan approval.

- **Think before coding** — Read project instructions, relevant skills, files and tests in the current workspace. Check existing work and trace affected flows, consumers, state changes and failure paths. State assumptions that affect the solution. Show different interpretations and simpler alternatives when relevant. Stop and ask if requirements conflict or a decision remains unclear. An analysis request does not authorize edits; obtain approval for proposed scope or behavior changes.
- **Keep the solution simple** — Solve the requested problem without extra features, speculative configuration, single-use abstractions or handling for impossible failures. Reuse existing solutions where suitable. Reduce complexity, not only line count. Do not weaken contracts, validation, access control or error handling to make the solution smaller.
- **Make precise changes** — Each changed line must support the request or a necessary correction in the affected flow. Preserve user work and existing conventions. Do not refactor or improve unrelated code, comments or formatting. Remove only items that your changes make unused. Report unrelated findings and pre-existing failures without fixing them.
- **Verify the outcome** — Define the expected result and the smallest check that proves it before editing. For a bug, prefer a test that fails before the fix and passes after. For a refactor, preserve existing tests and check behavior before and after. For several steps, give a short plan with a result and check for each step. Use project tools, cover affected flows and correct in-scope failures until checks pass. Update documentation only where needed. Review the complete diff before delivery; seek independent review for multi-file changes, new patterns or architecture decisions, and report when it is unavailable. State what was verified, what remains uncertain and any missing checks.
