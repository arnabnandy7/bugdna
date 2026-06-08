package io.github.bugdna.spring;

import io.github.bugdna.BugDiff;
import io.github.bugdna.BugDna;
import io.github.bugdna.FailureContext;
import io.github.bugdna.Fingerprint;
import io.github.bugdna.FingerprintDiff;

import java.util.Objects;

/**
 * Spring-friendly facade for generating and comparing bugdna fingerprints.
 */
public class BugDnaSpringService {

    private final BugDnaFingerprintRepository repository;

    BugDnaSpringService(BugDnaFingerprintRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    /**
     * Generates and records a fingerprint.
     *
     * @param failure failure to fingerprint
     * @return generated fingerprint
     */
    public Fingerprint fingerprint(Throwable failure) {
        Fingerprint fingerprint = BugDna.generate(failure);
        repository.record(fingerprint);
        return fingerprint;
    }

    /**
     * Generates and records a fingerprint with operational impact context.
     *
     * @param failure failure to fingerprint
     * @param context operational impact context
     * @return generated fingerprint
     */
    public Fingerprint fingerprint(Throwable failure, FailureContext context) {
        Fingerprint fingerprint = BugDna.generate(failure, context);
        repository.record(fingerprint);
        return fingerprint;
    }

    /**
     * Compares two failures and returns the important fingerprint difference.
     *
     * @param oldException previous failure
     * @param newException current failure
     * @return fingerprint diff
     */
    public FingerprintDiff diff(Throwable oldException, Throwable newException) {
        return BugDiff.compare(oldException, newException);
    }
}
