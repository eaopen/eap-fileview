package cn.keking.service;

import cn.keking.model.FileAttribute;
import org.jodconverter.core.office.OfficeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OfficePreviewConversionTests {
    @TempDir Path tempDir;

    private FileAttribute file(String suffix) {
        FileAttribute file = new FileAttribute();
        file.setSuffix(suffix);
        return file;
    }

    @Test
    void convertsWithOptionalFallbackAfterPrimaryFailure() throws Exception {
        OfficeToPdfService primary = mock(OfficeToPdfService.class);
        FileAttribute file = file("docx");
        Path result = tempDir.resolve("preview.pdf");
        doThrow(new OfficeException("LO failed")).when(primary)
                .openOfficeToPDF(anyString(), eq(result.toString()), eq(file));
        AtomicInteger invoked = new AtomicInteger();
        OfficePreviewFallbackConverter fallback = new OfficePreviewFallbackConverter() {
            public String name() { return "dummy"; }
            public boolean supports(String ext) { return "docx".equals(ext); }
            public void convert(Path source, Path target) throws Exception {
                invoked.incrementAndGet();
                Files.writeString(target, "%PDF-1.7 test");
            }
        };
        new OfficePreviewConversion(primary, List.of(fallback))
                .convert(tempDir.resolve("source.docx").toString(), result.toString(), file);
        assertEquals("%PDF-1.7 test", Files.readString(result));
        assertEquals(1, invoked.get());
    }

    @Test
    void doesNotInvokeFallbackWhenLibreOfficeSucceeds() throws Exception {
        OfficeToPdfService primary = mock(OfficeToPdfService.class);
        OfficePreviewFallbackConverter fallback = mock(OfficePreviewFallbackConverter.class);
        FileAttribute file = file("docx");
        new OfficePreviewConversion(primary, List.of(fallback)).convert("word.docx", "out.pdf", file);
        verifyNoInteractions(fallback);
    }

    @Test
    void remainsOptionalAndPreservesOriginalFailure() throws Exception {
        OfficeToPdfService primary = mock(OfficeToPdfService.class);
        FileAttribute file = file("docx");
        OfficeException original = new OfficeException("LO failed");
        doThrow(original).when(primary).openOfficeToPDF(anyString(), anyString(), eq(file));
        OfficeException thrown = assertThrows(OfficeException.class,
                () -> new OfficePreviewConversion(primary, List.of())
                        .convert("word.docx", "out.pdf", file));
        assertSame(original, thrown);
    }

    @Test
    void refusesFallbackForNonWordOrHtml() throws Exception {
        OfficeToPdfService primary = mock(OfficeToPdfService.class);
        doThrow(new OfficeException("LO failed")).when(primary)
                .openOfficeToPDF(anyString(), anyString(), any());
        OfficePreviewFallbackConverter fallback = mock(OfficePreviewFallbackConverter.class);
        for (String ext : new String[]{"xlsx", "pptx"}) {
            FileAttribute file = file(ext);
            assertThrows(OfficeException.class,
                    () -> new OfficePreviewConversion(primary, List.of(fallback))
                            .convert("source." + ext, "out.pdf", file));
        }
        assertThrows(OfficeException.class,
                () -> new OfficePreviewConversion(primary, List.of(fallback))
                        .convert("source.docx", "out.html", file("docx")));
        verifyNoInteractions(fallback);
    }

    @Test
    void htmlErrorCannotBePublishedAsPdf() throws Exception {
        OfficeToPdfService primary = mock(OfficeToPdfService.class);
        FileAttribute file = file("docx");
        doThrow(new OfficeException("LO failed")).when(primary)
                .openOfficeToPDF(anyString(), anyString(), eq(file));
        OfficePreviewFallbackConverter fallback = new OfficePreviewFallbackConverter() {
            public String name() { return "html-error"; }
            public boolean supports(String extension) { return true; }
            public void convert(Path source, Path target) throws Exception {
                Files.writeString(target, "<html>timeout</html>");
            }
        };
        Path result = tempDir.resolve("nonpdf.pdf");
        assertThrows(OfficeException.class, () ->
                new OfficePreviewConversion(primary, List.of(fallback))
                        .convert("word.docx", result.toString(), file));
        assertFalse(Files.exists(result));
    }

    @Test
    void badFallbackDoesNotPublishEmptyPdfOrHidePrimaryFailure() throws Exception {
        OfficeToPdfService primary = mock(OfficeToPdfService.class);
        FileAttribute file = file("doc");
        OfficeException error = new OfficeException("LO failed");
        doThrow(error).when(primary).openOfficeToPDF(anyString(), anyString(), eq(file));
        OfficePreviewFallbackConverter fallback = new OfficePreviewFallbackConverter() {
            public String name() { return "no-output"; }
            public boolean supports(String extension) { return true; }
            public void convert(Path source, Path target) { }
        };
        Path target = tempDir.resolve("result.pdf");
        OfficeException thrown = assertThrows(OfficeException.class,
                () -> new OfficePreviewConversion(primary, List.of(fallback))
                        .convert("source.doc", target.toString(), file));
        assertSame(error, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertFalse(Files.exists(target));
    }
}
