package io.github.bugdna.spring;

import io.github.bugdna.Fingerprint;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.assertj.core.api.Assertions.assertThat;

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
            assertThat(context).hasSingleBean(BugDnaSpringService.class);
        });
    }

    @Test
    void backsSpringServiceWithRecentFingerprintRepository() {
        contextRunner.run(context -> {
            BugDnaSpringService service = context.getBean(BugDnaSpringService.class);
            BugDnaFingerprintRepository repository = context.getBean(BugDnaFingerprintRepository.class);

            Fingerprint fingerprint = service.fingerprint(
                    failureAt("com.example.UserService", "get", 10)
            );

            assertThat(repository.recent()).hasSize(1);
            assertThat(repository.recent().get(0).getId()).isEqualTo(fingerprint.getId());
        });
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
    void exposesActuatorEndpointWhenActuatorIsPresent() {
        contextRunner.run(context -> {
            assertThat(Endpoint.class).isNotNull();
            assertThat(context).hasSingleBean(BugDnaEndpoint.class);
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
    void disablesWebExceptionLoggerWithProperty() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        BugDnaAutoConfiguration.class,
                        BugDnaWebAutoConfiguration.class
                ))
                .withPropertyValues("bugdna.log-enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(BugDnaExceptionLogger.class));
    }

    private static Throwable failureAt(String className, String methodName, int lineNumber) {
        NullPointerException failure = new NullPointerException();
        failure.setStackTrace(new StackTraceElement[] {
                new StackTraceElement(className, methodName, className + ".java", lineNumber)
        });
        return failure;
    }
}
