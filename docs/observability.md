# Observability

The Spring starter integrates with logs, SLF4J MDC, Actuator, Micrometer, and
Prometheus.

## Automatic Logs

Unhandled Spring MVC and WebFlux exceptions produce:

```text
ERROR [BUGDNA-7A3F21] Unhandled exception fingerprinted by bugdna
```

Set `bugdna.include-stack-trace=true` to include the exception.

## MDC Fields

When `bugdna.mdc-enabled=true`, the following fields are present during the automatic
BugDNA log call:

| Key | Example |
| --- | --- |
| `bugdna` | `BUGDNA-7A3F21` |
| `bugdna.id` | `BUGDNA-7A3F21` |
| `bugdna.confidence` | `90` |
| `bugdna.category` | `DATABASE` |
| `bugdna.family` | `DATABASE_CONNECTIVITY` |
| `bugdna.priority` | `UNKNOWN` |

BugDNA removes these keys in a `finally` block after logging. In WebFlux this MDC
scope covers BugDNA's synchronous log call only; it is not propagated as Reactor
context. BugDNA does not attach MDC to arbitrary core-library calls.

## Actuator Endpoint

When Actuator is present, the starter registers the `bugdna` endpoint.

```properties
management.endpoints.web.exposure.include=health,bugdna
```

Query it locally:

```bash
curl http://localhost:8080/actuator/bugdna
```

Example response shape:

```json
{
  "count": 1,
  "recent": [
    {
      "observedAt": "2026-06-12T00:00:00Z",
      "id": "BUGDNA-7A3F21",
      "rootCause": "java.lang.NullPointerException",
      "signature": "UserService#getUser",
      "stabilityScore": 90,
      "category": "UNKNOWN",
      "family": "UNKNOWN",
      "priority": "UNKNOWN"
    }
  ]
}
```

The endpoint is in-memory and bounded by `bugdna.recent-limit`.

## Micrometer

When a `MeterRegistry` bean exists, BugDNA registers:

| Meter | Meaning |
| --- | --- |
| `bugdna.failures.total` | Failures recorded by the starter since startup |
| `bugdna.unique.failures` | Distinct fingerprint IDs observed since startup |

These are gauges backed by the starter repository's lifetime counters.

## Prometheus

Add the Prometheus registry to the application and expose the endpoint:

```properties
management.endpoints.web.exposure.include=health,prometheus
```

Prometheus-normalized names:

```text
bugdna_failures_total
bugdna_unique_failures
```

Metrics reset when the application restarts.

Example PromQL:

```promql
bugdna_unique_failures
```

```promql
delta(bugdna_failures_total[15m])
```

The first query shows currently known unique IDs. The second shows failure growth
over 15 minutes when the process has not restarted during that range.

## Core Applications

The core artifact has no logging or metrics dependency. Use `Fingerprint` and
`FailureTracker` values with the observability system chosen by the application.
