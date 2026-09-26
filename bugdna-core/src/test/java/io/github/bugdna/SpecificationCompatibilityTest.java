package io.github.bugdna;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpecificationCompatibilityTest {

    @Test
    void identityFixturesMatchJavaImplementation() throws IOException {
        for (Map<String, Object> tc : loadFixture("identity.json")) {
            String desc = (String) tc.get("description");
            Map<String, Object> input = asMap(tc.get("input"));
            Map<String, Object> expected = asMap(tc.get("expected"));

            String rootCause = (String) input.get("rootCause");
            List<String> rawFrames = new ArrayList<>();
            for (Object fObj : asList(input.get("frames"))) {
                Map<String, Object> f = asMap(fObj);
                rawFrames.add(f.get("class") + "#" + f.get("method"));
            }
            List<String> causeChain = toStringList(input.get("causeChain"));

            Fingerprint fp = BugDna.generateFromSynthetic(
                    rootCause,
                    rawFrames,
                    causeChain,
                    FailureContext.unknown()
            );

            if (expected.containsKey("id")) {
                assertEquals(expected.get("id"), fp.getId(), desc);
            }
            if (expected.containsKey("stabilityScore")) {
                assertEquals(asInt(expected.get("stabilityScore")), fp.getStabilityScore(), desc);
            }
            if (expected.containsKey("signature")) {
                assertEquals(expected.get("signature"), fp.getSignature(), desc);
            }
            if (expected.containsKey("qualifiedSignature")) {
                assertEquals(expected.get("qualifiedSignature"), fp.getQualifiedSignature(), desc);
            }
            if (expected.containsKey("failureChain")) {
                assertEquals(toStringList(expected.get("failureChain")), fp.getFailureChain(), desc);
            }
            if (expected.containsKey("frameCount")) {
                assertEquals(asInt(expected.get("frameCount")), fp.getFrames().size(), desc);
            }
        }
    }

    @Test
    void categoryFixturesMatchJavaImplementation() throws IOException {
        for (Map<String, Object> tc : loadFixture("category.json")) {
            String desc = (String) tc.get("description");
            Map<String, Object> input = asMap(tc.get("input"));
            Map<String, Object> expected = asMap(tc.get("expected"));

            FailureCategory actual = BugDna.categorize((String) input.get("rootCause"));
            assertEquals(expected.get("category"), actual.name(), desc);
        }
    }

    @Test
    void familyFixturesMatchJavaImplementation() throws IOException {
        for (Map<String, Object> tc : loadFixture("family.json")) {
            String desc = (String) tc.get("description");
            Map<String, Object> input = asMap(tc.get("input"));
            Map<String, Object> expected = asMap(tc.get("expected"));

            FailureCategory category = FailureCategory.valueOf((String) input.get("category"));
            FailureFamily actual = BugDna.classifyFamilyFromEvidence(
                    category,
                    (String) input.get("evidence")
            );
            assertEquals(expected.get("family"), actual.name(), desc);
        }
    }

    @Test
    void priorityFixturesMatchJavaImplementation() throws IOException {
        for (Map<String, Object> tc : loadFixture("priority.json")) {
            String desc = (String) tc.get("description");
            Map<String, Object> input = asMap(tc.get("input"));
            Map<String, Object> expected = asMap(tc.get("expected"));

            long occurrences = asLong(input.get("occurrences"));
            long affectedUsers = asLong(input.get("affectedUsers"));
            boolean fatal = Boolean.TRUE.equals(input.get("fatal"));

            FailureContext ctx = (occurrences < 0 || affectedUsers < 0)
                    ? FailureContext.unknown()
                    : FailureContext.of(occurrences, affectedUsers, fatal);
            assertEquals(expected.get("priority"), BugDna.prioritize(ctx).name(), desc);
        }
    }

    @Test
    void similarityFixturesMatchJavaImplementation() throws IOException {
        for (Map<String, Object> tc : loadFixture("similarity.json")) {
            String desc = (String) tc.get("description");
            Map<String, Object> input = asMap(tc.get("input"));
            Map<String, Object> expected = asMap(tc.get("expected"));

            Fingerprint first = toFingerprint(asMap(input.get("first")));
            Fingerprint second = toFingerprint(asMap(input.get("second")));
            Similarity sim = BugSimilarity.compare(first, second);

            if (expected.containsKey("percentage")) {
                assertEquals(asInt(expected.get("percentage")), sim.getPercentage(), desc);
            }
            assertEquals(expected.get("isLikelyRelated"), sim.isLikelyRelated(), desc);
        }
    }

    @Test
    void diffFixturesMatchJavaImplementation() throws IOException {
        for (Map<String, Object> tc : loadFixture("diff.json")) {
            String desc = (String) tc.get("description");
            Map<String, Object> input = asMap(tc.get("input"));
            Map<String, Object> expected = asMap(tc.get("expected"));

            Fingerprint oldFp = toFingerprint(asMap(input.get("old")));
            Fingerprint newFp = toFingerprint(asMap(input.get("new")));
            FingerprintDiff diff = BugDiff.compare(oldFp, newFp);

            assertEquals(expected.get("summary"), diff.getSummary(), desc);
        }
    }

    @Test
    void driftFixturesMatchJavaImplementation() throws IOException {
        for (Map<String, Object> tc : loadFixture("drift.json")) {
            String desc = (String) tc.get("description");
            Map<String, Object> input = asMap(tc.get("input"));
            Map<String, Object> expected = asMap(tc.get("expected"));

            Fingerprint oldFp = toFingerprint(asMap(input.get("old")));
            Fingerprint newFp = toFingerprint(asMap(input.get("new")));
            FingerprintDrift drift = FingerprintDriftDetector.detect(oldFp, newFp);

            if (expected.containsKey("signatureDriftPercentage")) {
                assertEquals(
                        asInt(expected.get("signatureDriftPercentage")),
                        drift.getSignatureDriftPercentage(),
                        desc
                );
            }
            assertEquals(
                    expected.get("isPossibleCodePathChange"),
                    drift.isPossibleCodePathChange(),
                    desc
            );
        }
    }

    @Test
    void timelineFixturesMatchJavaImplementation() throws IOException {
        for (Map<String, Object> tc : loadFixture("timeline.json")) {
            String desc = (String) tc.get("description");
            Map<String, Object> input = asMap(tc.get("input"));
            Map<String, Object> expected = asMap(tc.get("expected"));

            FailureTracker tracker = new FailureTracker(asInt(input.get("timelineLimit")));
            for (Object evObj : asList(input.get("events"))) {
                Map<String, Object> ev = asMap(evObj);
                tracker.capture(
                        dummyFingerprint((String) ev.get("id")),
                        Instant.ofEpochSecond(asLong(ev.get("epochSecond")))
                );
            }

            List<FailureOccurrence> timeline = tracker.timeline();
            if (expected.containsKey("size")) {
                assertEquals(asInt(expected.get("size")), timeline.size(), desc);
            }
            List<String> actualOrder = new ArrayList<>();
            for (FailureOccurrence occ : timeline) {
                actualOrder.add(occ.getId());
            }
            assertEquals(toStringList(expected.get("order")), actualOrder, desc);
        }
    }

    @Test
    void burstFixturesMatchJavaImplementation() throws IOException {
        for (Map<String, Object> tc : loadFixture("burst.json")) {
            String desc = (String) tc.get("description");
            Map<String, Object> input = asMap(tc.get("input"));
            Map<String, Object> expected = asMap(tc.get("expected"));

            FailureTracker tracker = new FailureTracker();
            for (Object evObj : asList(input.get("events"))) {
                Map<String, Object> ev = asMap(evObj);
                tracker.capture(
                        dummyFingerprint((String) ev.get("id")),
                        Instant.ofEpochSecond(asLong(ev.get("epochSecond")))
                );
            }

            List<FailureBurst> bursts = tracker.bursts(
                    asLong(input.get("minimumPeakRatePerMinute")),
                    Duration.ofSeconds(asLong(input.get("maximumIdleGapSeconds")))
            );

            assertEquals(asInt(expected.get("burstCount")), bursts.size(), desc);
            List<Object> expectedBursts = asList(expected.get("bursts"));
            for (int i = 0; i < expectedBursts.size(); i++) {
                Map<String, Object> exp = asMap(expectedBursts.get(i));
                FailureBurst actual = bursts.get(i);
                assertEquals(exp.get("id"), actual.getId(), desc);
                assertEquals(asLong(exp.get("occurrences")), actual.getOccurrences(), desc);
                if (exp.containsKey("peakRatePerMinute")) {
                    assertEquals(
                            asLong(exp.get("peakRatePerMinute")),
                            actual.getPeakRatePerMinute(),
                            desc
                    );
                }
            }
        }
    }

    private static Fingerprint toFingerprint(Map<String, Object> map) {
        String id = (String) map.get("id");
        String rootCause = map.containsKey("rootCause")
                ? (String) map.get("rootCause")
                : "java.lang.RuntimeException";
        String qualifiedSignature = (String) map.get("qualifiedSignature");
        int hashIdx = qualifiedSignature.lastIndexOf('#');
        String className = hashIdx >= 0 ? qualifiedSignature.substring(0, hashIdx) : qualifiedSignature;
        String methodName = hashIdx >= 0 ? qualifiedSignature.substring(hashIdx + 1) : "";
        int dotIdx = className.lastIndexOf('.');
        String simpleClass = (dotIdx >= 0 ? className.substring(dotIdx + 1) : className).replace('$', '.');
        String signature = methodName.isEmpty() ? simpleClass : simpleClass + "#" + methodName;
        List<String> frames = toStringList(map.get("frames"));
        List<String> causeChain = map.containsKey("causeChain")
                ? toStringList(map.get("causeChain"))
                : Collections.singletonList(rootCause);

        return new Fingerprint(
                id,
                rootCause,
                signature,
                qualifiedSignature,
                frames,
                Collections.singletonList(simpleClass),
                causeChain,
                "fixture",
                90,
                FailurePriority.UNKNOWN,
                FailureCategory.UNKNOWN,
                FailureFamily.UNKNOWN
        );
    }

    private static Fingerprint dummyFingerprint(String id) {
        return new Fingerprint(
                id,
                "java.lang.RuntimeException",
                "Example#run",
                "com.example.Example#run",
                Collections.singletonList("com.example.Example#run"),
                Collections.singletonList("Example"),
                Collections.singletonList("java.lang.RuntimeException"),
                "fixture",
                90,
                FailurePriority.UNKNOWN,
                FailureCategory.UNKNOWN,
                FailureFamily.UNKNOWN
        );
    }

    private static List<Map<String, Object>> loadFixture(String fileName) throws IOException {
        Path path = Paths.get("../specification/fixtures", fileName);
        if (!Files.exists(path)) {
            path = Paths.get("specification/fixtures", fileName);
        }
        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        List<Object> rawList = asList(new MiniJsonParser(content).parse());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : rawList) {
            result.add(asMap(item));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object obj) {
        return (Map<String, Object>) obj;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object obj) {
        if (obj == null) {
            return Collections.emptyList();
        }
        return (List<Object>) obj;
    }

    private static List<String> toStringList(Object obj) {
        List<Object> raw = asList(obj);
        List<String> out = new ArrayList<>(raw.size());
        for (Object item : raw) {
            out.add((String) item);
        }
        return out;
    }

    private static int asInt(Object obj) {
        return ((Number) obj).intValue();
    }

    private static long asLong(Object obj) {
        return ((Number) obj).longValue();
    }

    private static final class MiniJsonParser {
        private final String src;
        private int pos;

        MiniJsonParser(String src) {
            this.src = src;
        }

        Object parse() {
            skipWhitespace();
            return readValue();
        }

        private Object readValue() {
            skipWhitespace();
            char ch = src.charAt(pos);
            if (ch == '{') {
                return readObject();
            }
            if (ch == '[') {
                return readArray();
            }
            if (ch == '"') {
                return readString();
            }
            if (ch == 't' || ch == 'f') {
                return readBoolean();
            }
            if (ch == 'n') {
                pos += 4;
                return null;
            }
            return readNumber();
        }

        private Map<String, Object> readObject() {
            pos++; // '{'
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (src.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                pos++; // ':'
                Object val = readValue();
                map.put(key, val);
                skipWhitespace();
                char ch = src.charAt(pos++);
                if (ch == '}') {
                    break;
                }
            }
            return map;
        }

        private List<Object> readArray() {
            pos++; // '['
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (src.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (true) {
                list.add(readValue());
                skipWhitespace();
                char ch = src.charAt(pos++);
                if (ch == ']') {
                    break;
                }
            }
            return list;
        }

        private String readString() {
            pos++; // '"'
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char ch = src.charAt(pos++);
                if (ch == '"') {
                    return sb.toString();
                }
                if (ch == '\\') {
                    char esc = src.charAt(pos++);
                    if (esc == 'u') {
                        String hex = src.substring(pos, pos + 4);
                        pos += 4;
                        sb.append((char) Integer.parseInt(hex, 16));
                    } else if (esc == 'n') {
                        sb.append('\n');
                    } else if (esc == 'r') {
                        sb.append('\r');
                    } else if (esc == 't') {
                        sb.append('\t');
                    } else {
                        sb.append(esc);
                    }
                } else {
                    sb.append(ch);
                }
            }
            throw new IllegalStateException("Unterminated JSON string");
        }

        private Boolean readBoolean() {
            if (src.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            pos += 5;
            return Boolean.FALSE;
        }

        private Number readNumber() {
            int start = pos;
            while (pos < src.length()) {
                char ch = src.charAt(pos);
                if ((ch >= '0' && ch <= '9') || ch == '-' || ch == '.' || ch == 'e' || ch == 'E' || ch == '+') {
                    pos++;
                } else {
                    break;
                }
            }
            String token = src.substring(start, pos);
            if (token.indexOf('.') >= 0 || token.indexOf('e') >= 0 || token.indexOf('E') >= 0) {
                return Double.parseDouble(token);
            }
            return Long.parseLong(token);
        }

        private void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }
    }
}
