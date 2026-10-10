package cn.keking.web.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.service.FileHandlerService;
import cn.keking.service.FilePreview;
import cn.keking.service.FilePreviewFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class PreviewCredentialLoggingTests {
    @Test
    void previewLogsTypeWithoutLoggingEncodedOrDecodedSignedUrl() {
        String secret = "canary-secret-preview-ticket-123456";
        String source = "https://files.example.net/download/document.pdf?ticket=" + secret;
        String encoded = Base64.getEncoder().encodeToString(source.getBytes(StandardCharsets.UTF_8));

        Logger logger = (Logger) LoggerFactory.getLogger(OnlinePreviewController.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> captured = new ListAppender<>();
        captured.start();
        logger.addAppender(captured);

        String previousKey = ConfigConstants.getKey();
        try {
            logger.setLevel(Level.INFO);
            ConfigConstants.setKeyValue("false");
            FileHandlerService handler = new FileHandlerService(null) {
                @Override public FileAttribute getFileAttribute(String url, HttpServletRequest request) {
                    FileAttribute file = new FileAttribute();
                    file.setName("document.pdf");
                    file.setSuffix("pdf");
                    return file;
                }
            };
            FilePreviewFactory factory = new FilePreviewFactory(null) {
                @Override public FilePreview get(FileAttribute file) {
                    return (url, model, attribute) -> "pdf";
                }
            };
            OnlinePreviewController preview = new OnlinePreviewController(factory, handler, null, null);
            assertEquals("pdf", preview.onlinePreview(encoded, null, null,
                    "false", "0", "false", new ExtendedModelMap(), new MockHttpServletRequest()));
            String messages = captured.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .reduce("", (a, b) -> a + "\n" + b);
            assertTrue(messages.contains("预览文件类型"), "File type should remain observable");
            assertFalse(messages.contains(secret), "Never log the signed source credential");
            assertFalse(messages.contains(source), "Never log the decoded source URL");
            assertFalse(messages.contains(encoded), "Never log the encoded source URL");
        } finally {
            ConfigConstants.setKeyValue(previousKey);
            logger.setLevel(previousLevel);
            logger.detachAppender(captured);
            captured.stop();
        }
    }
}
