# AGENTS.md — eap-fileview

Active coding guidance for this repository. The original kkFileView agent instructions are preserved at [doc/kkfileview/AGENTS.md](doc/kkfileview/AGENTS.md) as history; **this file supersedes them**.

## Scope and branches

- Project: `io.github.eaopen:eap-fileview`, based on kkFileView v5.0.3.
- **`dev` is the GitHub default and only maintained development line:** revision 3.0.2, **JDK 21 / Spring Boot 4.1.1 / Spring Framework 7 / Jackson 3**.
- `boot3` is a **frozen historical reference** (2.9.2 / Boot 3.5.6), not a maintained branch; do not add features, routine CI or separate release processes there.
- The upstream remote is **fetch-only**. Preserve original kkFileView credits/licensing, avoid copying upstream Boot 3 POM or old deployment automation into `dev`.

## Product boundaries

- Generic preview engine: HTTP(S) sources, format identification, preview routing and conversion, cache, FreeMarker/static viewers.
- The calling application is responsible for identities, permissions, storage, versions, and short-lived file access URLs.
- Downstream applications may consume the regular JAR or HTTP endpoint, keeping domain models, proprietary converters and authentication integrations outside this repository.
- Reuse existing generic interfaces before adding abstractions: `FilePreviewProvider`, `OfficePreviewFallbackConverter`, `EapFileViewComponents`.
- Do not weaken URL/host trust controls, path validation, temporary URL secrecy, or source-file integrity for convenience.

## Project layout

- `pom.xml`: version `revision`, JDK and Boot dependency versions.
- `server/pom.xml`: build a **regular JAR**; copy runtime dependency JARs for Docker. No executable Boot JAR.
- `server/src/main/java/cn/keking/`: preview engine and `ServerMain`.
- `server/src/main/java/io/github/eaopen/fileview/`: embeddable Spring component entry.
- `server/src/main/resources/web/`: FreeMarker viewer templates.
- `server/src/main/resources/static/`: viewer JS/CSS/assets.
- `server/src/main/config/`: application and test runtime properties.
- `Dockerfile`: thin app layer; `docker/Dockerfile.base`: stable Java/LibreOffice/dependencies layer.
- `compose.yaml` and `compose.build.yaml`: pull-only and local-build Compose configurations.
- `.github/workflows/`: active GitHub CI, CNB/Central Maven publishing and image workflows.
- `doc/kkfileview/`: preserved upstream README, original AGENTS, CI/E2E notes. These are **not current runbooks**.

## Development workflow

1. Start from `dev`. Use a short-lived `feature/*` or `fix/*` branch when practical; open PRs back to `dev`.
2. Group commits by meaningful capability (preview, source, Office, Docker, publication, docs), keeping diffs focused and tests proportional.
3. Review actual service/controller/template paths instead of guessing from upstream documentation.
4. Maintain Java 21 / Boot 4.1 compatibility when bringing in upstream patches; cherry-pick or selectively reimplement tested generic fixes only.
5. Do not publish to Central, overwrite immutable image tags, change unrelated production Nginx or touch historical `boot3` unless explicitly directed.
6. Tag an approved `dev` commit for release only after real CI, Linux dependency checks, Docker startup and representative preview samples pass.

## Build and tests

From the repo root on JDK 21 / Maven 3.9:

```bash
mvn -B -pl server -am -DskipTests clean package
mvn -B -pl server -am \
  '-Dtest=*,!EncodingTests,!ServerMainTests,!ArchiveTraversalSecurityTests' \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

The portable suite excludes three tests requiring local LibreOffice and/or SevenZipJBinding platform support. Do not present those tests as passing without a suitable environment and actual results. Use targeted tests, representative Office/PDF/XMind/archive/CAD samples, and E2E checks when changing format behavior.

## Deployment and publishing

- Ordinary Maven dependency: `io.github.eaopen:eap-fileview:3.0.2`.
- Stable Docker base: `ghcr.io/eaopen/eap-fileview-base:java21-sb41-r1`; final image: `ghcr.io/eaopen/eap-fileview:3.0.2`. Do not reuse the Boot 4.0 runtime-dependency base for Boot 4.1.
- The base includes Java 21, LibreOffice, fonts and resolved dependencies; the app layer adds only regular JAR and config. Local build must prepare Maven output first.
- Central and CNB use **the same organization publishing identity and authorization practices as eap-boot**. Credentials belong in GitHub Actions environments / private Maven settings, never in code.
- No `systemPath` JARs, redistributable unlicensed commercial libraries, leaked signing keys or invented publication claims.
- Docker and Maven Central availability must be verified independently; compilation alone does not establish release availability.

## Diagnosing previews

Follow URL trust/attribute parsing → `FilePreviewFactory` and chosen handler → fetch/conversion/cache → FreeMarker viewer/assets. Check relevant modules such as `OnlinePreviewController`, `FileHandlerService`, `OfficeFilePreviewImpl`, `PdfFilePreviewImpl`, `CompressFileReader`, `web/pdf.ftl`, `web/xmind.ftl` and `web/compress.ftl`.

For rendering failures, verify actual response content and short-lived ticket behavior (not only HTTP status). Never log signed full source URLs or silently change the original document. State what passed, what was not tested, and whether config affects only DEV or runtime deployment.

## Authoritative docs

[README.cn.md](README.cn.md) · [README.md](README.md) · [doc/BRANCHING.md](doc/BRANCHING.md) · [doc/EXTENSION_RELEASE.md](doc/EXTENSION_RELEASE.md) · [HTTP preview integration](doc/HTTP_PREVIEW_INTEGRATION.md) · [Preview capability review](doc/PREVIEW_CAPABILITY_REVIEW.md).

Original upstream reference: [doc/kkfileview/README.cn.md](doc/kkfileview/README.cn.md), [doc/kkfileview/README.md](doc/kkfileview/README.md), [doc/kkfileview/AGENTS.md](doc/kkfileview/AGENTS.md). Their removed Windows startup scripts and upstream CI workflows are not part of the active `dev` release workflow.
