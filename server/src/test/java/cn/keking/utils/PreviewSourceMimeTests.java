package cn.keking.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PreviewSourceMimeTests {

    @Test
    void refusesLoginPagesOrTicketJsonForBinaryFormats() {
        for (String suffix : new String[]{"pdf", "docx", "xlsx", "xmind",
                "zip", "7z", "dwg", "glb", "tiff", "ofd"}) {
            assertTrue(WebUtils.isMimeCheckRequired(suffix), suffix);
            assertFalse(WebUtils.isValidMimeType("application/json; charset=utf-8", suffix), suffix);
            assertFalse(WebUtils.isValidMimeType("text/html", suffix), suffix);
            assertTrue(WebUtils.isValidMimeType("application/octet-stream", suffix), suffix);
        }
    }

    @Test
    void acceptsAsciiDxfButNotHtmlLoginPage() {
        assertTrue(WebUtils.isValidMimeType("text/plain; charset=utf-8", "dxf"));
        assertFalse(WebUtils.isValidMimeType("text/html", "dxf"));
        assertFalse(WebUtils.isValidMimeType("application/json", "dxf"));
    }

    @Test
    void preservesTextAndCodePreviewSources() {
        for (String suffix : new String[]{"txt", "md", "csv", "json", "html", "js", "xml",
                "gltf", "stl", "stp", "step", "igs", "iges", "ifc"}) {
            assertFalse(WebUtils.isMimeCheckRequired(suffix), suffix);
        }
    }
}
