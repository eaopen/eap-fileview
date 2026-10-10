# kkFileView 官方未升级 Spring Boot 4 的核查结论（2026-10-10）

- 官方 v5.0.3 POM 的技术基线是 JDK 21 + Spring Boot 3.5.6；2025-10-11 合并的 PR #687 明确目标为 JDK 8/Boot 2.4.2 → JDK 21/Boot 3.5.6。
- 当前没有核实到维护者关于不升级 Boot 4 的官方解释。因此“架构上不支持 Spring Boot 4”等结论没有依据。
- 上游 5.0.x 主要持续修复预览安全、文件格式兼容、PDF/Office、跨平台等问题。保留稳定 Spring Boot 3.5 可以避免把大版本框架迁移风险与业务回归叠加，属于合理推测而非维护者原话。

## 迁移验证结果（本项目）

eap-fileview v3.0.2 选择 Java 21 + Spring Boot 4.1.1 / Spring Framework 7，保留官方 kkFileView 5.0.3 格式能力。真实遇到的兼容变更：

- spring-boot-starter-web → starter-webmvc；另外引入 starter-restclient，Jackson 2 代码迁移 Jackson 3。
- ServerProperties 包路径迁移到 spring-boot-web-server 自动配置模块。
- HttpComponentsClientHttpRequestFactory 的 setConnectTimeout 已在 Framework 7 移除，连接超时交给既有 Apache HttpClient connection manager。
- TestRestTemplate 需要显式依赖 spring-boot-resttestclient，并调整包路径、@AutoConfigureTestRestTemplate 注解。
- Boot 4 健康端点默认显示 liveness/readiness 组；为了保留仅聚合健康状态，显式关闭 probes。
- 需 clean 之后真正重新编译，避免旧 Boot 3 测试 class 文件暂时掩盖 ABI 不兼容。

核验：干净构建成功、105 个便携测试通过、普通 JAR 和 sources/javadoc 打包成功，独立 Maven 消费项目用 v3.0.2 可以编译。
尚未验证：缺少本机 LibreOffice 和 SevenZip JNI 的三项平台相关测试，以及真实文件 URL 端到端预览与容器内部启动。

## 后续同步策略

- 与上游保持代码级追踪：官方版本是 kkFileView 5.0.3；EAP 发版是 3.0.2，两者不混用。
- 以接口/测试作为回归边界，不将上游 Spring Boot 3.5 POM 覆盖已经迁到 Boot 4 的 EAP POM。
- 下游应用可使用兼容的 JDK 21 / Spring Boot 4.1 依赖或独立 HTTP 服务，不在本项目跟踪使用方的开发及部署计划。

参考：
- https://github.com/kekingcn/kkFileView/blob/v5.0.3/pom.xml
- https://github.com/kekingcn/kkFileView/pull/687
- https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
- https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes
