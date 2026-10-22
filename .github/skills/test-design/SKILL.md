---
name: test-design
description: "Use before writing, extending or reviewing tests. Chooses the few tests that protect changed logic, at the level that owns it with collaborators mocked, and verifies them with coverage and a deliberate break. Not for only running existing suites."
license: Digitaliseringsdirektoratet
---

# Test design

The suite verifies behaviour as a sum: each test covers one unit at its own level, and a test earns its place by failing when behaviour that matters breaks.

## Choose what to test

Test changed branches, mappings, validation and error paths; behaviour that is hard to verify by reading code, such as queries, pagination, transactions, concurrency and access control; and fixed bugs, with a test that fails without the fix.

Skip code the change does not touch, including unchanged API versions; framework behaviour, getters, delegation and identity mappings; and values or layers that run the same path again. Parameterize only over values that take different paths.

## Choose the level

- Test logic where it lives, usually the service, with repositories, clients and other services mocked. Example: `V1ApiServiceTest` captures the `PageRequest` to verify sort mapping.
- Test JPQL/SQL in repository tests against Testcontainers MariaDB; a mock cannot show that a query works.
- Test endpoints with MockMvc and a mocked service only when the HTTP contract changes: status, validation, authentication or serialization. Example: `V1RelyingPartiesEndpointTest`.
- Do not write end-to-end tests through several layers to prove what the level tests already cover.

## Write

1. List the changed branches from the diff, and reuse the nearest test class and its helpers.
2. Choose data where a wrong implementation gives a different result. Example: for sorting, insert in an order that differs from every expected order, and add one row a filter must exclude.
3. Assert the unit's result or its call to a mocked collaborator. Avoid timestamps created milliseconds apart.
4. Add an assertion to an existing test when it covers the case.

## Verify

- Coverage: every changed line and branch runs. Surefire has a fixed `argLine` here, so attach JaCoCo through the environment:

  ```bash
  cd apps/<app>
  agent=$(ls ~/.m2/repository/org/jacoco/org.jacoco.agent/*/org.jacoco.agent-*-runtime.jar | tail -1)
  JAVA_TOOL_OPTIONS="-javaagent:$agent=destfile=$PWD/target/jacoco.exec,includes=no.*" ./mvnw -q test -Dtest=<TestClass>
  ./mvnw -q org.jacoco:jacoco-maven-plugin:0.8.14:report
  ```

  Read `target/site/jacoco/` for the changed classes. If the agent is missing, run `./mvnw org.jacoco:jacoco-maven-plugin:0.8.14:prepare-agent` once.
- Deliberate break: temporarily break the changed logic, confirm a test fails, and restore the code.
- Remove tests that add no coverage or failure signal beyond the rest.

Report the behaviours covered, the coverage result and the deliberate break.
