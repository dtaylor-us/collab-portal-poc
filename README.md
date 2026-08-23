# Planning Results Portal / DPP Collaboration POC

This increment is a runnable backend vertical slice for a long-running DPP result review. PostgreSQL is authoritative for collaboration records; Camunda 8.9 coordinates user tasks, routing, timers, retries, incidents, and the rework loop.

## Run

Requirements: Docker with Compose 2.24+ (allocate roughly 4 CPUs/4 GB) and, for host builds, Java 21 plus Maven.

```shell
cp .env.example .env                 # optional; Compose has safe local defaults
docker compose up --build
```

| Component | URL/port | Credentials |
| --- | --- | --- |
| Collaboration API | http://localhost:8081 | none |
| Operate | http://localhost:8088/operate | demo / demo |
| Tasklist | http://localhost:8088/tasklist | demo / demo |
| Camunda V2 REST | http://localhost:8088/v2 | unprotected locally |
| Camunda gRPC | localhost:26501 | unprotected locally |
| Camunda health/metrics | http://localhost:9601 | none |
| Worker health | http://localhost:8082/actuator/health | none |
| PostgreSQL | localhost:5432 | `.env` values |

The API deploys the BPMN and three forms from the authoritative `workflows/` directory at startup. No Modeler deployment is required.

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

See [architecture](docs/architecture.md) and [scope](docs/scope.md).
