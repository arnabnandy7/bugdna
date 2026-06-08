package io.github.bugdna.spring;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Actuator endpoint for recent bugdna fingerprints.
 */
@Endpoint(id = "bugdna")
public class BugDnaEndpoint {

    private final BugDnaFingerprintRepository repository;

    BugDnaEndpoint(BugDnaFingerprintRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns recent bugdna fingerprint data.
     *
     * @return endpoint payload
     */
    @ReadOperation
    public Map<String, Object> bugdna() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("count", repository.size());
        payload.put("recent", repository.recent());
        return payload;
    }
}
