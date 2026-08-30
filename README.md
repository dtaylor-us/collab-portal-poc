# Planning Results Portal / DPP Collaboration POC

This repository is a runnable vertical slice for long-running DPP result collaboration. The custom Planning Results Portal presents the business experience; PostgreSQL is authoritative for collaboration records; Camunda 8.9 coordinates human tasks, routing, timers, retries, incidents, and the rework loop.

## Run

Requirements: Docker with Compose 2.24+ (allocate roughly 4 CPUs/4 GB) and, for host builds, Java 21 plus Maven.

```shell
cp .env.example .env                 # optional; Compose has safe local defaults
docker compose up --build
```

| Component | URL/port | Credentials |
| --- | --- | --- |
| Planning Results Portal | http://localhost:8090 | none |
| Collaboration API | http://localhost:8081 | none |
| Operate | http://localhost:8088/operate | demo / demo |
| Tasklist | http://localhost:8088/tasklist | demo / demo |
| Camunda V2 REST | http://localhost:8088/v2 | unprotected locally |
| Camunda gRPC | localhost:26501 | unprotected locally |
| Camunda health/metrics | http://localhost:9601 | none |
| Worker health | http://localhost:8082/actuator/health | none |
| PostgreSQL | localhost:5432 | `.env` values |

The API deploys the BPMN and three forms from the authoritative `workflows/` directory at startup. No Modeler deployment is required. The Portal is served by nginx and proxies `/api/*` to the Collaboration API, keeping browser traffic on one origin.

## Portal flow

The Portal currently supports the full collaboration loop represented by the POC:

1. A DPP review appears in the review queue.
2. The Transmission Owner accepts the result or submits a correction.
3. MISO accepts the correction or returns it for rework.
4. The Transmission Owner revises and resubmits when rework is required.
5. The Portal shows correction versions and MISO disposition history through completion.

The UI generates an idempotency key for every mutation. If PostgreSQL commits but Camunda advancement fails and the API returns `502`, the open action retains the same key so the user can retry the exact command without duplicating business data.

A clean database has no reviews. Seed a review through the API before opening the Portal:

```shell
curl -i -X POST http://localhost:8081/api/dpp-reviews \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: create-001' \
  -d '{"dppResultId":"DPP-RESULT-001","transmissionOwnerId":"TO-DEMO"}'
```

Then open <http://localhost:8090> and perform the remaining review actions through the Portal.

## API smoke scenario

Every mutation requires a stable `Idempotency-Key`.

```shell
curl -i -X POST http://localhost:8081/api/dpp-reviews \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: create-001' \
  -d '{"dppResultId":"DPP-RESULT-001","transmissionOwnerId":"TO-DEMO"}'

# Set REVIEW_ID from the response.
curl -X POST http://localhost:8081/api/dpp-reviews/$REVIEW_ID/corrections \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: correction-001' \
  -d '{"comment":"Expected constraint value differs."}'
curl -X POST http://localhost:8081/api/dpp-reviews/$REVIEW_ID/miso-reject \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: reject-001' \
  -d '{"comment":"Please revise the constraint value."}'
curl -X POST http://localhost:8081/api/dpp-reviews/$REVIEW_ID/corrections \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: correction-002' \
  -d '{"comment":"Revised constraint value."}'
curl -X POST http://localhost:8081/api/dpp-reviews/$REVIEW_ID/miso-accept \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: accept-001' -d '{}'
curl http://localhost:8081/api/dpp-reviews/$REVIEW_ID
curl http://localhost:8081/api/dpp-reviews/$REVIEW_ID/corrections
```

Direct TO acceptance is `POST /api/dpp-reviews/{id}/accept`. Listing is `GET /api/dpp-reviews`. Invalid transitions return `409`; validation errors return `400`; a committed database mutation followed by workflow failure returns `502` with recovery guidance.

## Failure experiments

Set `WORKFLOW_SIMULATE_FAILURE=true` and restart the API. A mutation commits its business state and idempotency record, then the adapter fails. Set the flag back to false and repeat the exact request/key to retry only workflow advancement. This deliberately demonstrates that the database and Camunda do not share a transaction.

Set `NOTIFICATION_SIMULATE_FAILURE=true` before the one-minute reminder fires. The worker explicitly decrements retries and applies a five-second backoff; after retries reach zero Operate shows an incident. Disable the flag and increase/repair retries in Operate to recover. The deterministic key `<processInstanceKey>:TO_REVIEW_REMINDER:1` suppresses redelivery within a worker lifetime. Production requires a durable provider-side idempotency store; the in-memory POC guard does not survive a worker restart.

Build without containers:

```shell
mvn clean verify
docker compose config
```

The cross-system recovery smoke test remains available as:

```shell
bash scripts/verify-consistency-recovery.sh
```

See [architecture](docs/architecture.md), [scope](docs/scope.md), and [Portal UI](portal-ui/README.md).
