# eap-fileview 扩展与发布

## 项目定位与版本

本项目是基于 kkFileView 5.0.3 的独立通用文件预览引擎，不依赖特定业务系统。
版本管理机制、发布身份和账号参考 [eap-boot](https://github.com/eaopen/eap-boot)：`io.github.eaopen` Maven 组织、CI-friendly `${revision}` 属性、`ossrh`/ `cnb` 发布 profile、相同的项目所有权及授权治理。当前 dev 开发版本 `3.0.2`（main 稳定版暂为 3.0.1），上游 kkFileView 版本单独维护，不放进正式 Maven 发布号。新修订版可通过 `-Drevision=3.0.3` 覆盖，每次发布必须为新版本，不覆盖已经发布的版本。

**仅对外输出两类产物**：
- 普通 Maven JAR：`io.github.eaopen:eap-fileview:3.0.2`，源码和 Javadoc 附件，供下游开发复用。
- Docker 镜像：`ghcr.io/eaopen/eap-fileview:3.0.2`，独立启动运行。

不再创建 Spring Boot Fat JAR、`-exec.jar`、压缩分发包。Maven `package` 额外把已解析的 **运行依赖 JAR** 复制到 `server/target/dependency/`，只供 Docker 中间镜像组装使用，不是另一个发行物。

## JDK 兼容性

**账号及发布治理与 eap-boot 共用，版本独立维护。** 本项目 JDK 21 + Spring Boot 4.1.1 + Spring Framework 7。
普通 Maven JAR 面向 JDK 21 / Spring Boot 4.1 的使用方；其他环境可通过 Docker HTTP 服务接入。
不允许把 `3.0.2` 当成 JDK 17 兼容承诺。若仍有 Java 17 环境，先独立 Docker 服务化，通过 HTTP 预览接口调用，避免运行时冲突。

## 普通 JAR 二次开发

下游使用 Maven 依赖，保留自己的业务模型、登录、授权、数据库、MQ 和独立启动类：

```xml
<dependency>
  <groupId>io.github.eaopen</groupId>
  <artifactId>eap-fileview</artifactId>
  <version>3.0.2</version>
</dependency>
```

Spring Boot 应用通过 `@Import(EapFileViewComponents.class)` 导入通用组件，可选注册 `FilePreviewProvider` 或 `OfficePreviewFallbackConverter`。未提供扩展时使用默认预览实现；业务特定代码不进入本项目。

## Docker 镜像职责、构建和使用

### 稳定中间镜像（低频）

`ghcr.io/eaopen/eap-fileview-base:java21-sb41-r1`：
Ubuntu 24.04 + OpenJDK 21 + LibreOffice + 开源中文字体 + **Maven 已解析运行时依赖**。
该镜像只在 Java/LibreOffice/字体/依赖版本发生实际变化时重新发布；**不随应用修订版每次重建**。

```bash
mvn -B -pl server -am -DskipTests package
docker build -f docker/Dockerfile.base \
  -t ghcr.io/eaopen/eap-fileview-base:java21-sb41-r1 .
```

基础镜像 GitHub Actions：`.github/workflows/base-image.yml`，手动触发，使用 GHCR GitHub Token 身份登录；变更中间镜像后需要手动提升 `java21-lo-rN` 稳定标签，不能复写旧稳定发行版。不要把运行依赖拷入源码 Git 仓库。

### 最终应用镜像（按业务版本）

`ghcr.io/eaopen/eap-fileview:3.0.2` 只在稳定中间镜像上复制普通 JAR 和默认配置，以 Java classpath 启动 `cn.keking.ServerMain`。

```bash
docker compose --env-file .env.example -f compose.yaml -f compose.build.yaml build
# 正常部署（镜像已经发布后）：
cp .env.example .env
# 修改来源主机白名单和公网访问地址
docker compose up -d
```

GHCR 最终镜像工作流：`.github/workflows/images.yml`，采用 `revision` 作为应用标签；若使用 Git tag，应为 `v3.0.2`。

## Maven 仓库与发布身份

`pom.xml` 与 eap-boot 对齐：
- `-Pcnb` 目标：`https://maven.cnb.cool/openeap/mvn/-/packages/`，Maven settings 的 serverId `openeap-mvn-cnb`，采用 EAP 现有 CNB 账号/Token。
- `-Possrh` 目标：Maven Central Publishing Portal，serverId `sonatype-central`，GPG 签名，sources、Javadoc 与 parent POM。使用 EAP 现有签名密钥体系，不提交口令。
- 工作流 `maven-cnb.yml` 和 `maven-central.yml` 分别发布到 CNB 和中央库；所有凭据只在 GitHub Actions Environment 或 Secret、私有 Maven settings 保存。应用中不得硬编码 EAP 账号、GPG 密钥或令牌。
- 中央库是公开且不可覆盖的发布渠道，需确保发布权限、坐标唯一、许可证和传递依赖兼容性后才触发。

## 历史 systemPath JAR 与 CNB 仓库

上游曾将 `server/lib/jai_core-1.1.3.jar` 和 `jai_codec-1.1.3.jar` 作为 `systemPath`。当前源码未直接 import Sun JAI 包，已有 `jai-imageio-core`、`jai-imageio-jpeg2000` 等现代实现，**主工程不再需要这两个依赖**，因此彻底移除 `systemPath` 与公开源码库中捆绑的二进制，不引入新的无法解析的依赖。

核验 2026-10-10：CNB 中 `javax.media:jai_core:1.1.3` 的 POM 可访问，但 JAR 不可访问；`javax.media:jai_codec:1.1.3` 的 POM/JAR 均不可访问。不能因为 POM 为 200 就认为制品完整。JAI 属 Sun JDL 许可，二进制再分发带有条件要求，**不能直接把本地旧 JAR 上传到公开 CNB 仓库**。

如果后续真实回归发现存在缺失 JAI 的特殊转换格式，建议先确认 Apache JAI ImageIO 替代是否充分；确实必要时在独立的受控 CNB 仓库按组织授权规则发布、保存文件 SHA256 和来源/许可证信息，发布完成后用标准 Maven 坐标和独立下游工程验证，不再使用 systemPath，不将 Sun JAR 直接纳入 Maven Central。

## 当前验证边界

Java 21 本机可运行的单元/安全测试与 Maven 普通 JAR 构建已验证；需要 Linux 容器中验证 LibreOffice 和 SevenZip JNI 的 3 个平台环境用例。真实 CloudFile 文件票据、Office、XMind、PDF/CAD 预览与最终容器启动仍需 E2E。


## 持续构建普通 JAR

GitHub workflow `jar-ci.yml` 在 main 推送及手动触发时运行 Java 21 的 105 项可移植测试，
只上传普通 eap-fileview JAR 到 GitHub Actions artifacts，便于 Maven Central/CNB 尚未发布时快速验证。
这不是 Maven Central 发布，正式外部依赖仍通过有凭据的仓库发布流程管理。

Aspose.CAD 25.10 属于独立的第三方商业依赖。发布包含该 JAR 的公开 Docker 运行依赖镜像前，
应核实组织已获得适用的使用和分发许可；如无分发权，应将它作为下游自行装配的可选 CAD 扩展。


## Spring Boot 4 upgrade validation (2026-10-10)

Official kkFileView v5.0.3 is based on Boot 3.5.6. eap-fileview upgrades
independently to JDK 21 / Boot 4.1.1 / Framework 7 / Jackson 3.
Changes include WebMVC and RestClient starters, Jackson 3 serialization APIs,
ServerProperties relocation, removed HttpClient connect-timeout factory API,
TestRestTemplate test module and Boot 4 liveness/readiness default settings.
105 portable tests pass. Linux-specific LibreOffice/SevenZip integration tests remain to run.

Runtime dependency JARs are baked into the stable base image, so Boot 3 to Boot 4
requires a NEW base image tag java21-sb41-r1. Never reuse the Boot 3 dependency base
tag java21-lo-r1 for Boot 4 jars.


## Branch and release workflow (2026-10-10)

The GitHub default branch is dev, formerly named boot4. This is the only
actively maintained development branch, currently 3.0.2 / JDK21 / Boot 4.1.1.

- Feature work / regular security fixes: feature/fix branch from dev -> tested PR into dev.
- main: stable publication gate, still 3.0.1 / Boot 4.0.8 until dev passes
  Linux LibreOffice/SevenZip, Docker startup and representative real preview tests.
  Promote through an approved PR and create v3.0.2 tag on main.
- boot3: frozen historical snapshot (2.9.2 / JDK21 / Boot 3.5.6).
  No separate maintenance, CI, releases or new work there.
- Upstream kkFileView remains fetch-only, source patches may be selectively
  ported to dev without replacing the Boot 4.1 Maven dependency definitions.

Output remains regular Maven JAR + Docker image, without executable JAR.
The stable runtime dependencies are pinned to java21-sb41-r1 (not the old Boot 4.0
base java21-sb4-r1). Publishing identities and authorizations use the same
eap-boot organization settings. See [BRANCHING.md](BRANCHING.md).
