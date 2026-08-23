# Architecture

The POC separates domain state, workflow state, user experience, and integration execution.

| Concern | Owner |
| --- | --- |
| Portal user experience | Planning Results Portal |
| DPP collaboration rules | DPP Results Collaboration API |
| Comments, corrections, dispositions | Future Azure SQL implementation |
| Published results and study artifacts | ADLS/data platform |
| Tasks, timers, routing, rework | Camunda 8 |
| Notification job execution | Planning Notification Worker |
| Message delivery | Future notification provider |
| ADLS replication | Out of scope |

Camunda variables should contain identifiers and routing facts, not complete domain records or large study artifacts. The collaboration API follows the dependency direction `web/infrastructure → application → domain`; its domain boundary is independent of Spring Web, Camunda, SQL, and ADLS implementations.

## Consistency boundary

The important cross-system sequence is:

```text
Persist domain submission → advance Camunda workflow
```

A later increment will evaluate idempotency and failure recovery between these operations. This scaffold does not implement an outbox or claim that the two systems share a transaction.

The notification worker is likewise at-least-once: a future provider adapter must use an idempotency key and tolerate redelivery before real messages are sent.
