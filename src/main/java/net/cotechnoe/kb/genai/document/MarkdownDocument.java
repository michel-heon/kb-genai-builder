package net.cotechnoe.kb.genai.document;

import java.nio.file.Path;
import java.util.Objects;

/** Canonical deterministic result of a source document transformation. */
public record MarkdownDocument(Path source, String content) {

    public MarkdownDocument {
        source = Objects.requireNonNull(source, "source must not be null").toAbsolutePath().normalize();
        content = Objects.requireNonNull(content, "content must not be null");
    }
}