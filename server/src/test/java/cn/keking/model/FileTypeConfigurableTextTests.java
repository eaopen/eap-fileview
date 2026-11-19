package cn.keking.model;

import cn.keking.config.ConfigConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileTypeConfigurableTextTests {
    private String[] previousTextSuffixes;

    @BeforeEach
    void saveConfiguration() {
        previousTextSuffixes = ConfigConstants.getSimText().clone();
    }

    @AfterEach
    void restoreConfiguration() {
        ConfigConstants.setSimTextValue(previousTextSuffixes);
    }

    @Test
    void newTextSuffixesWorkEvenWhenFileTypeWasPreviouslyInitialized() {
        assertEquals(FileType.PDF, FileType.typeFromFileName("existing.pdf"));
        ConfigConstants.setSimTextValue(new String[]{"dat", " .erg ", "DTA_FROM_MOD1"});
        assertEquals(FileType.SIMTEXT, FileType.typeFromFileName("calculation.DAT"));
        assertEquals(FileType.SIMTEXT, FileType.typeFromFileName("sample.erg"));
        assertEquals(FileType.SIMTEXT, FileType.typeFromFileName("result.dta_from_mod1"));
    }

    @Test
    void replacingTextSuffixesDoesNotLeaveStaleMappings() {
        ConfigConstants.setSimTextValue(new String[]{"dat"});
        assertEquals(FileType.SIMTEXT, FileType.typeFromFileName("sample.dat"));
        ConfigConstants.setSimTextValue(new String[]{"out"});
        assertEquals(FileType.OTHER, FileType.typeFromFileName("sample.dat"));
        assertEquals(FileType.SIMTEXT, FileType.typeFromFileName("sample.out"));
    }

    @Test
    void configuredTextCannotReplaceKnownPreviewHandlers() {
        ConfigConstants.setSimTextValue(new String[]{"pdf", "xlsx", "txt"});
        assertEquals(FileType.PDF, FileType.typeFromFileName("plan.pdf"));
        assertEquals(FileType.OFFICE, FileType.typeFromFileName("report.xlsx"));
        assertEquals(FileType.SIMTEXT, FileType.typeFromFileName("notes.txt"));
        assertEquals(FileType.OTHER, FileType.typeFromFileName("unknown.abcd"));
    }

    @Test
    void nullTextConfigurationIsTreatedAsEmpty() {
        ConfigConstants.setSimTextValue(null);
        assertEquals(FileType.OTHER, FileType.typeFromFileName("sample.dat"));
    }
}
