# JIRA-ID i branch og PR-tittel

Alle oppgaver har en JIRA-ID. **Spør alltid brukeren om JIRA-ID** hvis den ikke er oppgitt —
ikke anta eller finn på en. Brukes som prefiks i branch-navn og i PR-tittel:

```
<JIRA-ID>: <PR-tittel>
```

F.eks. `EUW-1234: Ny pr`. Uten dette feiler `validate-pr-title`-sjekken i GitHub Actions.

# PR-beskrivelser

Skriv menneskelig, kort og konkret. Unngå AI-språk ("This PR introduces..."), emojis, overdreven struktur, selvskryt og oppsummeringsvegger. Én linje holder for trivielle endringer.

Mal:

## Hva og hvorfor
1-2 setninger: hva endres og hvorfor.

## Endringer
- Viktigste punkter, ikke hver fil/detalj.

## Merknader
Valgfritt: risiko, følgeendringer, ting reviewer bør vite. Slett seksjonen hvis ikke relevant.

PR description skal alltid reflektere gjeldende endringer — oppdater den når det legges til flere commits/endringer etter at PR-en først ble opprettet, ikke bare la den stå igjen fra første commit.
