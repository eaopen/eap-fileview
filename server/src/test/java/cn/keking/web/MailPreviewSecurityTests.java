package cn.keking.web;
import cn.keking.model.FileType;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class MailPreviewSecurityTests {
    private static final String EML_BUNDLE =
            "static/eml/static/js/app.8292ba6c8f6b07911e29.js";

    @Test
    void emlLoadsSafeRendererBeforeTheLegacyBundle() throws IOException {
        String page = resource("static/eml/index.html");
        String helper = "src/safe-mail-body.js";
        String bundle = "static/js/app.8292ba6c8f6b07911e29.js";
        assertTrue(page.contains(helper));
        assertTrue(page.indexOf(helper) < page.indexOf(bundle));
    }

    @Test
    void emlBodyCannotUseTheOldUnsafeDomSink() throws IOException {
        String bundle = resource(EML_BUNDLE);
        assertTrue(bundle.contains("window.KkSafeMailBody.render(n, e)"));
        assertFalse(bundle.contains("document.getElementById(\"divPagenation\").innerHTML=e.text"));
        String safe = resource("static/eml/src/safe-mail-body.js");
        assertTrue(safe.contains("container.textContent = plain"));
        assertTrue(safe.contains("frame.setAttribute('sandbox', '')"));
        assertTrue(safe.contains("frame.srcdoc ="));
        assertTrue(safe.contains("Content-Security-Policy"));
        assertTrue(safe.contains("default-src 'none'"));
        assertTrue(safe.contains("script-src 'none'"));
        assertTrue(safe.contains("img-src data:"));
        assertTrue(safe.contains("new Set(["));
        assertTrue(safe.contains("'script', 'style'"));
        assertFalse(safe.contains("allow-same-origin"));
        assertFalse(safe.contains("allow-scripts"));
    }

    @Test
    void formatSelectionAndPreviewTemplatesAreAvailable() throws IOException {
        assertEquals(FileType.EML, FileType.typeFromFileName("回复邮件.eml"));
        assertEquals(FileType.MSG, FileType.typeFromFileName("Outlook 邮件.MSG"));
        assertTrue(resource("web/eml.ftl").contains("eml/index.html?file="));
        assertTrue(resource("web/msg.ftl").contains("msg/index.html?file="));
        assertTrue(resource("web/eml.ftl").contains("file.name?html"));
        assertTrue(resource("web/msg.ftl").contains("file.name?html"));
    }

    private static String resource(String path) throws IOException {
        try (var stream = new ClassPathResource(path).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
