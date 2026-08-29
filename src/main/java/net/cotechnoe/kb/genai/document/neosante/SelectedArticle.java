package net.cotechnoe.kb.genai.document.neosante;

import java.util.Objects;

/** A relevant article and its semantic selection metadata. */
public record SelectedArticle(ArticleCandidate candidate, ArticleSelection selection) {

    public SelectedArticle {
        candidate = Objects.requireNonNull(candidate, "candidate must not be null");
        selection = Objects.requireNonNull(selection, "selection must not be null");
        if (!selection.relevant()) {
            throw new IllegalArgumentException("a selected article must be relevant");
        }
    }
}
