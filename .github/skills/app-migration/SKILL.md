---
name: app-migration
description: "Migrer en frittstående applikasjon inn i dette monorepoet under apps/, med bevart git-historikk. Bruk når en bruker ber om å flytte/migrere et repo inn i apps/, konsolidere workflows for en app, eller sette opp CI/CD og Dependabot for en nylig migrert app. Basert på faktiske erfaringer fra migrering av eudiw-rp-register-admin."
license: Balevik
allowed-tools: ['codebase', 'terminalCommand', 'githubRepo']
---

# App-migrering inn i monorepo

Erfaringsbasert oppskrift for å flytte en frittstående applikasjon inn i `apps/<app-navn>/` i dette
monorepoet, med full historikk bevart, konsoliderte workflows og riktig Dependabot-oppsett.
Oppdater denne filen med nye lærdommer etter hver migrering.

## 1. Flytt historikk med git-filter-repo

1. Klon kildecoen til en scratch-mappe (ikke i selve monorepoet).
2. `git filter-repo --to-subdirectory-filter apps/<app-navn> --force` i klonen. Dette flytter
   *alt* innhold inn under `apps/<app-navn>/` i historikken, commit for commit.
3. I monorepo-worktreet: legg til den filtrerte klonen som en midlertidig remote, `fetch`, og
   `git merge --allow-unrelated-histories <remote>/<branch>`. Vanligvis ingen konflikter siden
   filtreringen flyttet alt til en ny, tom sti.
4. Verifiser at historikken faktisk fulgte med: `git log --follow -- apps/<app-navn>/pom.xml`
   (eller tilsvarende kjernefil) skal vise alle de opprinnelige commitene.
5. Fjern den midlertidige remoten og slett scratch-klonen.

## 2. Signerte commits (branch protection)

**Kritisk fallgruve:** Repoet krever verifiserte commit-signaturer på alle branches. Migrert
historikk er som regel usignert og **pushen vil bli avvist** med en lang liste "missing
verification" for hver commit.

Løsning: re-signer hele den nye historikken lokalt før push (krever at
`user.signingkey` / `gpg.format ssh` / `commit.gpgsign true` allerede er satt opp):

```bash
git branch backup-before-resign   # sikkerhetskopi først
git rebase --rebase-merges --exec "git commit --amend --no-edit -S" main
```

- Noen commits (typisk dependabot-duplikater) kan bli **tomme** etter signering — håndter med
  `git commit --amend --no-edit -S --allow-empty` + `git rebase --continue`.
- Verifiser etterpå at alt er signert, f.eks. `git log --show-signature main..HEAD | grep -c "Good"`
  bør matche antall commits.
- Slett backup-branchen når du er trygg på resultatet.
- **Sjekk for søl:** under en `--exec`-rebase kan IDE-genererte filer (f.eks. `.idea/.gitignore`)
  ved et uhell bli committet inn hvis de lå utrackede i arbeidstreet. Sjekk `git show --stat` på
  de nye commitene før push, og fjern med en målrettet interaktiv rebase (`edit` på riktig commit)
  hvis noe uønsket sneik seg inn.

## 3. Konsolider CI/CD-workflows

Mål: flytt fra "ett sett workflows per app" til **én workflow-fil per app i repo-roten**
(`.github/workflows/<app-navn>.yml`), som bruker `felleslosninger/github-workflows` sine
gjenbrukbare workflows med `application-path: apps/<app-navn>/` som styrende input.

- Hent de faktiske reusable workflow-definisjonene (`gh api repos/felleslosninger/github-workflows/contents/...`)
  for å se nøyaktig hvilke inputs som finnes — ikke gjett.
- Typisk jobbstruktur: `pull-request-checks` (kjøres kun på PR), `build-image` +
  `update-image` (kjøres kun på push til main), evt. `publish-dev-docker`.
- **Kjent quirk:** `ci-pr-checks-image.yml` sin nøstede `call-auto-merge`-jobb krever
  `contents: write` *unconditionally*, selv når auto-merge er skrudd av. GitHub validerer dette
  mot permissions på den kallende jobben for *hele workflow-filen* — så
  `pull-request-checks`-jobben må ha `contents: write`, ellers feiler workflow-validering
  med en litt kryptisk feilmelding om `call-auto-merge`.
- Generiske hjelpe-workflows (f.eks. PR-label-håndtering) kan løftes til repo-roten som
  app-uavhengige filer, i stedet for å dupliseres per app.

### Docker build-context — ikke bruk `context`-input hvis du ikke må

Man trenger **ikke** en egen `context`-input for å peke Docker-builden mot riktig app-mappe.
`docker/build-push-action` bruker som standard `Build().gitContext()` når `context` er utelatt —
altså **hele repoet** ved gjeldende git-ref, ikke en tom/lokal mappe. Løsningen er derfor:

- Behold `docker-file`/`file`-input pekende på `apps/<app-navn>/docker/Dockerfile.xxx`.
- Rett opp `COPY`/`ADD`-instruksjoner i Dockerfilen slik at de er relative til **repo-roten**
  (prefiks med `apps/<app-navn>/...`), siden build-konteksten nå er hele monorepoet.
