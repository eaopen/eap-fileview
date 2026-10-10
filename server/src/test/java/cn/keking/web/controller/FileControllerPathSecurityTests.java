package cn.keking.web.controller;

import cn.keking.config.ConfigConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileControllerPathSecurityTests {

    @TempDir
    Path tempDir;

    private String originalFileDir;

    @BeforeEach
    void rememberConfiguredFileDirectory() {
        originalFileDir = ConfigConstants.getFileDir();
    }

    @AfterEach
    void restoreConfiguredFileDirectory() {
        ConfigConstants.setFileDirValue(originalFileDir);
    }

    @Test
    void shouldResolveDirectoriesInsideDemoRoot() throws IOException {
        Path demoRoot = Files.createDirectory(tempDir.resolve("demo"));
        Path nested = Files.createDirectories(demoRoot.resolve("folder/subfolder"));

        assertEquals(demoRoot.toRealPath(), FileController.resolveDirectoryUnderRoot(demoRoot, ""));
        assertEquals(nested.toRealPath(), FileController.resolveDirectoryUnderRoot(demoRoot, "folder/subfolder"));
        assertEquals(nested.toRealPath(), FileController.resolveDirectoryUnderRoot(demoRoot, "folder\\subfolder"));
    }

    @Test
    void shouldRejectParentTraversalWithEitherSeparator() throws IOException {
        Path demoRoot = Files.createDirectory(tempDir.resolve("demo"));

        assertThrows(SecurityException.class,
                () -> FileController.resolveDirectoryUnderRoot(demoRoot, "../outside"));
        assertThrows(SecurityException.class,
                () -> FileController.resolveDirectoryUnderRoot(demoRoot, "..\\outside"));
        assertThrows(SecurityException.class,
                () -> FileController.resolveDirectoryUnderRoot(demoRoot, "folder/../outside"));
    }

    @Test
    void shouldRejectAbsoluteDriveAndUncPaths() throws IOException {
        Path demoRoot = Files.createDirectory(tempDir.resolve("demo"));

        assertThrows(SecurityException.class,
                () -> FileController.resolveDirectoryUnderRoot(demoRoot, "/etc"));
        assertThrows(SecurityException.class,
                () -> FileController.resolveDirectoryUnderRoot(demoRoot, "C:\\Windows"));
        assertThrows(SecurityException.class,
                () -> FileController.resolveDirectoryUnderRoot(demoRoot, "\\\\server\\share"));
    }

    @Test
    void shouldRejectSymlinkThatEscapesDemoRoot() throws IOException {
        Path demoRoot = Files.createDirectory(tempDir.resolve("demo"));
        Path outside = Files.createDirectory(tempDir.resolve("outside"));
        Path link = demoRoot.resolve("outside-link");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (IOException | UnsupportedOperationException e) {
            Assumptions.assumeTrue(false, "Symbolic links are unavailable in this environment");
        }

        assertThrows(SecurityException.class,
                () -> FileController.resolveDirectoryUnderRoot(demoRoot, "outside-link"));
    }

    @Test
    void listFilesShouldNotExposeEntriesOutsideDemoRoot() throws IOException {
        Files.createDirectory(tempDir.resolve("demo"));
        Files.createFile(tempDir.resolve("outside-secret.txt"));
        ConfigConstants.setFileDirValue(tempDir.toString());
        FileController controller = new FileController();

        Map<String, Object> result = controller.getFiles("..", "", 0, 20, null, null);

        assertEquals("非法目录路径", result.get("error"));
        assertTrue(((List<?>) result.get("data")).isEmpty());
    }
}
