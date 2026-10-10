# Branch strategy — eap-fileview

## Long-lived branches

| Branch | Role | Maven revision | Java | Spring Boot | Upstream |
|---|---|---|---|---|---|
| `dev` | kkFileView compatibility/maintenance | 2.9.2 | 21 | 3.5.6 | kkFileView v5.0.3 |
| `main` | stable release gate (not a daily development line) | 3.0.1 until next promoted release | 21 | 4.0.8 | previously validated version |

The EAP fileview Maven group remains `io.github.eaopen`. Authentication, Maven Central signing and CNB credentials follow the existing eap-boot publication system. The EAP version does **not** equal the upstream kkFileView source version.

## Contribution and release flow

1. Start short-lived `feature/*` or `fix/*` branches from `boot4` for new development or `dev` for upstream compatibility maintenance.
2. Open a PR to its source branch; require Java 21 build, the portable test suite and a plain-JAR consumer compile check.
3. Transport selected generic fixes across lines by cherry-pick. **Never merge the full `dev` history into `boot4`**, because that would drag Spring Boot 3.5 POM/config into Boot 4.1.
4. Keep `main` as stable publication; when `boot4` is checked on Linux with LibreOffice, SevenZip JNI, Docker startup and representative CloudFile files, merge it to `main` through a PR and tag `v3.0.2` there.
5. For a 2.9.x maintenance release from `dev`, use a separate approved release decision and tag; don't change the Boot 4 mainline accidentally.

## Images and integration boundaries

- `dev` uses Java 21 and Boot 3.5.6, matching upstream dependency expectations.
- `boot4` 3.0.2 uses a new runtime dependency image tag `java21-sb41-r1`; it must not reuse `java21-sb4-r1` from Boot 4.0.
- The release output stays a normal Maven JAR plus Docker image (no executable JAR).
- Upstream kkFileView stays fetch-only and is **not** a publication remote.

Both development branches should run their own CI on push. Publishing Maven Central/CNB requires the separate EAP publication credentials and approvals; successful build alone is not evidence of publication.
