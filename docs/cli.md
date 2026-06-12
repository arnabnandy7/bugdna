# Command-Line Interface

The `bugdna-cli` module analyzes BugDNA fingerprint IDs already present in text log
files. It does not parse Java stack traces or generate new fingerprints from raw
exception text.

## Build

Build the executable JAR:

```bash
mvn -pl bugdna-cli clean package
```

The result is:

```text
bugdna-cli/target/bugdna-cli-1.0.1.jar
```

Run it directly:

```bash
java -jar bugdna-cli/target/bugdna-cli-1.0.1.jar analyze app.log
```

Repository launcher scripts are also provided:

```bash
bin/bugdna analyze app.log
```

Windows:

```powershell
bin\bugdna.cmd analyze app.log
```

Add the repository `bin` directory to `PATH` to use the short command:

```bash
bugdna analyze app.log
```

## Analyze a Log File

Given a log file containing repeated IDs:

```text
2026-06-12 ERROR [BUGDNA-001] Payment failed
2026-06-12 ERROR [BUGDNA-002] Customer lookup failed
2026-06-12 ERROR [BUGDNA-001] Payment failed
```

Run:

```bash
bugdna analyze app.log
```

Output:

```text
Unique Failures: 2

BUGDNA-001 : 2
BUGDNA-002 : 1
```

Results are sorted by occurrence count descending, then fingerprint ID for
deterministic ties.

## Compare Log Files

Compare an older log with a newer log:

```bash
bugdna compare app-v1.log app-v2.log
```

Output:

```text
New Failure Signatures:
3

Resolved:
7
```

`New Failure Signatures` counts IDs found only in the newer log. `Resolved` counts
IDs found only in the older log. A fingerprint found in both logs is unchanged even
when its occurrence count differs.

## Matching Rules

The analyzer:

- Reads the file as UTF-8
- Finds every `BUGDNA-*` hexadecimal token
- Matches IDs case-insensitively and prints uppercase IDs
- Counts multiple fingerprint occurrences on the same line
- Ignores lines without a BugDNA ID

An empty log or a log without fingerprint IDs returns:

```text
Unique Failures: 0
```

## Exit Codes

| Code | Meaning |
| --- | --- |
| `0` | Analysis completed |
| `2` | Invalid command or arguments |
| `3` | Invalid or unreadable log file |

Invalid usage prints:

```text
Usage:
  bugdna analyze <log-file>
  bugdna compare <old-log-file> <new-log-file>
```
