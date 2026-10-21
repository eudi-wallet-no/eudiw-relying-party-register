---
name: update-etsi-119-612-trustlist-issuer
description: Add an issuer service to the ETSI TS 119 612 trust list in the test environment, either for an existing TSP or a new TSP.
license: Digitaliseringsdirektoratet
---

# Update the ETSI TS 119 612 test trust list

Use this skill when adding an issuer service to the ETSI TS 119 612 trust list. All changes must be limited to the test environment.

## Before editing

1. Read `apps/eudiw-trustlist-service/UPDATE_ROUTINE.md` and inspect `apps/eudiw-trustlist-service/src/main/resources/application-test.yaml` and the related provider entries in `application.yaml`.
2. Collect the TSP information and issuer service information from the user or their supplied source. Do not invent or silently infer missing or conflicting values. Ask for clarification when needed.
3. Verify the service certificate is valid Base64-encoded X.509 data and inspect its subject and validity dates. Its organization name and organization identifier must match the TSP name and trade name. Every Norwegian `trade-name` value must use `NTRNO-NOFOR.<organization-number>` (for example, `NTRNO-NOFOR.974760673`), not the legacy `NTRNO-<organization-number>` format. Check both `lang-no` and `lang-en`, preserve the organization number digits, and compare them with the certificate's organization identifier. Store the certificate as one Base64 line in YAML, without PEM markers or whitespace.
4. Find the TSP in `application-test.yaml` by its organization number (`trade-name`), cross-checking the certificate subject. If more than one entry matches or the identifiers conflict, stop and ask rather than choosing an entry.
5. Check whether the same service or certificate is already listed. If it is, do not add a duplicate or replace it silently; ask the user how to proceed.

## Existing TSP

When the organization number matches an existing service provider:

- Add only the new service under that provider's existing `services` list.
- Preserve all existing TSP metadata, including name, trade name, information URIs, website, email, and postal address. Do not replace or normalize those values from the new submission.
- Update `trustlist-service.tsl612.scheme-information.list-issue-date-time` in `application-test.yaml` to the current date at `00:00:00.00Z`.
- Do not create another provider entry.

## New TSP

When no existing provider matches the organization number:

- Add one provider under `trustlist-service.tsl612.service-providers` in `application-test.yaml`.
- Include the TSP name, trade name, information URIs, email, website, postal address, and the new service. Follow the key structure and multilingual conventions of the existing entries.
- Store the email as a `mailto:` URI in `email`, adding the scheme if the supplied value does not include it.
- Use the supplied service name and service type identifier. Set `starting-time` to the certificate's `notBefore` time if no other authoritative start time was supplied; ask the user to resolve any conflict.
- Update `trustlist-service.tsl612.scheme-information.list-issue-date-time` in `application-test.yaml` to the current date at `00:00:00.00Z`.

## Scope and verification

- Never add or copy this issuer into `application-dev.yaml`, `application-systest.yaml`, `application-prod.yaml`, or any other environment or trust list.
- Do not change the ETSI 119 602 lists; this procedure is only for ETSI TS 119 612.
- Review the diff to confirm that only `application-test.yaml` changed and, for an existing TSP, that its existing metadata was left untouched.
- Run the focused profile test from the repository root:

  ```sh
  mvn -f apps/eudiw-trustlist-service/pom.xml -Dtest=Trustlist612TestProfileDSSTest test
  ```

- Confirm the test passes, which exercises the test profile and validates the generated signed trust list against the ETSI schema. Report any failure; do not treat a failed validation as a successful update.

## Final report

End with a clear, concise report that makes the checks performed and the changes made explicit. Include a status for each applicable check, using `PASS`, `FAIL`, `BLOCKED`, or `NOT RUN`; never imply an unperformed check passed. Cover at least:

- TSP and certificate identity match, certificate Base64/X.509 validity, and validity dates.
- Duplicate check and confirmation that changes are limited to the test ETSI 119 612 trust list.
- The configuration change made, including whether an existing TSP or a new TSP was updated.
- Diff/scope review and the focused profile test result.

For each item, state the relevant result briefly. If a check fails or is blocked, explain why and clearly state whether the update is incomplete. Report the exact test command and its outcome.
