# Security Policy

## Supported Versions

Security fixes are provided for the latest release in the current minor release
line.

| Version | Supported |
| --- | --- |
| 1.0.x | Yes |
| Earlier versions | No |

Users should upgrade to the latest version available from Maven Central before
reporting an issue.

## Reporting a Vulnerability

Please report suspected security vulnerabilities privately through
[GitHub Security Advisories](https://github.com/arnabnandy7/bugdna/security/advisories/new).

Do not open a public GitHub issue, discussion, or pull request for an
undisclosed vulnerability.

Include as much of the following information as possible:

- The affected BugDNA module and version
- A description of the vulnerability and its potential impact
- Steps or a minimal example that reproduces the issue
- Relevant configuration, runtime, Java, and framework versions
- Any known mitigations or suggested fixes

Remove secrets, credentials, personal data, and other sensitive information
from all reports and reproduction material.

## Response Process

The maintainer will aim to:

- Acknowledge the report within 7 days
- Investigate and assess its severity
- Coordinate remediation and disclosure with the reporter
- Publish a security advisory and patched release when appropriate

Resolution time depends on the issue's complexity and impact. Please allow a
reasonable remediation period before public disclosure.

## Scope

Reports concerning BugDNA source code, published artifacts, the CLI, or the
Spring Boot starter are in scope. Vulnerabilities in third-party dependencies
should also be reported to the affected upstream project when appropriate.

General bugs, feature requests, and non-security hardening suggestions may be
reported through the project's public
[issue tracker](https://github.com/arnabnandy7/bugdna/issues).
