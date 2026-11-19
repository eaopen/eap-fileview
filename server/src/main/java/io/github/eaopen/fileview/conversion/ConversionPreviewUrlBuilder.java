package io.github.eaopen.fileview.conversion;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Connect project-authorized conversion outputs to the existing
 * /onlinePreview HTTP API, without adding a new public download endpoint.
 * The project authenticates the current user and resolves an opaque artifact
 * reference to a short-lived, read-only HTTP(S) URL before invoking this.
 */
public final class ConversionPreviewUrlBuilder {
    public enum Selection { AUTO, DRAWING, MODEL, THUMBNAIL }

    private ConversionPreviewUrlBuilder() {
    }

    public static URI build(URI fileviewBase,
                            ConversionJobResult job,
                            Selection selection,
                            Function<String, URI> authorizedArtifactResolver) {
        Objects.requireNonNull(job, "job");
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(authorizedArtifactResolver, "authorizedArtifactResolver");
        if (job.status() != ConversionJobResult.Status.COMPLETED) {
            throw new IllegalStateException("conversion job is not completed");
        }
        var artifact = job.artifacts().stream()
                .filter(value -> selection == Selection.AUTO
                        || value.role().name().equals(selection.name()))
                .min(Comparator.comparingInt(value -> order(value.role())))
                .orElseThrow(() -> new IllegalArgumentException("no matching preview artifact"));

        URI artifactUrl = Objects.requireNonNull(
                authorizedArtifactResolver.apply(artifact.reference()),
                "resolver returned no artifact URL");
        requireHttpUrl(artifactUrl, "artifact URL");
        if (!hasExpectedSuffix(artifactUrl, artifact.format())) {
            throw new IllegalArgumentException(
                    "artifact URL path must have the declared file extension; "
                    + "serve signed content through a file-named project gateway");
        }

        requireHttpUrl(fileviewBase, "fileview base URL");
        if (fileviewBase.getRawQuery() != null) {
            throw new IllegalArgumentException("fileview base URL cannot contain query parameters");
        }
        String base = fileviewBase.toASCIIString();
        if (!base.endsWith("/")) {
            base += "/";
        }
        // Reuse the existing controller and its source host trust checks.
        // The signed URL remains a bearer credential; never persist/log this result.
        String encodedUrl = URLEncoder.encode(
                Base64.getEncoder().encodeToString(
                        artifactUrl.toASCIIString().getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);
        return URI.create(base + "onlinePreview?url=" + encodedUrl);
    }

    private static int order(ConversionJobResult.Role role) {
        return switch (role) {
            case DRAWING -> 0;
            case MODEL -> 1;
            case THUMBNAIL -> 2;
        };
    }

    private static void requireHttpUrl(URI uri, String name) {
        if (uri == null || !uri.isAbsolute() || uri.getHost() == null
                || uri.getRawUserInfo() != null || uri.getRawFragment() != null
                || !(uri.getScheme().equalsIgnoreCase("https")
                     || uri.getScheme().equalsIgnoreCase("http"))) {
            throw new IllegalArgumentException(name + " must be an HTTP(S) URI without user-info or fragment");
        }
    }

    private static boolean hasExpectedSuffix(URI url, ConversionJobResult.Format format) {
        String path = url.getPath();
        if (path == null) {
            return false;
        }
        String suffix = "." + format.name().toLowerCase(Locale.ROOT);
        return path.toLowerCase(Locale.ROOT).endsWith(suffix);
    }
}
