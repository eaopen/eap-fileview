# 项目扩展接入指南

`eap-fileview` 提供通用文件预览，不负责用户身份、业务权限、存储或消息系统。默认优先使用独立 HTTP(S) 预览服务；需要 Java 插件时再选择普通 Maven JAR 嵌入。

## 扩展方式和边界

| 场景 | 推荐方式 | 项目保留的职责 |
| --- | --- | --- |
| HTTP(S) 可读取的普通文件 | Docker + `/onlinePreview` | 文件权限、签名 URL 和有效期、访问审计 |
| 项目文本后缀 | `KK_SIMTEXT` | 判断哪些格式确实是文本 |
| 项目专有查看/转换器 | `FilePreviewProvider` Spring Bean | 转换器和其运行依赖、许可 |
| Word 转 PDF 失败回退 | `OfficePreviewFallbackConverter` | 仅对失败样本启用，不应默默删除文档信息 |
| CAD/NX/PLM 专有文件或异步队列 | 独立项目适配服务 | DB、MQ、回调、转换任务和授权 |

## 通过配置支持自定义文本后缀

例如，对确认是文本的项目工程文件，可在部署的环境中设置：

`KK_SIMTEXT=txt,log,properties,dat,erg,input,out`

该配置**替换**默认文本后缀集合，不是追加；应保留项目需要的通用后缀。支持大小写不敏感的后缀、前导点和配置项空格。内置的 PDF/Office/CAD 等处理器优先，不会被文本后缀名单覆盖。

新版类型识别每次使用当前配置，避免 Java 枚举首次初始化时固化 `KK_SIMTEXT` 的问题。配置变更是否自动装载仍由调用项目的配置刷新机制决定。

**注意：**后缀识别不代表二进制格式可以按文本安全读取。调用方应约束可访问来源、大小、有效期和权限；不应将敏感签名 URL 放到日志中。

## Spring Boot Java 扩展

使用方需满足 Java 21 / Spring Boot 4.1 基线，通过 `@Import(EapFileViewComponents.class)` 导入通用组件，并提供独立的 `FilePreviewProvider` Bean。扩展通过 `supports(FileAttribute)` 选择要处理的文件，并依 `Ordered` 优先级决定具体处理器；其余文件仍使用通用内置预览器。

独立 Docker 服务不会自动发现项目代码中的 Spring Bean。需要 Java 扩展时，应由项目应用装配其模块，或将转换器作为独立服务，通过 HTTP/签名资源对接，不应把项目 SDK、私有数据库或商业转换器直接加入公开基础镜像。

对于仍运行 Java 17 / Spring Boot 3 的项目，优先选择 HTTP 接口而不是直接嵌入 Java 21 / Boot 4.1 JAR。

## 迁移和验收

1. 保留原业务 URL 和身份/文件授权逻辑，将通用类型逐步切换到通用 HTTP 预览；专有格式先走既有适配层。
2. 对照真实文件验证原版与新预览输出，包括 Office/PDF/XMind/大 XLSX/工程文本；不能仅以 HTTP 200 认定成功。
3. 用过期/撤销的票据、带签名查询参数、跨域、HTTP 跳转、缓存命中和失败转换验证边界；不要放宽来源白名单。
4. 大型 XLSX、特殊 PDF、含修订的 Office 等可能改变文档语义的处理，应通过显式降级或可选回退，不默认丢弃数据。
5. 只在多个项目需要且有实际成功样本时抽象新 SPI，避免为了“通用”把单个项目流程复制到核心引擎。

通用引擎仍以 kkFileView 为参考上游，但安全/格式修复应独立评估并选择性合入，不应覆盖 Java 21 / Boot 4.1 的构建基线。
