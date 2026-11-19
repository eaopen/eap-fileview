package io.github.eaopen.fileview.conversion;

import java.util.List;
import java.util.Objects;

/**
 * Transport-neutral conversion result. External CAD SDKs and project workers
 * run outside the public preview engine; artifact references are opaque
 * project identifiers, not filesystem paths or permanent download URLs.
 */
public record ConversionJobResult(
        String jobId,
        Status status,
        String sourceFormat,
        String sourceRevision,
        String converterId,
        List<Artifact> artifacts) {

    public enum Status { PENDING, RUNNING, COMPLETED, FAILED, CANCELLED }
    public enum Role { DRAWING, MODEL, THUMBNAIL }
    public enum Format { PDF, SVG, PNG, JPG, JPEG, GLB, GLTF, STL, OBJ, STEP, IGES }

    public record Artifact(Role role, Format format, String reference) {
        public Artifact {
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(format, "format");
            requireText(reference, "artifact reference");
        }
    }

    public ConversionJobResult {
        requireText(jobId, "jobId");
        Objects.requireNonNull(status, "status");
        requireText(sourceFormat, "sourceFormat");
        requireText(sourceRevision, "sourceRevision");
        requireText(converterId, "converterId");
        artifacts = List.copyOf(Objects.requireNonNull(artifacts, "artifacts"));
        if (status == Status.COMPLETED && artifacts.isEmpty()) {
            throw new IllegalArgumentException("completed job must contain at least one artifact");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