- Oppdater ev. `docker-compose.yaml` i app-mappen i samme slag: `context` må endres fra `.` til
  `../..` (eller tilsvarende relativ sti til repo-roten), og `dockerfile`-stien må peke til den
  nye plasseringen — ellers brytes lokal `docker compose up`.

### Kjent uløst risiko: dev-docker image-navngiving

`misc-publish-dev-docker.yml` tagger images med `github.event.repository.name` — altså hele
monorepo-navnet, ikke app-navnet. Dette **kolliderer** så snart to eller flere apper publiserer
dev-images fra samme monorepo. Må rettes oppstrøms i `felleslosninger/github-workflows` før flere
apper migreres inn og bruker dev-docker-publisering samtidig.

## 4. Dependabot for monorepo — én PR på tvers av apper

Standard Dependabot-oppsett gir én PR *per app/katalog*. For å samle oppdateringer på tvers av
apper til én PR per økosystem:

- Bruk `directories` (flertall, støtter globbing, f.eks. `/apps/*`) i stedet for `directory`
  (entall — globber ikke).
- Legg til én catch-all-gruppe: `groups: <navn>: patterns: ["*"] group-by: dependency-name`.
  `group-by: dependency-name` er den offisielle mekanismen for å slå sammen oppdateringer *på
  tvers av flere kataloger* til én PR. (Forveksle ikke med `multi-ecosystem-groups`, som i stedet
  slår sammen *ulike økosystemer* — ikke det vi vil ha her.)
- Private registries (f.eks. GitHub Packages) må deklareres under `registries:` i
  `dependabot.yml` med credentials fra **Dependabot-secrets** (ikke nødvendigvis samme sted som
  Actions-secrets — bekreft eksplisitt med brukeren at de riktige secret-navnene faktisk finnes
  som Dependabot-secrets i repo-innstillingene, ikke bare anta at Actions-secrets gjenbrukes).
- Start enkelt (én catch-all-gruppe per økosystem) fremfor å kopiere detaljerte grupper fra andre
  prosjekter blindt — finmask heller etter hvert som flere apper er på plass og mønstre blir
  tydelige.

## 5. Andre migreringsartefakter

- **Root `.gitignore`**: fjern ev. duplikate `.gitignore`-filer i app-mappen og konsolider i én
  root-fil som dekker Maven/Gradle/Spring Boot/IDE/OS/logg/env-støy for hele monorepoet.
- **App-lokale `.github/`-mapper** (workflows, dependabot.yml) som blir overflødige etter
  konsolidering til rot, bør fjernes helt — GitHub leser uansett bare rot-`.github/`-innhold.
- **`.trivyignore`**: la den ligge **på app-nivå** (`apps/<app-navn>/.trivyignore`), ikke rot.
  `trivy-scan`-actionen sjekker rot-filen *først* og bruker den for **alle** apper hvis den
  finnes — en rot-fil ville dermed lekke CVE-unntak mellom apper i et monorepo med flere apper.
- **Root README**: hold en enkel tabell over apper i monorepoet med lenke til hver apps egen
  README, pluss en kort forklaring av mappestrukturen (`apps/`, `docs/`).

## 6. PR-beskrivelser og shell-encoding

- Bruk `.github/copilot-instructions.md` for å style AI-genererte PR-beskrivelser kort og
  menneskelig (unngå AI-preget språk/filler). Hold den kompakt — én fil er nok, ikke separat
  `PULL_REQUEST_TEMPLATE.md` også.
- **Shell-heredoc UTF-8-fallgruve**: norske tegn (æøå, é) i commit-meldinger sendt direkte som
  `-m "..."`-strengargumenter gjennom bash-verktøyet kan bli mishandlet. Skriv i stedet meldingen
  til en fil via `cat > file << 'EOF' ... EOF` (quoted heredoc-terminator hindrer shell-tolkning)
  og bruk `git commit -F file`.

## 7. Reviewer-feedback på Thymeleaf-templates (mønster fra faktisk PR-review)

- `<span th:text="${...}"/>` (selvlukkende med Thymeleaf-attributt) er **gyldig, idiomatisk
  Thymeleaf** — malmotoren renderer det til et fullverdig `<span>...</span>` i HTML-en som
  faktisk sendes til nettleseren. Automatiserte HTML-lintere som ikke forstår Thymeleaf vil
  klage på dette som "ugyldig selvlukkende element" — vurder å avvise slik feedback med denne
  begrunnelsen, med mindre det faktisk er en statisk/plain span uten Thymeleaf-attributt.
- Kopier-lim-feil forekommer derimot reelt: sjekk om et `<header th:replace="... page_footer_fragment">`
  faktisk skal være `<footer ...>` — se hva søsterfiler/delte fragmenter (`fragments/general.html`)
  bruker som konvensjon, og rett *alle* forekomster av samme feil i søsterfiler samtidig, ikke
  bare filen kommentaren pekte på.
