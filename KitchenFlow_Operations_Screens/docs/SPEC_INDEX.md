# KitchenFlow project notes

The workspace now contains both the React/Vite client and a Spring Boot API backed by PostgreSQL. The API implements the frontend's live order, payment-record, kitchen ticket, menu, staff, station, availability, settings, audit, and public-board endpoints. The API contract and its operational limits are in [Frontend and API contract](./FRONTEND_BACKEND_INTEGRATION.md) and [Security and operational boundaries](./SECURITY_REVIEW.md).

The system design PDFs remain in the parent workspace's `kds_documnet` folder. They are draft requirements for a broader product, including deployment and payment-provider integration that this codebase does not implement. Do not use those drafts as proof that external payment processing or production deployment has been completed.

To run the connected stack locally, follow the [workspace README](../../README.md).
