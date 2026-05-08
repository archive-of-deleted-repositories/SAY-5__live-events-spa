# Azure deploy stub

This Bicep template is a **production target reference**. It is **not
validated against an Azure subscription**, no resources are provisioned by
CI, and there is no `az deployment` step in this repo's pipeline.

## What's modelled

| Component                  | Resource                                      |
| -------------------------- | --------------------------------------------- |
| API control plane          | Azure Kubernetes Service (system + user pool) |
| Message bus                | Event Hubs namespace (Kafka protocol)         |
| Read-model                 | Azure Database for PostgreSQL Flexible Server |
| SPA hosting                | Azure Static Web Apps                         |
| Edge / WAF                 | Azure Front Door (Standard)                   |
| Telemetry                  | Application Insights                          |

## What's deliberately not modelled

- No managed identities for AKS -> Event Hubs / Postgres (left as TODO).
- No private endpoints. Production traffic should flow through a VNet, but
  doing so doubles the LOC and contradicts the "stub" goal.
- No Bicep modules / per-env parameter files. A real deployment would split
  this single file into modules and feed it from `dev.bicepparam` etc.
- No GitOps / Argo CD wiring. This repo only documents intent; it doesn't
  apply manifests.

## Validating syntactically

```bash
az bicep build --file main.bicep
```

That requires the `az` CLI plus the Bicep extension; the result is a plain
ARM JSON template.
