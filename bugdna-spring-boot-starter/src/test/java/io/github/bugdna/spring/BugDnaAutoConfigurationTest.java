package io.github.bugdna.spring;

import io.github.bugdna.Fingerprint;
import io.github.bugdna.FingerprintDiff;
import io.github.bugdna.FailureContext;
import io.github.bugdna.FailureTracker;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.server.WebExceptionHandler;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class BugDnaAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    BugDnaAutoConfiguration.class,
                    BugDnaActuatorAutoConfiguration.class
            ));

    @Test
    void registersCoreSpringService() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(BugDnaProperties.class);
            assertThat(context).hasSingleBean(BugDnaFingerprintRepository.class);
            assertThat(context).hasSingleBean(FailureTracker.class);
            assertThat(context).hasSingleBean(BugDnaSpringService.class);
        });
    }

    @Test
    void backsSpringServiceWithRecentFingerprintRepository() {
        contextRunner.run(context -> {
            BugDnaSpringService service = context.getBean(BugDnaSpringService.class);
            BugDnaFingerprintRepository repository = context.getBean(BugDnaFingerprintRepository.class);
            FailureTracker tracker = context.getBean(FailureTracker.class);

            Fingerprint fingerprint = service.fingerprint(
                    failureAt("com.example.UserService", "get", 10)
            );

            assertThat(repository.recent()).hasSize(1);
            assertThat(repository.recent().get(0).getId()).isEqualTo(fingerprint.getId());
            assertThat(tracker.getTotalOccurrences()).isEqualTo(1);
            assertThat(tracker.failures().get(0).getId()).isEqualTo(fingerprint.getId());
        });
    }

    @Test
    void springServiceRecordsContextFingerprintsAndDiffsFailures() {
        BugDnaFingerprintRepository repository = new BugDnaFingerprintRepository(5);
        BugDnaSpringService service = new BugDnaSpringService(repository);

        Fingerprint fingerprint = service.fingerprint(
                failureAt("com.example.UserService", "get", 10),
                FailureContext.of(100, 10, false)
        );
        FingerprintDiff diff = service.diff(
                failureAt("com.example.UserRepository", "find", 10),
                failureAt("com.example.CustomerRepository", "find", 12)
        );

        assertThat(fingerprint.getPriority().name()).isEqualTo("HIGH");
        assertThat(repository.recent()).hasSize(1);
        assertThat(diff.getSummary()).isEqualTo("Repository Layer Changed");
    }

    @Test
    void repositoryKeepsBoundedNewestFirstImmutableSnapshots() {
        BugDnaFingerprintRepository repository = new BugDnaFingerprintRepository(2);
        Fingerprint first = fingerprintAt("com.example.FirstService", "run", 1);
        Fingerprint second = fingerprintAt("com.example.SecondService", "run", 2);
        Fingerprint third = fingerprintAt("com.example.ThirdService", "run", 3);

        repository.records(first);
        repository.records(second);
        repository.records(third);

        List<BugDnaFingerprintRepository.FingerprintSnapshot> recent = repository.recent();
        assertThat(repository.size()).isEqualTo(2);
        assertThat(repository.totalCount()).isEqualTo(3);
        assertThat(repository.uniqueCount()).isEqualTo(3);
        assertThat(recent).hasSize(2);
        assertThat(recent.get(0).getId()).isEqualTo(third.getId());
        assertThat(recent.get(0).getObservedAt()).isNotNull();
        assertThat(recent.get(0).getRootCause()).isEqualTo(third.getRootCause());
        assertThat(recent.get(0).getSignature()).isEqualTo(third.getSignature());
        assertThat(recent.get(0).getStabilityScore()).isEqualTo(third.getStabilityScore());
        assertThat(recent.get(0).getCategory()).isEqualTo(third.getCategory().name());
        assertThat(recent.get(0).getPriority()).isEqualTo(third.getPriority().name());
        assertThat(recent).extracting("id").doesNotContain(first.getId());
        assertThatThrownBy(recent::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void repositoryRejectsInvalidLimitAndNullFingerprints() {
        assertThatThrownBy(() -> new BugDnaFingerprintRepository(0))
                .isInstanceOf(IllegalArgumentException.class);

        BugDnaFingerprintRepository repository = new BugDnaFingerprintRepository(1);

        assertThatThrownBy(() -> repository.records(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void endpointReturnsRecentFingerprintPayload() {
        BugDnaFingerprintRepository repository = new BugDnaFingerprintRepository(5);
        repository.records(fingerprintAt("com.example.UserService", "get", 10));

        Map<String, Object> payload = new BugDnaEndpoint(repository).bugdna();

        assertThat(payload).containsEntry("count", 1);
        assertThat((List<?>) payload.get("recent")).hasSize(1);
    }

    @Test
    void disablesAutoConfigurationWithProperty() {
        contextRunner
                .withPropertyValues("bugdna.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(BugDnaSpringService.class));
    }

    @Test
    void bindsProperties() {
        contextRunner
                .withPropertyValues(
                        "bugdna.log-enabled=false",
                        "bugdna.mdc-enabled=false",
                        "bugdna.include-stack-trace=true",
                        "bugdna.recent-limit=3"
                )
                .run(context -> {
                    BugDnaProperties properties = context.getBean(BugDnaProperties.class);

                    assertThat(properties.isLogEnabled()).isFalse();
                    assertThat(properties.isMdcEnabled()).isFalse();
                    assertThat(properties.isIncludeStackTrace()).isTrue();
                    assertThat(properties.getRecentLimit()).isEqualTo(3);
                });
    }

    @Test
    void propertiesRejectInvalidRecentLimit() {
        BugDnaProperties properties = new BugDnaProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThatThrownBy(() -> properties.setRecentLimit(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exposesActuatorEndpointWhenActuatorIsPresent() {
        contextRunner.run(context -> {
            assertThat(Endpoint.class).isNotNull();
            assertThat(context).hasSingleBean(BugDnaEndpoint.class);
        });
    }

    @Test
    void exposesMicrometerFailureMetrics() {
        contextRunner
                .withUserConfiguration(MeterRegistryConfiguration.class)
                .withConfiguration(AutoConfigurations.of(BugDnaMetricsAutoConfiguration.class))
                .run(context -> {
                    BugDnaSpringService service = context.getBean(BugDnaSpringService.class);
                    SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);

                    service.fingerprint(failureAt("com.example.UserService", "get", 10));
                    service.fingerprint(failureAt("com.example.UserService", "get", 10));
                    service.fingerprint(failureAt("com.example.OrderService", "save", 20));

                    assertThat(registry.get("bugdna.failures.total").gauge().value()).isEqualTo(3);
                    assertThat(registry.get("bugdna.unique.failures").gauge().value()).isEqualTo(2);
                });
    }

    @Test
    void registersWebExceptionLoggerInWebApplications() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        BugDnaAutoConfiguration.class,
                        BugDnaWebAutoConfiguration.class
                ))
                .run(context -> {
                    assertThat(context).hasSingleBean(BugDnaExceptionLogger.class);
                    assertThat(context).hasBean("bugDnaExceptionLogger");
                    assertThat(context.getBean("bugDnaExceptionLogger"))
                            .isInstanceOf(HandlerExceptionResolver.class);
                });
    }

    @Test
    void enableAnnotationRegistersAutomaticExceptionCapture() {
        new WebApplicationContextRunner()
                .withUserConfiguration(BugDnaEnabledApplication.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(BugDnaSpringService.class);
                    assertThat(context).hasSingleBean(FailureTracker.class);
                    assertThat(context).hasSingleBean(BugDnaExceptionLogger.class);
                });
    }

    @Test
    void disablesWebExceptionLoggerWithProperty() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        BugDnaAutoConfiguration.class,
                        BugDnaWebAutoConfiguration.class
                ))
                .withPropertyValues("bugdna.log-enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(BugDnaExceptionLogger.class));
    }

    @Test
    void registersWebFluxExceptionLoggerInReactiveApplications() {
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        BugDnaAutoConfiguration.class,
                        BugDnaWebFluxAutoConfiguration.class
                ))
                .run(context -> {
                    assertThat(context).hasSingleBean(BugDnaWebFluxExceptionLogger.class);
                    assertThat(context).hasBean("bugDnaWebFluxExceptionLogger");
                    assertThat(context.getBean("bugDnaWebFluxExceptionLogger"))
                            .isInstanceOf(WebExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(BugDnaExceptionLogger.class);
                });
    }

    @Test
    void disablesWebFluxExceptionLoggerWithProperty() {
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        BugDnaAutoConfiguration.class,
                        BugDnaWebFluxAutoConfiguration.class
                ))
                .withPropertyValues("bugdna.log-enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(BugDnaWebFluxExceptionLogger.class));
    }

    @Test
    void webFluxExceptionLoggerRecordsLogsAndReEmitsFailure(CapturedOutput output) {
        BugDnaProperties properties = new BugDnaProperties();
        BugDnaFingerprintRepository repository = new BugDnaFingerprintRepository(5);
        FailureTracker tracker = new FailureTracker();
        BugDnaWebFluxExceptionLogger logger = new BugDnaWebFluxExceptionLogger(
                new BugDnaSpringService(repository, tracker),
                properties
        );
        Throwable failure = failureAt("com.example.ReactiveController", "show", 10);

        assertThatThrownBy(() -> logger.handle(null, failure).block())
                .isSameAs(failure);

        assertThat(logger.getOrder()).isEqualTo(Integer.MIN_VALUE);
        assertThat(repository.recent()).hasSize(1);
        assertThat(tracker.getTotalOccurrences()).isEqualTo(1);
        assertThat(output)
                .contains("[" + repository.recent().get(0).getId()
                        + "] Unhandled exception fingerprinted by bugdna");
        assertThat(MDC.get("bugdna")).isNull();
        assertThat(MDC.get("bugdna.id")).isNull();
    }

    @Test
    void exceptionLoggerLogsFingerprintAndClearsMdc(CapturedOutput output) {
        BugDnaProperties properties = new BugDnaProperties();
        BugDnaFingerprintRepository repository = new BugDnaFingerprintRepository(5);
        FailureTracker tracker = new FailureTracker();
        BugDnaExceptionLogger logger = new BugDnaExceptionLogger(
                new BugDnaSpringService(repository, tracker),
                properties
        );

        assertThat(logger.resolveException(
                null,
                null,
                null,
                (Exception) failureAt("com.example.UserController", "show", 10)
        )).isNull();

        assertThat(logger.getOrder()).isEqualTo(Integer.MIN_VALUE);
        assertThat(repository.recent()).hasSize(1);
        assertThat(tracker.getTotalOccurrences()).isEqualTo(1);
        assertThat(output)
                .contains("[" + repository.recent().get(0).getId()
                        + "] Unhandled exception fingerprinted by bugdna");
        assertThat(MDC.get("bugdna")).isNull();
        assertThat(MDC.get("bugdna.id")).isNull();
        assertThat(MDC.get("bugdna.confidence")).isNull();
    }

    @Test
    void exceptionLoggerCanLogStackTracesAndSkipMdc(CapturedOutput output) {
        BugDnaProperties properties = new BugDnaProperties();
        properties.setIncludeStackTrace(true);
        properties.setMdcEnabled(false);
        BugDnaExceptionLogger logger = new BugDnaExceptionLogger(
                new BugDnaSpringService(new BugDnaFingerprintRepository(5)),
                properties
        );

        logger.resolveException(
                null,
                null,
                null,
                (Exception) failureAt("com.example.UserController", "show", 10)
        );

        assertThat(output).contains("java.lang.NullPointerException");
        assertThat(MDC.get("bugdna")).isNull();
        assertThat(MDC.get("bugdna.id")).isNull();
    }

    private static Throwable failureAt(String className, String methodName, int lineNumber) {
        NullPointerException failure = new NullPointerException();
        failure.setStackTrace(new StackTraceElement[] {
                new StackTraceElement(className, methodName, className + ".java", lineNumber)
        });
        return failure;
    }

    private static Fingerprint fingerprintAt(String className, String methodName, int lineNumber) {
        return io.github.bugdna.BugDna.generate(failureAt(className, methodName, lineNumber));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableBugDna
    static class BugDnaEnabledApplication {
    }

    @Configuration(proxyBeanMethods = false)
    static class MeterRegistryConfiguration {

        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
