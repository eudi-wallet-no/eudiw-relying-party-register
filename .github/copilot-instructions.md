# JIRA-ID i branch og PR-tittel

Alle oppgaver har en JIRA-ID. **Spør alltid brukeren om JIRA-ID** hvis den ikke er oppgitt —
ikke anta eller finn på en. Brukes i PR-tittel:

```
<JIRA-ID>: <PR-tittel>
```

F.eks. `EUW-1234: Ny pr`. Uten dette feiler `validate-pr-title`-sjekken i GitHub Actions.

PR-tittelen (delen etter JIRA-ID-prefikset) skal skrives på **norsk**.

## Branch-navn

Ny branch (uansett om den opprettes via `rename_branch` eller direkte med git) skal hete
`<jira-id>`, f.eks. `euw-1234`. Ikke bruk fritekstbeskrivelse. Finnes branchen fra før,
inkrementer: `euw-1234-2`, `euw-1234-3` osv.

# PR-beskrivelser

Skriv menneskelig, kort og konkret. Unngå AI-språk ("This PR introduces..."), emojis, overdreven struktur, selvskryt og oppsummeringsvegger. Én linje holder for trivielle endringer.

Mal:
```
## Hva og hvorfor
1-2 setninger: hva endres og hvorfor.

## Endringer
- Viktigste punkter, ikke hver fil/detalj.
```