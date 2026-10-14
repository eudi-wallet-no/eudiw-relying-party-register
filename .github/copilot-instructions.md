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
- Never create a pull request on the remote. The user creates PRs manually.
- Branch: `<jira-id>`, preserving the supplied casing; if taken, use `<jira-id>-2`, then `<jira-id>-3`.
- PR title: `<JIRA-ID>: <Norwegian title>`.
- PR description: very short and written in Norwegian. State what changes and the practical benefit in direct, plain sentences. Include important removals or fixes; omit implementation explanations, unchanged behavior and file references unless needed to understand a consequence.