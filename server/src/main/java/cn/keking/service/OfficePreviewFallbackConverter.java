package cn.keking.service;

import java.nio.file.Path;

/**
 * Optional, application-provided Office converter. EAP FileView knows only the
 * generic file contract; any commercial/industry-specific implementation belongs
 * in a separate downstream module (including its license and dependencies).
 */
public interface OfficePreviewFallbackConverter {
    /** A short non-sensitive name used in diagnostic logs. */
    String name();

    /** Called only after LibreOffice fails for a Word-to-PDF conversion. */
    boolean supports(String extension);

    /** Write a nonempty PDF to target (a temporary path); never mutate source. */
    void convert(Path source, Path target) throws Exception;
}
