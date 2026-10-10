# eap-fileview 扩展与发布

## 项目定位与版本

版本规则与 [eap-boot](https://github.com/eaopen/eap-boot) 保持一致：`io.github.eaopen` Maven 组织、CI-friendly `${revision}` 属性、`ossrh`/ `cnb` 发布 profile、相同的项目所有权及授权治理。当前 EAP 发布版 `2.9.2`，上游 kkFileView 版本单独维护，不放进正式 Maven 发布号。修订版通过 `-Drevision=2.9.3` 覆盖，每次发布必须为新版本，不覆盖已经发布的版本。

**仅对外输出两类产物**：
- 普通 Maven JAR：`io.github.eaopen:eap-fileview:2.9.2`，源码和 Javadoc 附件，供下游开发复用。
- Docker 镜像：`ghcr.io/eaopen/eap-fileview:2.9.2`，独立启动运行。

不再创建 Spring Boot Fat JAR、`-exec.jar`、压缩分发包。Maven `package` 额外把已解析的 **运行依赖 JAR** 复制到 `server/target/dependency/`，只供 Docker 中间镜像组装使用，不是另一个发行物。

## JDK 兼容性

**版本号与 eap-boot 同步，不代表运行 Java 版本一致。** 当前 eap-boot 的 Java 17 和本项目 kkFileView 5.0.3/Java 21 应区分。
不允许把 `2.9.2` 当成 JDK 17 兼容承诺。若仍有 Java 17 环境，先独立 Docker 服务化，通过 HTTP 预览接口调用，避免运行时冲突。


## 普通 JAR 扩展

下游应用通过标准 Maven 依赖引用通用预览组件，应用专属模型、账号、数据库和其他服务由使用方负责：

```xml
<dependency>
  <groupId>io.github.eaopen</groupId>
  <artifactId>eap-fileview</artifactId>
  <version>2.9.2</version>
</dependency>
```


## Docker 镜像职责、构建和使用

### 稳定中间镜像（低频）

`ghcr.io/eaopen/eap-fileview-base:java21-lo-r1`：
Ubuntu 24.04 + OpenJDK 21 + LibreOffice + 开源中文字体 + **Maven 已解析运行时依赖**。
该镜像只在 Java/LibreOffice/字体/依赖版本发生实际变化时重新发布；**不随应用修订版每次重建**。

```bash
mvn -B -pl server -am -DskipTests package
docker build -f docker/Dockerfile.base \
  -t ghcr.io/eaopen/eap-fileview-base:java21-lo-r1 .
```

基础镜像 GitHub Actions：`.github/workflows/base-image.yml`，手动触发，使用 GHCR GitHub Token 身份登录；变更中间镜像后需要手动提升 `java21-lo-rN` 稳定标签，不能复写旧稳定发行版。不要把运行依赖拷入源码 Git 仓库。

### 最终应用镜像（按业务版本）

`ghcr.io/eaopen/eap-fileview:2.9.2` 只在稳定中间镜像上复制普通 JAR 和默认配置，以 Java classpath 启动 `cn.keking.ServerMain`。

```bash
docker compose --env-file .env.example -f compose.yaml -f compose.build.yaml build
# 正常部署（镜像已经发布后）：
cp .env.example .env
# 修改来源主机白名单和公网访问地址
docker compose up -d
```

GHCR 最终镜像工作流：`.github/workflows/images.yml`，采用 `revision` 作为应用标签；若使用 Git tag，应为 `v2.9.2`。

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


## Development maintenance branches (2026-10-10)

This document describes the dev compatibility branch:
- dev continues as release revision 2.9.2 on JDK21 + Spring Boot 3.5.6, matching upstream kkFileView v5.0.3.
- boot4 is the separate development branch for release 3.0.2 on JDK21 + Spring Boot 4.1.1.
- main remains the stable release gate for 3.0.1 until the Boot4 upgrade qualifies for promotion.

Do not merge Boot4 POM/runtime dependencies into dev. Cherry-pick isolated generic fixes
only after verifying on the Spring Boot 3.5 test suite. Refer to doc/BRANCHING.md.
