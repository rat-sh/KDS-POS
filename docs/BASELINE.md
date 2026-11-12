# Baseline results

Baseline captured for the revision 0.2 compliance task on 2026-11-12. The frontend had already been converted to API-only operation before this baseline; these results describe that working tree, not the original demo-mode client.

## Environment

- Windows 10, PowerShell, Maven Wrapper 3.3.4 using Maven 3.9.9.
- Java 27 is installed. The Maven project compiles with `--release 21`.
- Bun 1.4.2 and the locked frontend dependencies are available.
- Docker Desktop 29.8.2 was started for the baseline run; Testcontainers used PostgreSQL 16.15.
- No `pdftotext` executable was initially installed. A verified, temporary Poppler 25.07.0 portable archive was downloaded and used from `target/pdf-tools` to extract all seven specification PDFs to `docs/text/`.
- The referenced `docs/KF-REV-002.md` was absent. The task attachment contains revision 0.2 decisions and requirements; see the limitation in [DEVIATIONS.md](./DEVIATIONS.md).

## Results

| Command | Result |
| --- | --- |
| `./mvnw -v` | PASS — Maven 3.9.9; wrapper metadata and JAR restored. |
| `./mvnw -DskipTests compile` | PASS — Spring sources compile. |
| `./mvnw test` | PASS — 1 context-load test against PostgreSQL 16.15 Testcontainers. |
| `bun install --frozen-lockfile` | PASS — no lockfile changes. |
| `bun run typecheck` | PASS. |
| `bun run lint` | PASS — 193 TypeScript files. |
| `bun run test` | PASS — 9 files, 17 tests. |
| `bun run check:structure` | PASS — 204 source files. |
| `bun run build` | PASS — Vite production build. |
| `bun run test:e2e` | NOT RUN — the API/gateway stack is not implemented yet. |
| `docker compose up --build -d` | NOT RUN — Compose currently contains only the database service; API and gateway are later phases. |

These are baseline checks only. They do not prove compliance with the seven specifications or revision 0.2. The backend context-load test initially failed because the test bootstrap admin display name was unset; setting a test-only display name made the baseline pass.

## Post-change verification — 2026-11-12

| Command / check | Result |
| --- | --- |
| `./mvnw -DskipTests compile` | PASS — Spring Boot 3.5.16 code compiles. |
| `./mvnw test` | PASS — 5 tests against PostgreSQL 16.15 Testcontainers, including station records, recall, and the idempotent paid-order ticket lifecycle. |
| `bun run typecheck` | PASS. |
| `bun run lint` | PASS — 194 TypeScript files. |
| `bun run test` | PASS — 9 files, 17 tests. |
| `bun run build` | PASS. |
| `bun run test:e2e` with no API running | FAIL as expected — the suite requires Spring health to be available. |
| `docker compose --env-file .env.example -p kitchenflow-smoke up --build -d` | PASS — API and gateway images built; all Compose services became healthy. |
| Gateway HTTP smoke checks | PASS — frontend and health routes returned 200; bootstrap login and authenticated admin-users request succeeded. |
| `bun run test:e2e` with Compose running | PASS — both connected-app scenarios. |
| `docker compose config` | PASS. |

The isolated smoke Compose project and its temporary PostgreSQL volume were removed after verification. These checks demonstrate that the current frontend/API stack starts and the tested flows respond; they do not establish compliance for the unimplemented endpoints and requirements still marked Partial or Not done in [TRACEABILITY.md](./TRACEABILITY.md).
