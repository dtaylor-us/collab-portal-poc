# Planning Results Portal UI

This directory contains the custom Planning Results Portal experience for the DPP collaboration POC.

The UI is intentionally lightweight: static HTML, CSS, and JavaScript served by nginx. nginx proxies `/api/*` to the DPP Collaboration API so the browser stays on one origin and the backend remains the capability boundary.

## Current user experience

The Portal supports:

- review queue with status summary, filtering, and search
- review details and Camunda correlation metadata
- Transmission Owner acceptance
- Transmission Owner correction submission
- MISO correction acceptance or rejection for rework
- Transmission Owner rework/resubmission
- correction-version history
- MISO disposition history
- safe retry of a `502` coordination failure using the same idempotency key

The UI does not persist authoritative collaboration or workflow state. PostgreSQL remains authoritative for review business data and Camunda remains authoritative for orchestration state.

## Visual direction

The prototype uses an original MISO-inspired visual language based on the public `misoenergy.org` experience: strong dark-blue navigation/hero surfaces, restrained green/gold accents, broad content spacing, prominent section hierarchy, and utility navigation. It does not copy the public site's markup, assets, or logo.

## Run

From the repository root:

```shell
docker compose up --build
```

Open <http://localhost:8090>.

The portal nginx container proxies API requests to `dpp-collaboration-api:8080`; no browser CORS configuration is required for the local Compose topology.
