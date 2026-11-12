# KF-REV-002 — partial requirements transcribed from the task attachment

> Source limitation: the attachment references a separate revision 0.2 document, but that file is not present in this workspace. This note transcribes only the details explicitly included in the user-provided task. It is not the complete or authoritative revision document.

## Explicit ticket lifecycle additions

- Ticket statuses: `NEW`, `ACCEPTED`, `IN_PROGRESS`, `DONE`, `SERVED`, and existing cancellation state as applicable.
- Acceptance and service actions persist the actor and timestamps (the attachment associates these with FR-REC-01 through FR-REC-09).
- Add endpoints K-08, K-09, and K-10. The attachment explicitly defines K-10 as a ticket record query with accepted/served/all state filters, token search, accepted/served actor display names, durations, recall count, and CSV support; complete K-08/K-09 detail is unavailable here.
- Add tests UT-18, UT-19, PC-15, PC-16, CC-16, CC-17, RM-06, SS-20, PF-17, and AT-12.

## Explicit corrections

- Use `INVALID_TRANSITION` for prior transition-specific errors. A failed completion because tickets are not served has `details.reason = TICKETS_NOT_SERVED`.
- Use `STALE_VERSION` for optimistic-lock conflicts.
- Route the application through an Nginx edge and the specified in-process request pipeline.

For the rest of revision 0.2, see the attachment itself. Do not infer missing endpoint schemas, acceptance criteria, or test cases from this partial note.
