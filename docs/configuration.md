# Configuration Reference

Spring properties use the `bugdna` prefix.

| Property | Type | Default | Description |
| --- | --- | --- | --- |
| `bugdna.enabled` | boolean | `true` | Enables BugDNA auto-configuration |
| `bugdna.log-enabled` | boolean | `true` | Enables automatic MVC and WebFlux exception logs |
| `bugdna.mdc-enabled` | boolean | `true` | Adds BugDNA fields to MDC during automatic web logging |
| `bugdna.otel-enabled` | boolean | `true` | Adds BugDNA fields to the current OpenTelemetry span when the OpenTelemetry API is present |
| `bugdna.include-stack-trace` | boolean | `false` | Includes the exception stack trace in automatic logs |
| `bugdna.recent-limit` | integer | `50` | Maximum recent snapshots retained in memory |
| `bugdna.actuator.enabled` | boolean | `true` | Enables the Actuator endpoint when Actuator is present |

`bugdna.recent-limit` must be at least `1`.

## Recommended Production Baseline

```properties
bugdna.enabled=true
bugdna.log-enabled=true
bugdna.mdc-enabled=true
bugdna.otel-enabled=true
bugdna.include-stack-trace=false
bugdna.recent-limit=50
bugdna.actuator.enabled=true
```

## Stack Trace Tradeoff

Enabling stack traces improves debugging context but increases log volume and may
expose exception messages or application data. BugDNA fingerprints themselves do not
use exception messages.

## Actuator Exposure

Creating the endpoint does not automatically expose it over HTTP. Use Spring Boot's
management configuration:

```properties
management.endpoints.web.exposure.include=health,bugdna,prometheus
```

Apply the same authentication and network controls used for other management
endpoints.

## Logging Pattern

To show the MDC fingerprint:

```properties
logging.pattern.console=%-5level [%X{bugdna}] %logger{36} - %msg%n
```

The MDC value is present during BugDNA's automatic exception log call and is removed
afterward.

## OpenTelemetry

BugDNA only enriches the current span. It does not start spans, configure a tracer
provider, or export telemetry. Applications that already use OpenTelemetry can
query existing traces by attributes such as `bugdna` or `bugdna.id`.
