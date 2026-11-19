# CAD / 3D 预览扩展方案

## 现有能力和边界

通用引擎保持 kkFileView 的 DWG/DXF/DWF 等 2D CAD 转换和 STL/OBJ/GLB/glTF/STEP/IGES/IFC 等 3D 查看器路由。后缀识别不代表原生 CAD 文件可以正确解析，预览必须用真实文件验收。STL、IGES、IFC 在历史数组中有重叠，当前的 3D 查看器路由保持不变，避免影响已有集成。

NX PRT、Solid Edge DFT/PAR/PSM/ASM、JT 等格式暂不伪装为内置已支持类型。项目可以通过 FilePreviewProvider 指定原生格式处理器，并在外部转换器完成后提供 PDF / SVG / GLB 等受控预览产物。原生扩展名、CAD 版本和真实文件类型需要上下文判断，不能单靠后缀。

## 首阶段修改

- 3D 文件准备失败时应显示原有错误页，不展示空白 3D 查看器。
- 用可移植测试固定 2D CAD / 通用 3D / 专有 CAD 后缀路由及项目 Provider 覆盖行为。
- 不自动改变现有 CAD/3D 路由，不自动调用商业 SDK。

## 外部转换器接入原则

- 保持统一的产物契约：jobId、status、sourceFormat、converter、artifacts[{role, format, ref}]；结果的 ref 是可授权解析的内部标识，不是磁盘路径或长期公开 URL。
- 文件及产物读写权限由调用系统控制，超时、异常、任务取消、版本化缓存和失败结果应有稳定语义。
- Java 21 / Boot 4 项目可用可选的转换适配 Bean；Java 17 / Boot 3 项目使用独立 HTTP 服务。
- 不能把项目私有数据库、RabbitMQ 配置或商业许可证纳入公开通用库。商业 SDK、外部转换服务可由项目方提供有效许可证后试用，但必须先验证格式版本、输出完整度、运行平台、并发及再分发许可。
- PDF、SVG、GLB 可使用现有查看器；由外部服务生成的 HTML 不可直接以不受信任脚本嵌入业务同源页面。

## 工程文件验收

使用经授权的脱敏真实样本验证 NX PRT（不同版本、含关联装配）、Solid Edge DFT、JT；同时覆盖普通 PDF、DWG/DXF、GLB/STEP，检查浏览器效果、错误回退、权限票据过期、缓存和撤销。当前仅有通用格式测试，尚不代表上述专有样本已通过。
