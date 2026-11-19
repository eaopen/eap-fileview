# CAD 外部转换产物接入规范

本规范说明最小的、**不引入商业依赖**的 CAD 转换结果接入方式。代码位于 `io.github.eaopen.fileview.conversion`。

## 不变的默认行为

- **kkFileView 已有的 CAD 处理和在线 3D 查看器一律保留。** DWG/DXF/DWF、STEP/STL/GLB 等继续采用既有路径，不替换转换器、缓存、文件处理、配置和页面。
- 新增 `ConversionJobResult` 与 `ConversionPreviewUrlBuilder` 两个纯 Java 类型；不会自动注册 Web Controller、定时任务，也不会访问文件或网络。只有项目主动调用才生效。
- NX PRT、Solid Edge DFT/PAR/PSM/ASM、JT 等原生格式，只有经项目转换器测试成功才称为支持，不根据文件后缀猜测能力。

## 免费优先的扩展顺序

| 级别 | 转换实现 | 目标场景 | 约束 |
| --- | --- | --- | --- |
| 0 | **已有 kkFileView** | 现有 CAD 预览与 3D 直接查看 | 无功能替换 |
| 1 | **FreeCAD / Open CASCADE（开源）** | STEP、IGES、BREP、STL 等通用中间格式 | Worker 独立部署、运行时资源隔离，按具体格式验收 |
| 1 | **IfcOpenShell / IfcConvert（开源）** | IFC/BIM → GLB、OBJ、STEP、SVG 等 | 不应当声称支持 NX / DFT 原生文件 |
| 2 | **项目已有 NX/DFT/JT Worker** | 现有业务中已验证的原生转换 | 仍归项目方管理，保留现有权限、许可和消息队列 |
| 3 | **可选商业评估** | 免费方案不能满足的 NX、Solid Edge、JT 等格式 | 优先选择可试用产品，项目方提供正式/试用授权 |

商业候选可包括 CAD Exchanger SDK、HOOPS Exchange 等。必须先核验具体文件格式和版本、部署平台、输出质量、商业授权、许可限制及再分发条款。**通用公开 Maven JAR 和 Docker 镜像默认不捆绑商业 SDK、许可证或其专用二进制。**

免费或开源不是所有专有原生格式都能被解析的承诺。没有可行的开源路径时，业务可继续使用既有转换器，商业 SDK 只是第二选择而不是强制前置。

## 转换产物契约

`ConversionJobResult` 定义以下字段：`jobId`、`status`、`sourceFormat`、`sourceRevision`、`converterId`、`artifacts`。产物由 `role`（DRAWING/MODEL/THUMBNAIL）、`format`（PDF/GLB 等）和不透明 `reference` 组成。

- `reference` 必须由项目方理解与解析，不能当作浏览器可访问的本地文件路径或长期公开下载链接。
- `COMPLETED` 必须至少有一个产物；尚未完成的任务应由项目界面展示进度/错误，而不是创建空白预览页。
- 转换缓存应包含源文件的内容版本、依赖装配版本、转换器版本和输出参数；当前 DTO 只承载结果，不代替完整缓存策略。
- 对二维工程图优先输出 PDF，对三维模型优先输出自包含 GLB；需要保持精确 CAD/B-Rep 或 PMI 的场景应保留原文件及相关数据。

## 项目接入

在 Java 21 / Boot 4 应用中，由项目自己的服务查询转换结果，重新检查用户对源文件的权限，并为被选产物签发**短有效期、可撤销、只读**的文件 URL：

~~~java
ConversionJobResult job = conversionService.findById(jobId);
URI url = ConversionPreviewUrlBuilder.build(
    URI.create("https://preview.example/fileview/"),
    job,
    ConversionPreviewUrlBuilder.Selection.AUTO,
    artifactRef -> artifactGateway.issueReadUrl(user, artifactRef)
);
~~~

`AUTO` 选择顺序：DRAWING → MODEL → THUMBNAIL；也可明确指定 `MODEL` 等。生成的 URI 复用 kkFileView 现有的 `/onlinePreview?url=BASE64`，不新增公开文件下载接口。

**URL 安全限制：**
- 解析后的产物只能是 HTTP(S)，拒绝 `file://`、FTP、内嵌用户名/密码或 URL 片段。
- 因既有预览器按文件名后缀选择类型，项目网关须提供保留 `.pdf`、`.glb` 等真实格式后缀的授权地址，不能在签名 URL 上自行追加或修改参数。
- Base64 **不是加密**。最终预览链接中仍含可解码的临时票据；项目需控制缓存、访问日志、Referrer、来源主机白名单、反向代理日志和授权有效期。
- 现有 `KK_TRUST_HOST` / 网络层 SSRF 和 HTTP 重定向检查仍然有效，不应为了接入外部产物关闭。
- 产物由项目受控网关提供，需校验内容类型，禁止将不受信任的 HTML/脚本放到业务同源执行。优先 PDF、GLB，复杂 glTF 文件需处理相对资源。

Java 17 / Boot 3 项目不直接引入本次 Java 21 JAR，可由项目独立适配服务生成同一套现有 HTTP 预览链接。

## 验证与未完成事项

本 PR 仅验证无网络/文件 IO 的结果选择与 URL 组装，不提供真实 CAD Worker。下一阶段通过已有 DWG/DXF/GLB、开源 STEP/IFC 样本及经授权的 Etech NX PRT、Solid Edge DFT、JT 文件分别验证转换质量、模型完整性、版本依赖、权限撤销、异常、性能和产物缓存；未通过真实场景前不宣称原生格式支持。
