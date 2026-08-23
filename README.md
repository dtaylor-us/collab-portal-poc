# Planning Portal Camunda 8 POC

This repository is a modular proof of concept for a simplified DPP Results Review workflow. It preserves the current Camunda BPMN and forms while providing runnable shells for the capability-specific collaboration API and asynchronous notification worker.

## Business scenario

DPP results are published, reviewed by a Transmission Owner (TO), and either accepted or corrected. MISO reviews corrections and can accept them or return them for TO rework. The loop continues until acceptance. A non-interrupting timer may create a `send-to-review-reminder` job.

## Modules

- `workflows/`: Desktop Modeler BPMN and deployed forms.
- `services/dpp-collaboration-api/`: bounded-context Spring Boot shell. It intentionally has no endpoints, persistence, or workflow commands yet.
- `workers/planning-notification-worker/`: Camunda worker that simulates reminder delivery.
- `portal-ui/`: placeholder boundary for a future custom Portal UI; no frontend stack is selected.
- `docs/`: architecture, scope, and decision records.

## Prerequisites

- Java 21
- Maven 3.6.3 or newer
- Camunda 8.9 Run
- Camunda Desktop Modeler

## Build and run

Validate the complete reactor:

```shell
mvn clean verify
```

Run the collaboration API shell:

```shell
mvn -pl services/dpp-collaboration-api spring-boot:run
```

With Camunda 8 Run already running locally, start the notification worker:

```shell
mvn -pl workers/planning-notification-worker spring-boot:run
```

The worker defaults to unauthenticated Camunda Run at REST `http://localhost:8080` and gRPC `http://localhost:26500`. Override these with `CAMUNDA_REST_ADDRESS`, `CAMUNDA_GRPC_ADDRESS`, and `CAMUNDA_AUTH_METHOD`. Other starter settings, including credentials for a future secured environment, can use Spring Boot's documented environment-variable mapping. Never commit credentials.

## Deploy the workflow

1. Open `workflows/dpp-result-review.bpmn` in Desktop Modeler.
2. Deploy the BPMN and each form in `workflows/forms/` to the same Camunda 8.9 cluster.
3. Keep the deployed form IDs `to-review`, `miso-correction-review`, and `to-correction-rework`; the BPMN refers to these IDs rather than file paths.

No application in this repository automatically deploys resources or starts a process.

## Reminder experiment

1. Start Camunda 8 Run.
2. Deploy the BPMN and forms through Desktop Modeler.
3. Start the notification worker.
4. Start and advance a process manually until the non-interrupting timer creates `send-to-review-reminder`.
5. Observe the worker's simulated reminder log and confirm the job completes. Correlation identifiers are logged only when available; the worker does not require invented variables.

## Current limitations

There is no custom UI, REST contract, domain model, SQL/ADLS adapter, identity configuration, production deployment, or real notification provider. ADLS replication is out of scope. Notification delivery is simulated; a real provider must be idempotent because Camunda job processing is at-least-once. See `docs/scope.md` for the increment boundaries.
