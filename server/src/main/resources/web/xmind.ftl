<!DOCTYPE HTML>
<html>
<head>
<title>${file.name}文件预览</title>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=no" />
<#include "*/commonHeader.ftl">
<script src="js/base64.min.js" type="text/javascript"></script>
<#if currentUrl?contains("http://") || currentUrl?contains("https://")>
    <#assign finalUrl="${currentUrl}">
    <#elseif currentUrl?contains("ftp://") >
   <#assign finalUrl="${currentUrl}">
<#else>
    <#assign finalUrl="${baseUrl}${currentUrl}">
</#if>
</head>
<body>
<#-- 通用改进：显示远程文件下载状态，避免一次性预览票据过期时出现空白画布。 -->
<div id="preview-status" role="status" style="padding:20px; text-align:center;">正在加载思维导图…</div>
<div id="mount">
</div>

<script src="xmind/xmind.js"></script>
<script type="text/javascript">
   	var url = '${finalUrl?js_string}';
	var kkagent = '${kkagent}';
    var baseUrl = '${baseUrl}'.endsWith('/') ? '${baseUrl}' : '${baseUrl}' + '/';
    if (kkagent === 'true' || !url.startsWith(baseUrl)) {
        url = baseUrl + 'getCorsFile?urlPath=' + encodeURIComponent(Base64.encode(url))+ "&key=${kkkey}";
    }
  const init = async () => {
    var windowWidth = (document.documentElement.clientWidth || document.body.clientWidth) - 30;
    var windowHeight = (document.documentElement.clientHeight || document.body.clientHeight) - 30;
    const res = await fetch(url);
    // 修改原因：部分临时 URL 会以 200 返回错误 JSON/HTML，必须在传给 XMind 解析器前识别。
    if (!res.ok) throw new Error('文件获取失败 (HTTP ' + res.status + ')');
    const bytes = await res.arrayBuffer();
    const header = new Uint8Array(bytes, 0, Math.min(bytes.byteLength, 4));
    if (header.length < 4 || header[0] !== 0x50 || header[1] !== 0x4b
            || header[2] !== 0x03 || header[3] !== 0x04) {
        throw new Error('获取的内容不是有效的 XMind ZIP 文件');
    }
    new XMindEmbedViewer({
      el: '#mount',
      file: bytes,
      styles: { height: windowHeight + 'px', width: windowWidth + 'px' }
    });
    document.getElementById('preview-status').hidden = true;
  };
  init().catch(function (error) {
    // 修改原因：失败只显示无敏感信息的提示，不在浏览器界面泄露临时取流 URL。
    console.warn('XMind preview failed:', error.message);
    document.getElementById('preview-status').textContent =
       '思维导图加载失败：文件地址可能已过期或内容无效，请重新打开预览。';
  });
   		 /*初始化水印*/
 if (!!window.ActiveXObject || "ActiveXObject" in window)
{
}else{
 initWaterMark();
}
</script>
</body>
</html>