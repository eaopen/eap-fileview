# eap-fileview

An **independently deployable and extensible file preview engine**, derived from [kkFileView v5.0.3](https://github.com/kekingcn/kkFileView). It runs as a Docker service or can be embedded through a **regular Maven JAR**, independently of any particular business application.

**Default and actively maintained branch: `dev` — v3.0.3 (unreleased), Java 21, Spring Boot 4.1.2.**

[Spring Boot 4.1.2 upgrade review](doc/SPRING_BOOT_412_UPGRADE.md) · [中文说明](README.cn.md) · [Extensions and publishing](doc/EXTENSION_RELEASE.md) · [Branch rules](doc/BRANCHING.md) · [Original kkFileView documentation](doc/kkfileview/README.md)

## What it provides

- Preview Office/WPS, PDF/OFD, images, XMind, archives, text, CAD, 3D and other supported formats. The [preserved upstream README](doc/kkfileview/README.md) contains the full format list, examples and background.
- `FilePreviewProvider`, `OfficePreviewFallbackConverter` and `EapFileViewComponents` for downstream extensions.
- A generic URL-based preview service; the calling application owns user authentication, storage permission checks, short-lived download URLs and domain-specific workflows.
- Consumer-specific data models, proprietary converters, permission logic and integrations belong in the calling application.
- Targeted upstream bug fixes without overwriting the Boot 4.1 dependency baseline.

## System architecture

The solution diagram outlines the system boundary, HTTP endpoints, preview routing, optional extension contracts, and Maven/Docker deployment. It is a scalable SVG adaptation of the GPT-authored architecture design.

![eap-fileview solution and system architecture](doc/img/eap-fileview-solution-architecture.svg)

FilePreviewProvider and OfficePreviewFallbackConverter are optional. Authentication, authorization and signed URL expiry belong to the calling system.

## Branch status

| Branch | Purpose | Runtime |
| --- | --- | --- |
| **`dev`** | **Default, only actively maintained development branch** | **3.0.3 (unreleased) / Java 21 / Spring Boot 4.1.2** |
| `boot3` | Historical compatibility snapshot; **no separate maintenance** | 2.9.2 / Java 21 / Spring Boot 3.5.6 |

Create focused feature/fix branches from `dev`; after CI, Docker startup and representative preview checks, tag the approved `dev` commit for release. See [branch policy](doc/BRANCHING.md).

## Build the regular JAR

Requires JDK 21 and Maven 3.9:

```bash
java -version
mvn -B -pl server -am -DskipTests clean package
```

Output: `server/target/eap-fileview-3.0.3.jar`, with `server/target/dependency/` used only to assemble the self-contained runtime. **No executable JAR or legacy distribution archive.**

Run the portable test suite (three additional environment-dependent tests need LibreOffice or SevenZip JNI locally):

```bash
mvn -B -pl server -am \
  '-Dtest=*,!EncodingTests,!ServerMainTests,!ArchiveTraversalSecurityTests' \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

## Docker deployment

### Maven artifacts and Docker layers

```mermaid
flowchart LR
    src["Java 21 / Maven sources"] --> jar["Regular JAR<br/>eap-fileview-3.0.3.jar"]
    src --> deps["Current runtime dependencies<br/>target/dependency/"]
    env["Ubuntu 24.04<br/>JRE 21 + LibreOffice + fonts"] --> app["Self-contained app image"]
    deps --> app
    jar --> app
    jar -.-> consumer["Optional embedded Boot app"]
    app --> compose["Docker Compose local build"]
```

Rebuild the stable base only when the JDK, LibreOffice, fonts or dependency set changes. Ordinary application code updates can reuse it.


Using the last **published** 3.0.2 image (Boot 4.1.1); 3.0.3 is not yet released:

```bash
cp .env.example .env
# Configure KK_BASE_URL and KK_TRUST_HOST inside .env
docker compose up -d --wait --wait-timeout 180
```

Before upgrading, record the current `EAP_FILEVIEW_IMAGE`. Set it to the target version tag or digest, run `docker compose pull`, then repeat the startup command. To roll back, restore the previous image reference and repeat both commands. Compose waits for the health check and keeps the preview cache in its named volume.

To build the unreleased version locally, first run Maven clean package. The Dockerfile is self-contained:

```bash
mvn -B -pl server -am -DskipTests clean package
docker compose -f compose.yaml -f compose.build.yaml up -d --build --wait --wait-timeout 180
```

The current application Dockerfile is self-contained: Java 21, LibreOffice, fonts, runtime dependencies and the ordinary JAR. The separately published base image is optional and not used by this Dockerfile. Compose defaults to `127.0.0.1:18012` and the `/fileview` context path. Verify actual [image publication status](https://github.com/eaopen/eap-fileview/actions) before attempting a pull.

## Downstream Java integration

```xml
<dependency>
  <groupId>io.github.eaopen</groupId>
  <artifactId>eap-fileview</artifactId>
  <version>3.0.2</version>
</dependency>
```

Import `EapFileViewComponents` in the consumer's own Spring Boot application and provide optional preview handler beans as needed. The Maven group, signing account and CNB/Central publication settings follow eap-boot. A passing CI build **does not guarantee publication to Maven Central**.

## Documentation

- [Solution and system architecture](doc/ARCHITECTURE.md)
- [Extension SPI, Docker images and Maven publishing](doc/EXTENSION_RELEASE.md)
- [Generic HTTP file preview integration](doc/HTTP_PREVIEW_INTEGRATION.md)
- [Generic preview capability review](doc/PREVIEW_CAPABILITY_REVIEW.md)
- [Branch policy and release promotion](doc/BRANCHING.md)
- [Spring Boot 4 migration assessment](doc/UPSTREAM_SPRING_BOOT4_ASSESSMENT.md)
- [Original kkFileView English README](doc/kkfileview/README.md)
- [Original kkFileView Chinese README, full format list and screenshots](doc/kkfileview/README.cn.md)
- [Original kkFileView AGENTS and historical CI/E2E docs](doc/kkfileview/)

The upstream authorship, license and original instructions are retained as references. Archived Boot 3 or Windows automation commands are **not current eap-fileview `dev` instructions**.
