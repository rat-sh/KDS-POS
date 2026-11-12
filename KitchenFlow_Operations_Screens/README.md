# KitchenFlow Operations Screens

React and Vite frontend for POS, payments, kitchen display, the public order board, and manager/admin screens. All live menu, order, payment, and ticket state comes from the Spring Boot API in the parent folder. There is no browser mock-data mode.

## Run

Install [Bun](https://bun.sh/), start PostgreSQL and the Spring Boot API using the [workspace instructions](../README.md), then run:

```sh
bun install
bun run dev
```

The dev server listens on port 5173 and proxies `/api` and `/actuator` to `http://localhost:8080`. Set `VITE_API_PROXY_TARGET` in `.env.local` when the API runs elsewhere. `VITE_API_BASE_URL` defaults to `/api/v1`.

## Commands

```sh
bun run typecheck
bun run lint
bun run test
bun run test:e2e
bun run build
```

## Source layout

- `src/app` — startup, session restoration, and role routing
- `src/api` — authenticated HTTP client, endpoint wrappers, and response mapping
- `src/domain` — order, pricing, permissions, queue, and SLA rules
- `src/features` — POS, kitchen, boards, admin, inventory, and reports
- `src/shared` — reusable UI and hooks
- `src/store` — short-lived client state refreshed from the API
- `src/styles` — global and feature styles
- `docs` — frontend/API behavior and security notes
- `e2e` — browser-flow tests

Only public `VITE_*` values belong in this client. Keep database passwords, JWT secrets, and payment credentials in the backend environment.
