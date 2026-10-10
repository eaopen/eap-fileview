package cn.keking.service;

import cn.keking.model.FileAttribute;
import org.jodconverter.core.office.OfficeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Generic preview conversion orchestration. Defaults to upstream LibreOffice;
 * optional plugins can rescue Word documents that fail the default converter.
 * All output remains preview-only; never changes the original document.
 */
public final class OfficePreviewConversion {
    private static final Logger logger = LoggerFactory.getLogger(OfficePreviewConversion.class);
    private static final Set<String> FALLBACK_SUFFIXES = Set.of("doc", "docx", "docm");

    private final OfficeToPdfService primary;
    private final List<OfficePreviewFallbackConverter> fallbacks;

    public OfficePreviewConversion(OfficeToPdfService primary, List<OfficePreviewFallbackConverter> fallbacks) {
        this.primary = primary;
        this.fallbacks = List.copyOf(fallbacks == null ? List.of() : fallbacks);
    }

    public void convert(String source, String output, FileAttribute file) throws OfficeException {
        try {
            primary.openOfficeToPDF(source, output, file);
            return;
        } catch (OfficeException primaryFailure) {
            String suffix = file.getSuffix() == null ? "" : file.getSuffix().toLowerCase(Locale.ROOT);
            // Modification reason: fallback PDF conversion must not be tried for spreadsheets,
            // protected documents with bad passwords, or Office->HTML output.
            if (!FALLBACK_SUFFIXES.contains(suffix) || !output.toLowerCase(Locale.ROOT).endsWith(".pdf")
                    || (file.getFilePassword() != null && !file.getFilePassword().isBlank())) {
                throw primaryFailure;
            }
            for (OfficePreviewFallbackConverter fallback : fallbacks) {
                if (!fallback.supports(suffix)) continue;
                Path candidate = null;
                try {
                    Path target = Path.of(output).toAbsolutePath();
                    Files.createDirectories(target.getParent());
                    // Use a private scratch file and publish only after full conversion.
                    candidate = Files.createTempFile(target.getParent(), ".preview-fallback-", ".pdf");
                    fallback.convert(Path.of(source), candidate);
                    // 修改原因：有些转换服务失败时会写入 HTML/JSON 错误体。
                    // 产物必须至少具有 PDF 文件头才能进入共享预览缓存。
                    if (!Files.isRegularFile(candidate) || Files.size(candidate) < 5) {
                        throw new java.io.IOException("Fallback did not produce a PDF");
                    }
                    try (var input = Files.newInputStream(candidate)) {
                        if (!Arrays.equals(input.readNBytes(5), "%PDF-".getBytes(StandardCharsets.US_ASCII))) {
                            throw new java.io.IOException("Fallback produced a non-PDF response");
                        }
                    }
                    Files.move(candidate, target, StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Office preview recovered via fallback converter: {}", fallback.name());
                    return;
                } catch (Exception fallbackFailure) {
                    logger.warn("Office fallback converter failed: {}", fallback.name());
                    primaryFailure.addSuppressed(fallbackFailure);
                } finally {
                    if (candidate != null) {
                        try { Files.deleteIfExists(candidate); }
                        catch (java.io.IOException e) { logger.debug("Unable to remove temporary conversion output", e); }
                    }
                }
            }
            throw primaryFailure;
        }
    }
}
