# ADR 0001: POC application boundaries

- Status: Accepted
- Date: 2026-08-23

## Decision

Use a capability-specific DPP Collaboration API and a separate asynchronous Planning Notification Worker. Camunda owns workflow state; external stores own durable domain state. Keep these components in a modular Maven monorepo during the POC to avoid premature microservice decomposition.

## Consequences

The API can evolve around DPP collaboration use cases without becoming a universal Planning API. Notification delivery can evolve independently behind a delivery interface. Cross-system consistency and idempotency remain explicit later POC concerns rather than being hidden in the initial scaffold.
