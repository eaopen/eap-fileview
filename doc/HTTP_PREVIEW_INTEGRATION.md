# 通用 HTTP 文件预览集成规范

eap-fileview 是独立文件预览服务。它不保存文件管理系统的用户、组织、权限、版本或业务数据，不提供文件编辑与回写接口，也不要求使用某一种文件存储产品。

## 基础调用契约

调用系统先完成用户身份和文件访问授权，然后提供可以被预览服务访问的只读 HTTP(S) 文件 URL。

- 默认入口：GET /onlinePreview?url=BASE64_ENCODED_FILE_URL
- 如果配置了上下文路径，示例为 /fileview/onlinePreview
- Office 预览模式可使用 officePreviewType=pdf 等受支持的参数
- 无文件名后缀的临时文件链接可以通过源 URL 的 fullfilename 参数向预览引擎提供文件名提示
- 文件 URL 可以是签名 URL、一次性票据或调用方提供的受控临时下载链接；凭据不应写入本项目代码和仓库

## 安全及运行边界

1. 文件来源系统负责授权、有效期、撤销、下载审计以及权限更改；预览引擎只执行格式识别、下载、转换、缓存和阅读页呈现
2. 通过 KK_TRUST_HOST 等配置限制可访问的源主机，保护内网地址和非受信跳转目标；使用 HTTP 重定向时应验证实际目标
3. 不把完整的临时签名 URL、票据或凭据记录在普通日志中；在线预览 Controller 对 decoded/encoded 来源 URL 的直接日志已经脱敏，其他转换/下载组件及代理日志仍应独立核查
4. 预览为只读体验，不等于浏览器无法保存内容；转换缓存与临时下载文件需要磁盘限制、自动清理和目录访问保护
5. 预览服务和文件来源服务需要彼此网络可达；容器中的 DNS、反向代理路径、重定向和 MIME 内容类型应做真实请求检查

## 独立部署

使用 JDK 21 + Spring Boot 4.1。普通 Maven JAR 提供嵌入式扩展，Docker 镜像提供独立 HTTP 服务。

~~~bash
cp .env.example .env
# Edit KK_BASE_URL and KK_TRUST_HOST according to your file source and reverse proxy.
docker compose up -d
~~~

仓库内置的镜像发布需先由正式流水线完成；本地构建参见 [扩展与发布文档](EXTENSION_RELEASE.md)。不要在此文档中放入使用方代码路径、数据库表结构、账号、登录 API 或生产网关配置。

## 可选 Office 扩展

默认由 LibreOffice 转换 Office 文件；下游 Spring 应用如注册 OfficePreviewFallbackConverter Bean，可在特定 Word 转 PDF 失败时尝试额外转换器。转换器输出需要是有效的 PDF，默认策略不改变受密码保护文件，也不覆盖 Excel/PPT 的转换处理。

可选 OOXML 字体诊断使用 KK_OFFICE_FONT_DIAGNOSTICS=true，检测 docx/docm 文件中显式声明的字体是否可能缺失，仅记录有限诊断信息、不改写文件。

商业转换器的依赖、许可证、业务适配、部署授权全部由集成方独立处理，不能成为通用 Maven JAR 的强制依赖。

## 当前验证边界

JDK 21 构建和 105 项可移植测试已通过。额外三个环境相关测试需要配置对应 LibreOffice、SevenZip JNI 环境。容器启动、真实授权 URL、Office/PDF/XMind/CAD 样本以及反向代理仍需逐项联调，不以单元测试代替。
