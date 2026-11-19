package io.github.eaopen.fileview.conversion;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static io.github.eaopen.fileview.conversion.ConversionJobResult.*;
import static org.junit.jupiter.api.Assertions.*;

class ConversionPreviewUrlBuilderTests {
    private static final URI FILEVIEW = URI.create("https://preview.example/fileview");
    private static final URI PDF = URI.create(
            "https://files.example/artifacts/plan.pdf?X-Signed=a%2Bb&expiry=600");
    private static final URI GLB = URI.create(
            "https://files.example/artifacts/model.glb?ticket=one%2Ftwo");

    private static ConversionJobResult complete() {
        return new ConversionJobResult("job-123", Status.COMPLETED, "nx-prt",
                "sha256:abc", "project-nx-worker",
                List.of(new Artifact(Role.MODEL, Format.GLB, "artifact-model"),
                        new Artifact(Role.DRAWING, Format.PDF, "artifact-drawing")));
    }

    @Test
    void autoPrefersDrawingAndPreservesSignedUrlWithoutModification() {
        AtomicReference<String> resolved = new AtomicReference<>();
        URI link = ConversionPreviewUrlBuilder.build(FILEVIEW, complete(),
                ConversionPreviewUrlBuilder.Selection.AUTO, ref -> {
                    resolved.set(ref);
                    return PDF;
                });
        assertEquals("artifact-drawing", resolved.get());
        assertTrue(link.toString().startsWith(
                "https://preview.example/fileview/onlinePreview?url="));
        assertEquals(PDF.toASCIIString(), sourceUrlEncodedIn(link));
        assertFalse(link.toString().contains("X-Signed="),
                "signed query must remain inside the encoded source URL");
    }

    @Test
    void projectCanSelectModelAndUseExisting3dPreviewPath() {
        AtomicReference<String> resolved = new AtomicReference<>();
        URI link = ConversionPreviewUrlBuilder.build(FILEVIEW, complete(),
                ConversionPreviewUrlBuilder.Selection.MODEL, ref -> {
                    resolved.set(ref);
                    return GLB;
                });
        assertEquals("artifact-model", resolved.get());
        assertEquals(GLB.toASCIIString(), sourceUrlEncodedIn(link));
    }

    @Test
    void rejectsUnreadyJobsBeforeLookingUpArtifacts() {
        for (Status status : List.of(Status.PENDING, Status.RUNNING, Status.FAILED, Status.CANCELLED)) {
            ConversionJobResult job = new ConversionJobResult("job", status, "nx-prt",
                    "r1", "worker", List.of());
            assertThrows(IllegalStateException.class, () ->
                    ConversionPreviewUrlBuilder.build(FILEVIEW, job,
                            ConversionPreviewUrlBuilder.Selection.AUTO, ref -> PDF));
        }
    }

    @Test
    void rejectsMissingRoleAndIncompleteResult() {
        assertThrows(IllegalArgumentException.class, () ->
                ConversionPreviewUrlBuilder.build(FILEVIEW, complete(),
                        ConversionPreviewUrlBuilder.Selection.THUMBNAIL, ref -> PDF));
        assertThrows(IllegalArgumentException.class, () ->
                new ConversionJobResult("job", Status.COMPLETED, "dft", "r1", "worker",
                        List.of()));
    }

    @Test
    void rejectsNonHttpOrEmbeddedCredentialsOrUrlFragments() {
        for (URI invalid : List.of(URI.create("file:///tmp/plan.pdf"),
                URI.create("ftp://files.example/plan.pdf"),
                URI.create("https://user:pass@files.example/plan.pdf"),
                URI.create("https://files.example/plan.pdf#fragment"))) {
            assertThrows(IllegalArgumentException.class, () ->
                    ConversionPreviewUrlBuilder.build(FILEVIEW, complete(),
                            ConversionPreviewUrlBuilder.Selection.DRAWING, ref -> invalid));
        }
        assertThrows(IllegalArgumentException.class, () ->
                ConversionPreviewUrlBuilder.build(
                        URI.create("https://preview.example/fileview?token=secret"),
                        complete(), ConversionPreviewUrlBuilder.Selection.DRAWING, ref -> PDF));
    }

    @Test
    void requiresFilenameSuffixWithoutMutatingPresignedUrl() {
        URI opaqueSignedUrl = URI.create("https://files.example/download/123?signature=abc");
        assertThrows(IllegalArgumentException.class, () ->
                ConversionPreviewUrlBuilder.build(FILEVIEW, complete(),
                        ConversionPreviewUrlBuilder.Selection.DRAWING, ref -> opaqueSignedUrl));
    }

    @Test
    void jobArtifactListIsDefensivelyCopied() {
        ArrayList<Artifact> mutable = new ArrayList<>();
        mutable.add(new Artifact(Role.MODEL, Format.GLB, "model-ref"));
        ConversionJobResult job = new ConversionJobResult("job", Status.COMPLETED,
                "jt", "revision-a", "worker", mutable);
        mutable.clear();
        assertEquals(1, job.artifacts().size());
        assertThrows(UnsupportedOperationException.class, () ->
                job.artifacts().clear());
    }

    private static String sourceUrlEncodedIn(URI link) {
        String raw = link.getRawQuery();
        assertTrue(raw.startsWith("url="));
        String base64 = URLDecoder.decode(raw.substring(4), StandardCharsets.UTF_8);
        return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
    }
}
