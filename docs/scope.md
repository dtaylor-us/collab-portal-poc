# POC scope

## Implemented

- Capability-specific REST API and application/domain boundaries
- PostgreSQL persistence with Flyway, correction versioning, and dispositions
- Camunda process startup and V2 user-task query/completion behind a workflow port
- Application lifecycle validation with consistent HTTP errors
- Persisted command idempotency and deterministic failure/retry experiment
- Explicit notification retry/backoff and POC duplicate suppression
- Read/list/history APIs, health endpoints, correlated logs
- Authoritative BPMN/form startup deployment
- Docker Compose local runtime and non-root multi-stage Java 21 images

## Intentionally out of scope

Portal UI, ADLS/ADF/Azure SQL, real email, Entra/enterprise identity, SUGAR, files/scanning, multiple TOs/constraints, complex authorization, Kafka/event streaming, production HA/Kubernetes, and production-grade reconciliation/provider idempotency remain excluded.

## Known POC limits

- A workflow response lost after engine acceptance remains an uncertain outcome; command idempotency prevents duplicate database mutation but cannot prove exactly-once Camunda completion.
- Notification duplicate protection is process-local and resets on worker restart.
- Local Camunda uses file-backed H2 secondary storage. PostgreSQL is the collaboration application's store, not Camunda's store.
- Compose is a developer topology, not a production deployment.
