# Spring Boot 4.1.2 upgrade review

## Scope and release boundary

- Upgrade the existing Java 21 / Spring Boot 4.1 line from 4.1.1 to 4.1.2 through the existing spring-boot-dependencies BOM, leaving WebMVC, RestClient, FreeMarker, Actuator and test starters under BOM management.
- Change the **unreleased dev Maven revision to 3.0.3**. Do not republish 3.0.2, which already has a Git tag/release and a published Docker image.
- Preserve the published Compose image default `ghcr.io/eaopen/eap-fileview:3.0.2` (Boot 4.1.1); local source builds use a separate `eap-fileview:dev` image to avoid mislabeling 3.0.3 as 3.0.2.
- The current final Dockerfile is **self-contained** and copies the current Maven runtime JARs. The separately published base image is optional. If rebuilt for the new dependency set, use `java21-sb41-r2` and do not overwrite `java21-sb41-r1`.

## Runtime dependency tree comparison

Compared using Maven dependency:tree with Boot 4.1.1 vs 4.1.2 on the same server POM (runtime scope). Versions listed here are actually resolved for this project, not every item in the upstream release notes.

| Dependency | Before | After |
| --- | --- | --- |
| Spring Boot modules/starters | 4.1.1 | 4.1.2 |
| Spring Framework MVC/Context/AOP | 7.0.9 | 7.0.10 |
| Jackson 3 Core / Databind | 3.1.5 | 3.1.7 |
| Embedded Tomcat | 11.0.24 | 11.0.27 |
| FreeMarker | 2.3.34 | 2.3.35 |
| Micrometer Core / Observation | 1.17.1 | 1.17.2 |
| Apache HttpClient5 | 5.6.4 | 5.6.5 |
| Apache HttpCore5 | 5.4.3 | 5.4.4 |
| Netty modules | 4.2.17.Final | 4.2.19.Final |
| Reactor Core | 3.8.7 | 3.8.8 |
| SLF4J API | 2.0.18 | 2.0.20 |
| Byte Buddy | 1.18.11 | 1.18.15 |

Explicitly pinned file-preview dependencies such as JODConverter 4.4.11, PDFBox 3.0.6, Redisson 4.0.0, Aspose.CAD 25.10 and POI 5.2.5 are **not upgraded in this PR**. They require separate real-file conversion and runtime compatibility checks rather than a speculative bulk bump. Both Jackson 2 annotations and Jackson 3 libraries may appear transitively; do not force a single Jackson major version without checking upstream compatibility.

## Local checks on November 19, 2026

- Portable Maven tests with Boot 4.1.2: **127 passed, 0 failures or errors** on Java 21.
- Maven clean package: **BUILD SUCCESS**, ordinary eap-fileview-3.0.3.jar produced.
- The packaged runtime dependency folder contains 166 jars, with Boot 4.1.2 / Spring MVC 7.0.10 / Jackson 3.1.7 / Tomcat 11.0.27 and no mixed old Spring Boot jars.
- Compose configuration separately verified: local source build is tagged eap-fileview:dev using APP_VERSION=3.0.3; published runtime deployment remains ghcr.io/eaopen/eap-fileview:3.0.2.
- Locally built the self-contained **linux/arm64** Docker image and verified HTTP Actuator returned 200 with UP.
- Verified LibreOffice 24.2.7.2 CLI conversion of HTML and DOCX samples to valid PDF.
- Verified the real viewer flow via an isolated Docker network: a sample DOCX served over HTTP was requested using /onlinePreview, returned HTTP 200, and the viewer produced a PDF with an OfficeToPdfService success log.
- The JODConverter-managed LibreOffice processes initially exited with code 81, then restarted and connected to both ports; conversion completed. A separate config watcher logged a missing server/src/main/config runtime path. These runtime warnings should be triaged independently; neither prevented the tested preview conversion.
- Linux amd64 startup, platform-specific LibreOffice/SevenZip tests and representative real Etech/CloudFile files still remain full-release gates.

## Validation and publishing gates

1. Run portable Java 21 unit/security tests and Maven clean package. Use **clean** after dependency upgrades so `server/target/dependency` contains no stale 4.1.1 runtime jars.
2. Inspect dependency:tree and packaged `server/target/dependency` for a single Spring Boot 4.1.2 / Framework 7.0.10 dependency set.
3. Validate Linux Docker startup, Actuator health, LibreOffice/SevenZip environment-specific tests and representative Office, PDF, XMind, CAD/3D previews.
4. Test Etech/CloudFile caller integration, signed URLs, cache, Redis optional mode and actual HTTP preview routing before release.
5. Only after gates pass, create v3.0.3 release/tag and publish new Maven / Docker artifacts. The existing v3.0.2 remains untouched.

Official reference: https://spring.io/blog/2026/10/23/spring-boot-4-1-2-available-now/
Managed dependencies: https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html
