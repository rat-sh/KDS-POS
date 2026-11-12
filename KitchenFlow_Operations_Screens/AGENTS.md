# Frontend project notes

This folder contains the KitchenFlow React/Vite client. The Spring Boot API and PostgreSQL migrations are in the parent workspace. Keep frontend changes inside this project unless the task explicitly requires a coordinated API change.

## Development

- Use Bun for frontend install, scripts, and lockfile updates. `bun.lock` is the only frontend dependency lockfile.
- Start PostgreSQL and the Spring Boot API using the parent [README](../README.md), then run `bun install` and `bun run dev` here.
- Vite proxies `/api` and `/actuator` to `http://localhost:8080` by default.
- Do not add browser mock users, sample orders, localStorage order state, or a mock data switch. API-backed state is the source of truth.
- Only public `VITE_*` configuration belongs in the browser. Keep database/JWT/payment secrets in the backend environment.

## Source map

- `src/app/` — startup, session restoration, and role routing
- `src/api/` — authenticated HTTP client, endpoint wrappers, and mapping
- `src/domain/` — order, ticket, pricing, permissions, queue, and SLA rules
- `src/features/` — POS, kitchen, boards, inventory, admin, and reporting UI
- `src/shared/` — reusable UI and hooks
- `src/store/` — short-lived client state refreshed from the API
- `src/styles/` — global and feature styles
- `docs/` — API integration and security notes
- `e2e/` — connected frontend/API smoke tests; the backend must be running

## Change safety

- Check git status before edits and preserve unrelated user work.
- Never expose secrets, tokens, `.env` files, agent/tool folders, or binaries.
- Do not commit, push, merge, or open/close a PR without the user's approval.
- Before claiming the connected flow works, run the checks supported by the local environment and report anything that could not run.
