# Branch and release policy — eap-fileview

## Only actively maintained development branch

**`dev` is the GitHub default and the only actively maintained development line.** It is the former `boot4` branch:

- Release `revision`: **3.0.2**
- Runtime: **JDK 21 / Spring Boot 4.1.1 / Spring Framework 7**
- Source baseline: kkFileView 5.0.3 with selected general-purpose fixes
- Downstream Java 21 / Spring Boot 4.1 applications can embed the regular JAR or use the HTTP preview interface.

Use focused `feature/*` or `fix/*` branches from `dev`; review and merge PRs into `dev`. Validate the regular Maven JAR, security/portable tests, real preview flows and Docker compatibility as appropriate.

## Stable release gate

`main` is not a second development line. It currently retains **3.0.1 / Boot 4.0.8**. Promote a tested `dev` commit to `main` via review after CI, Linux LibreOffice/SevenZip validation, Docker startup and representative CloudFile/Office/PDF/XMind previews. Then tag `v3.0.2` on the approved `main` commit.

## Frozen Boot 3.5 reference

`boot3` (formerly `dev`) retains **2.9.2 / JDK 21 / Spring Boot 3.5.6** as an **unmaintained historical snapshot**. Do not create separate CI, product tasks, releases or regular maintenance there. The former `boot4` name has been replaced with `dev`.

The upstream kkFileView Git remote stays fetch-only. Review and cherry-pick relevant format/security fixes **into `dev`**, without overwriting Boot 4.1 POM, down-stream integration boundaries or EAP release metadata.

## Publication, identity and outputs

Maven group `io.github.eaopen`, CNB and Central publishing accounts, GPG signing and authorization follow the organization's existing eap-boot setup, while the fileview `revision` is independently versioned.

Outputs are a **regular Maven JAR plus Docker image** only. Base image `java21-sb41-r1` carries stable runtime dependencies; application changes go into the thin final image. Never claim a release exists until the repository/registry confirms publication.
