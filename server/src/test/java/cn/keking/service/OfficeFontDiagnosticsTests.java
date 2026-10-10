package cn.keking.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class OfficeFontDiagnosticsTests {
    @TempDir Path directory;

    @Test
    void findsWordFontDeclarationsButNotUnrelatedMetadata() throws Exception {
        Path docx = directory.resolve("test.docx");
        try (ZipOutputStream archive = new ZipOutputStream(Files.newOutputStream(docx))) {
            archive.putNextEntry(new ZipEntry("word/styles.xml"));
            archive.write(("<w:styles><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"ARIAL\""
                    + " w:eastAsia=\"Uninstalled CJK\" /></w:styles>").getBytes());
            archive.closeEntry();
            archive.putNextEntry(new ZipEntry("docProps/core.xml"));
            archive.write("<x w:ascii=\"Metadata Only\" />".getBytes());
            archive.closeEntry();
        }
        Set<String> referenced = OfficeFontDiagnostics.referencedFonts(docx);
        assertEquals(2, referenced.size());
        assertTrue(referenced.contains("Uninstalled CJK"));
        assertFalse(referenced.contains("Metadata Only"));
        assertEquals(Set.of("Uninstalled CJK"),
                OfficeFontDiagnostics.missingFonts(referenced, Set.of("arial")));
    }

    @Test
    void diagnosticsNeverBlocksInvalidFile() throws Exception {
        Path invalid = directory.resolve("bad.docx");
        Files.writeString(invalid, "not a Word file");
        assertThrows(IOException.class, () -> OfficeFontDiagnostics.referencedFonts(invalid));
        assertDoesNotThrow(() -> OfficeFontDiagnostics.logPotentiallyMissingFonts(invalid, "docx"));
        assertDoesNotThrow(() -> OfficeFontDiagnostics.logPotentiallyMissingFonts(invalid, "doc"));
    }

    @Test
    void skipsOversizedWordXml() throws Exception {
        Path docx = directory.resolve("large.docx");
        try (ZipOutputStream archive = new ZipOutputStream(Files.newOutputStream(docx))) {
            archive.putNextEntry(new ZipEntry("word/document.xml"));
            byte[] large = new byte[2 * 1024 * 1024 + 1];
            Arrays.fill(large, (byte) 'x');
            archive.write(large);
            archive.closeEntry();
        }
        assertTrue(OfficeFontDiagnostics.referencedFonts(docx).isEmpty());
    }
}
