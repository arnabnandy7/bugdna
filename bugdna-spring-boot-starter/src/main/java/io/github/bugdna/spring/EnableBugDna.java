package io.github.bugdna.spring;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables bugdna services and automatic capture of unhandled Spring web exceptions.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({
        BugDnaAutoConfiguration.class,
        BugDnaWebAutoConfiguration.class,
        BugDnaWebFluxAutoConfiguration.class,
        BugDnaMetricsAutoConfiguration.class
})
public @interface EnableBugDna {
}
