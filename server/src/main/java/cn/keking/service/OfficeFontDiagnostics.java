package cn.keking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Optional best-effort check of fonts explicitly referenced by Word OOXML.
 * This does not claim that LibreOffice actually substituted fonts: embedded
 * EMF/WMF fonts and Office theme font resolution are outside its scope.
 */
public final class OfficeFontDiagnostics {
    private static final Logger log = LoggerFactory.getLogger(OfficeFontDiagnostics.class);
    private static final int MAX_XML_BYTES = 2 * 1024 * 1024;
    private static final Pattern FONT_ATTR =
            Pattern.compile("w:(?:ascii|hAnsi|eastAsia|cs)=\"([^\"]{1,80})\"");

    private OfficeFontDiagnostics() {}

    /** Never block conversion because of font diagnostics. */
    public static void logPotentiallyMissingFonts(Path file, String extension) {
        if (file == null || extension == null
                || !Set.of("docx", "docm").contains(extension.toLowerCase(Locale.ROOT))) {
            return;
        }
        try {
            Set<String> requested = referencedFonts(file);
            if (requested.isEmpty()) return;
            Set<String> available = Set.copyOf(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT)));
            Set<String> missing = missingFonts(requested, available);
            if (!missing.isEmpty()) {
                // Keep logs short and prevent control characters in untrusted font names.
                String names = missing.stream().limit(8)
                        .map(s -> s.replaceAll("[\\p{Cntrl}]", "?"))
                        .reduce((a, b) -> a + ", " + b).orElse("");
                log.warn("Word preview potentially missing {} font families: {}{}",
                        missing.size(), names, missing.size() > 8 ? ", …" : "");
            }
        } catch (IOException | RuntimeException | LinkageError ex) {
            log.debug("Optional Office font diagnostics skipped: {}", ex.toString());
        }
    }

    public static Set<String> referencedFonts(Path file) throws IOException {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try (ZipFile zip = new ZipFile(file.toFile())) {
            for (String path : new String[]{"word/styles.xml", "word/document.xml"}) {
                ZipEntry entry = zip.getEntry(path);
                if (entry == null || entry.isDirectory() || entry.getSize() > MAX_XML_BYTES) continue;
                try (var input = zip.getInputStream(entry)) {
                    byte[] bytes = input.readNBytes(MAX_XML_BYTES + 1);
                    if (bytes.length > MAX_XML_BYTES) continue;
                    Matcher matches = FONT_ATTR.matcher(new String(bytes, StandardCharsets.UTF_8));
                    while (matches.find() && names.size() < 128) {
                        String font = matches.group(1).strip();
                        if (!font.isBlank() && !font.startsWith("+")) names.add(font);
                    }
                }
            }
        }
        return Collections.unmodifiableSet(names);
    }

    /** Case-insensitive family check; absence is a hint, not an actual substitution event. */
    public static Set<String> missingFonts(Set<String> referenced, Set<String> available) {
        Set<String> installed = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        installed.addAll(available);
        Set<String> missing = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String font : referenced) {
            if (!installed.contains(font)) missing.add(font);
        }
        return Collections.unmodifiableSet(missing);
    }
}
