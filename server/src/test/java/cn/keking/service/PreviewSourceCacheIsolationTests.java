package cn.keking.service;

import cn.keking.model.FileAttribute;
import cn.keking.service.cache.CacheService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class PreviewSourceCacheIsolationTests {
    @Test
    void sameFilenameDifferentSourceOrTicketMustNotShareCacheFiles() {
        FileHandlerService handler = new FileHandlerService((CacheService) null);
        ReflectionTestUtils.setField(handler, "uriEncoding", "UTF-8");
        FileAttribute first = handler.getFileAttribute(
                "https://files.example.net/files/token-A/plan.pdf", null);
        FileAttribute second = handler.getFileAttribute(
                "https://files.example.net/files/token-B/plan.pdf", null);
        FileAttribute office = handler.getFileAttribute(
                "https://files.example.net/files/token-C/plan.docx", null);
        FileAttribute otherOffice = handler.getFileAttribute(
                "https://files.example.net/files/token-D/plan.docx", null);
        assertEquals("plan.pdf", first.getName());
        assertEquals("plan.pdf", second.getName());
        assertNotEquals(first.getOriginFilePath(), second.getOriginFilePath());
        assertNotEquals(first.getOutFilePath(), second.getOutFilePath());
        assertNotEquals(first.getCacheListName(), second.getCacheListName());
        assertNotEquals(office.getOriginFilePath(), otherOffice.getOriginFilePath());
        assertNotEquals(office.getOutFilePath(), otherOffice.getOutFilePath());
        assertFalse(first.getOriginFilePath().contains("token-A"), "Never persist signed URLs in cache paths");
    }

    @Test
    void identicalSourceProducesStableConversionCacheIdentity() {
        FileHandlerService handler = new FileHandlerService((CacheService) null);
        ReflectionTestUtils.setField(handler, "uriEncoding", "UTF-8");
        String source = "https://files.example.net/files/a/report.docx";
        FileAttribute first = handler.getFileAttribute(source, null);
        FileAttribute second = handler.getFileAttribute(source, null);
        assertEquals(first.getOriginFilePath(), second.getOriginFilePath());
        assertEquals(first.getCacheName(), second.getCacheName());
        assertEquals("report.docx", first.getName());
    }
}
