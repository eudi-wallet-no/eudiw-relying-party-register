# AI instructions

## Before you start
- Read [`docs/`](../docs/)
- Multi-file changes, unclear scope, new patterns or architecture decisions: [`proposal`](skills/proposal/SKILL.md) before implementation.
- Writing or changing tests: [`test-design`](skills/test-design/SKILL.md).

## README og dokumentasjon
- README: kort innledning til formål, forutsetninger og hvordan løsningen settes opp og kjøres.
- Endre README bare når dette faktisk endres, eller brukeren ber om det. Ikke vedlikehold den som fast steg ved kodeendringer.
- Hold implementasjonsdetaljer, rammeverksmekanismer og endringshistorikk utenfor. Opprett `docs/`-innhold bare ved et konkret behov.

## Branch and PR
- Ask for the JIRA ID if it is missing. Never invent one.
- Never create a pull request on the remote. The user creates PRs manually.
- Branch: `<jira-id>`, preserving the supplied casing; if taken, use `<jira-id>-2`, then `<jira-id>-3`.
- PR title: `<JIRA-ID>: <Norwegian title>`.
- PR description: Norwegian, one short paragraph. Problem, what changes, and the practical benefit. Nothing else unless the reviewer needs it to judge the change.