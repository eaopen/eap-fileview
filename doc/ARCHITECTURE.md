# eap-fileview 解决方案与系统架构

![eap-fileview 解决方案与系统架构](img/eap-fileview-solution-architecture.svg)

图由 GPT 根据源码及构建配置整理，以可缩放 SVG 存放于 doc/img/，用于 README 和技术文档。项目不依赖具体业务或文件存储系统。

## 解决方案定位

eap-fileview 基于 kkFileView v5.0.3 独立演进，当前 dev 版本 3.0.2，技术基线为 JDK 21 + Spring Boot 4.1.1。支持独立 HTTP 服务部署与普通 Maven JAR 嵌入式复用，面向 Office/WPS、PDF/OFD、图片、压缩包、XMind、文本、CAD 和 3D 等文件预览。

## HTTP 预览架构

1. **接入层**：浏览器、Web 前端或独立调用方，可通过反向代理访问预览服务。
2. **预览接口**：OnlinePreviewController 提供在线预览入口；另有可选多图片预览及跨域文件读取辅助入口。
3. **来源安全与类型识别**：解码 URL 参数、验证可信文件来源、识别 FileAttribute 与预览格式。来源网络信任校验不能替代业务文件授权。
4. **处理器选择**：FilePreviewFactory 优先匹配可选的 FilePreviewProvider Bean，默认使用内置预览处理器。
5. **文件转换**：处理器通过下载和解析工具获取文件，Office 通过 JODConverter/LibreOffice 转换；当特定 Word 转 PDF 失败时，可调用 OfficePreviewFallbackConverter 提供可选回退。
6. **缓存与展示**：转换结果使用临时文件和缓存（JDK、RocksDB、可选 Redis）；通过 FreeMarker 模板、PDF.js 和其他浏览器查看器提供 HTML 及预览资源。

文件来源或调用方负责用户身份、业务权限、存储、临时签名 URL 和有效期；本项目只负责通用预览处理，不管理业务数据。

## Maven 与 Docker 分层

**普通 Maven JAR**：io.github.eaopen:eap-fileview:3.0.2，供 JDK 21 + Spring Boot 4.1 应用按需导入，扩展组件入口为 EapFileViewComponents。

**稳定 Docker 基础镜像**：JRE 21、LibreOffice、字体和构建时解析的 Maven 运行依赖。应用镜像在基础镜像上新增普通 JAR 和配置，通过 Docker Compose 部署。

只发布普通 JAR 与 Docker 镜像；不需要可执行 Fat JAR。target/dependency 是构建镜像使用的依赖目录，而不是额外发布产品。

## 分支与验证边界

dev：v3.0.2 / JDK 21 / Spring Boot 4.1.1，唯一日常开发线。boot3：v2.9.2 / JDK 21 / Spring Boot 3.5.6，只保留历史兼容快照。

105 项可移植测试已通过。LibreOffice/SevenZip 相关环境测试、真实文件预览样本和完整 Docker 启动仍应单独验证；架构图并不代表已完成全部运行验收。

[扩展和发布说明](EXTENSION_RELEASE.md) · [通用 HTTP 集成规范](HTTP_PREVIEW_INTEGRATION.md) · [分支管理](BRANCHING.md)。
