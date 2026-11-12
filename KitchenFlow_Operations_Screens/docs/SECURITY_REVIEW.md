# Security and operational boundaries

## Implemented

- Backend endpoints require a signed access token except login/refresh/logout, health, and the token-only public status board.
- Passwords are stored as BCrypt hashes. Refresh tokens are random, stored as SHA-256 hashes, rotated on refresh, and delivered in an HttpOnly, SameSite=Strict cookie.
- Access-token signing secret and initial admin credentials come from environment configuration; startup refuses short signing secrets and weak bootstrap passwords.
- Role checks cover POS, kitchen station access, management, administration, and audit. Kitchen users can only read and mutate their assigned stations.
- Database writes use Flyway-managed PostgreSQL tables, transactional order/payment/ticket flows, optimistic record versions, idempotency for order creation, and audit rows.
- The browser holds no database credentials or signing secret. API calls include correlation IDs. Production requires HTTPS and `REFRESH_COOKIE_SECURE=true`.

## Before operating a real outlet

- Configure a managed PostgreSQL instance, backups, TLS, a strong secret manager, trusted `ALLOWED_ORIGINS`, and the secure refresh-cookie setting.
- Enter verified products, prices, tax rates, and staff accounts. Migrations intentionally create no sample restaurant data.
- Integrate and reconcile a payment provider. Current Cash/UPI/Card actions record a staff-entered payment and do not authorize, capture, or verify electronic transactions.
- Add distributed login throttling, security event monitoring, and deployment-level rate limits before exposing the API to the public internet.
- Review production deployment, backup/restore, disaster recovery, and the seven draft system documents separately. This workspace does not include the production reverse proxy or deployment stack.

Never put `DATABASE_PASSWORD`, `KITCHENFLOW_JWT_SECRET`, or payment-provider secrets in frontend `VITE_*` variables or commit a populated `.env` file.
