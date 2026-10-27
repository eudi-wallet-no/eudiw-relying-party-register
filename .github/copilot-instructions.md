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
- PR-beskrivelse: én til to korte setninger på norsk om hva som faktisk er endret, på overordnet nivå. Bruk konkrete ord fra bestillingen; ikke legg til antatt nytte eller omskriv en enkel endring til en teknisk forklaring. Ta med behovet bare når det forklarer endringen. Utelat implementasjonsdetaljer, validering, feilkoder, uendret atferd og tester med mindre revieweren trenger dem for å vurdere endringen.