package cn.keking;

import cn.keking.model.FileAttribute;
import freemarker.template.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XmindTemplateTests {
    @Test
    void rendersStatusAndSanitizesSourceUrlForJavaScript() throws Exception {
        Configuration templates = new Configuration(Configuration.VERSION_2_3_32);
        templates.setClassForTemplateLoading(getClass(), "/web");
        templates.setDefaultEncoding("UTF-8");
        templates.setClassicCompatible(true);
        FileAttribute file = new FileAttribute();
        file.setName("demo.xmind");
        Map<String, Object> model = new ExtendedModelMap();
        model.put("file", file);
        model.put("baseUrl", "https://preview.example/fileview/");
        model.put("currentUrl", "https://files.example/xmind?token=a'b");
        model.put("kkagent", "false");
        model.put("kkkey", "");
        StringWriter output = new StringWriter();
        templates.getTemplate("xmind.ftl").process(model, output);
        String html = output.toString();
        assertTrue(html.contains("role=\"status\""));
        assertTrue(html.contains("if (!res.ok)"));
        assertTrue(html.contains("header[0] !== 0x50"));
        assertTrue(html.contains("token=a\\'b"), "JavaScript string must be escaped");
        assertTrue(html.contains("getCorsFile?urlPath="), "Keep authenticated proxy path");
    }

    @Test
    void xmindBundleHasDropdownDependencies() throws Exception {
        var url = getClass().getResource("/static/xmind/index.html");
        assertNotNull(url);
        String html;
        try (var stream = url.openStream()) {
            html = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        String popper = "js/popper-135fa9e662.min.js";
        String bootstrap = "js/bootstrap-26779614c4.min.js";
        assertTrue(html.indexOf(popper) < html.indexOf(bootstrap));
        assertTrue(html.contains(popper) && html.contains(bootstrap));
        assertNotNull(getClass().getResource("/static/xmind/" + popper));
        assertNotNull(getClass().getResource("/static/xmind/" + bootstrap));
    }
}
