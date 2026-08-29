package net.cotechnoe.kb.genai.document.neosante;

import java.nio.file.Path;
import java.util.Objects;

/** A candidate article extracted from one review PDF, including source provenance. */
public record ArticleCandidate(Path source, int startPage, int endPage, String text) {

    public ArticleCandidate {
        source = Objects.requireNonNull(source, "source must not be null").toAbsolutePath().normalize();
        if (startPage < 1) {
            throw new IllegalArgumentException("startPage must be greater than zero");
        }
        if (endPage < startPage) {
            throw new IllegalArgumentException("endPage must not precede startPage");
        }
        text = Objects.requireNonNull(text, "text must not be null").strip();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("text must not be blank");
        }
    }
}
