package net.cotechnoe.kb.genai.document.neosante;

import java.util.List;
import java.util.Objects;

/** Semantic decision returned by an article selector. */
public record ArticleSelection(boolean relevant, String title, List<String> topics, String rationale) {

    public ArticleSelection {
        title = Objects.requireNonNull(title, "title must not be null").strip();
        topics = List.copyOf(Objects.requireNonNull(topics, "topics must not be null"));
        rationale = Objects.requireNonNull(rationale, "rationale must not be null").strip();
        if (relevant && title.isEmpty()) {
            throw new IllegalArgumentException("a relevant article must have a title");
        }
    }

    public static ArticleSelection rejected(String rationale) {
        return new ArticleSelection(false, "", List.of(), rationale);
    }
}
