# AI instructions

## Before you start
- Read [`docs/`](../docs/)
- Multi-file changes, unclear scope, new patterns or architecture decisions: [`proposal`](skills/proposal/SKILL.md) before implementation.

## README og dokumentasjon
- README: kort innledning til formål, forutsetninger og hvordan løsningen settes opp og kjøres.
- Endre README bare når dette faktisk endres, eller brukeren ber om det. Ikke vedlikehold den som fast steg ved kodeendringer.
- Hold implementasjonsdetaljer, rammeverksmekanismer og endringshistorikk utenfor. Opprett `docs/`-innhold bare ved et konkret behov.

## Branch and PR
- Ask for the JIRA ID if it is missing. Never invent one.
- Create a PR, including any necessary commit and push, only at the user's request, including via the app's "Create PR" button.
- Branch: `<jira-id>`, preserving the supplied casing; if taken, use `<jira-id>-2`, then `<jira-id>-3`.
- PR title: `<JIRA-ID>: <Norwegian title>`.
- PR description: one or two short sentences in Norwegian describing what actually changed at a high level. Use concrete wording from the request; do not add assumed benefits or turn a simple change into a technical explanation. Include the need only when it explains the change. Omit implementation details, validation, error codes, unchanged behaviour and tests unless the reviewer needs them to assess the change.