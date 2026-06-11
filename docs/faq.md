# FAQ

## Does BugDNA Store Exceptions in a Database?

No. The core tracker and starter repository are in-memory only.

## Do Counts Survive Restart?

No. Export Micrometer metrics or send reports to persistent monitoring when history
is required.

## Is the Tracker Available Without Spring?

Yes:

```java
FailureTracker tracker = new FailureTracker();
```

## Is the Tracker Available in Spring?

Yes. The starter registers the same core `FailureTracker` as an injectable bean.

## Does the Core Library Modify MDC?

No. The core has no SLF4J dependency. MDC integration exists only in the Spring
starter's automatic exception logger.

## Does BugDNA Replace Spring Exception Handling?

No. The MVC resolver captures the failure and lets Spring continue handling it.

## Are Exception Messages Used in Fingerprints?

No. Messages are intentionally excluded.

## Can Line Number Changes Alter an ID?

No. Line numbers are excluded. Class, method, exception type, or normalized call-path
changes can alter the ID.

## Is `@EnableBugDna` Required?

No. The starter supports Spring Boot auto-configuration. The annotation is an
explicit alternative.

## Does Automatic Capture Support WebFlux?

No. Current automatic capture targets servlet Spring MVC.

## How Do I Get a Top-10 Report?

```java
tracker.topFailureReport();
```

## How Do I Reset Counts?

```java
tracker.clear();
```
