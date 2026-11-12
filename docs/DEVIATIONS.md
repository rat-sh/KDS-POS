# Deviations and source limitations

This file records differences from the seven draft specifications and the revision 0.2 task instructions. A deviation is not considered accepted as product scope unless explicitly marked as a decision supplied by the user.

| ID | Difference | Disposition |
| --- | --- | --- |
| D-001 | The user-provided task explicitly retains simulated Cash/UPI/Card payment records and forbids a payment-provider integration. The broader specifications discuss refunds and payment processing. | User decision; keep payments simulated and list unimplemented refund/provider requirements in traceability. |
| D-002 | The user-provided task explicitly forbids Kafka, Redis, Spring Cloud Gateway, and microservices. | User decision; implement the documented Nginx edge plus in-process API pipeline only. |
| D-003 | `docs/KF-REV-002.md` is referenced by the task but was not present in the workspace. Only selected lifecycle, endpoint, and test identifiers were included in the attached task text. | Source gap remains. This implementation records only the facts in the attachment; the full revision acceptance criteria cannot be independently verified. |
| D-004 | The repository has no Git metadata at its root. The frontend subfolder has an empty Git repository with no commits and all project files untracked. | Environment/repository setup limitation. No files have been staged or committed. |
| D-005 | Docker Desktop was initially stopped; it was started for the baseline run. | Resolved: Testcontainers passes, Compose config passes, API/gateway images build, the isolated stack reached healthy, and it passed gateway/auth/browser smoke checks. Smoke containers and their temporary database volume were removed. |
| D-006 | `pdftotext` was initially absent. | Used verified Poppler 25.07.0 portable `pdftotext -layout` to convert the seven required PDFs. The temporary archive was removed after conversion; extracted text remains in `docs/text/`. |
| D-007 | The initial repository used Spring Boot 4.1.2 while the task context and specifications name Spring Boot 3.x. | Resolved: parent and dependencies now use Spring Boot 3.5.16; compile and Testcontainers context test pass. |
| D-008 | Existing schema and code use `order_header`, `kitchen_ticket`, `app_user`, `audit_entry`, `menu_item`, `menu_category`, `modifier_group`, `menu_modifier`, `payment`, `refresh_token`, `idempotency_record`, and `outlet_config`. | Decision supplied in the task: retain these delivered table names and map the DBD schema to them. |
| D-009 | The existing backend used `spring-boot-starter-data-jpa` although business persistence uses JDBC. | Resolved: replaced with `spring-boot-starter-jdbc`; no JPA dependency or JPA settings remain. |
| D-010 | The attachment identifies K-08, K-09, and K-10, but does not include the exact endpoint table paths for K-08 and K-09. | K-08 accept and K-09 serve paths were inferred from the existing lifecycle actions. K-10 is implemented as `GET /stations/{stationId}/records`; confirm against the complete revision document when available. |

## Revision 0.2 details available in the attachment

The attachment specifies ticket states `NEW`, `ACCEPTED`, `IN_PROGRESS`, `DONE`, `SERVED`; persisted accepted/served actors and timestamps; additional `K-08`, `K-09`, `K-10`; and additional tests `UT-18`, `UT-19`, `PC-15`, `PC-16`, `CC-16`, `CC-17`, `RM-06`, `SS-20`, `PF-17`, `AT-12`. It directs that `TICKETS_NOT_SERVED` be represented as `INVALID_TRANSITION` with `details.reason`, and calls for an Nginx edge plus in-process request pipeline. This is a partial summary, not a replacement for the missing revision document.
