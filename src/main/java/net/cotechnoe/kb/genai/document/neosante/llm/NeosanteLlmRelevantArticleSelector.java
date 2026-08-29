package net.cotechnoe.kb.genai.document.neosante.llm;

import net.cotechnoe.kb.genai.document.neosante.ArticleCandidate;
import net.cotechnoe.kb.genai.document.neosante.ArticleExtractionException;
import net.cotechnoe.kb.genai.document.neosante.ArticleSelection;
import net.cotechnoe.kb.genai.document.neosante.NeosanteRelevantArticleSelector;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Néosanté LLM-backed implementation of the project-owned relevance-selection port. */
public final class NeosanteLlmRelevantArticleSelector implements NeosanteRelevantArticleSelector {

    private static final String RELEVANT = "RELEVANT";
    private static final String NOT_RELEVANT = "NOT_RELEVANT";

    private final NeosanteArticleClassificationModel model;

    public NeosanteLlmRelevantArticleSelector(NeosanteArticleClassificationModel model) {
        this.model = Objects.requireNonNull(model, "model must not be null");
    }

    @Override
    public ArticleSelection select(ArticleCandidate candidate) {
        String response;
        try {
            response = model.classify(prompt(candidate));
        } catch (RuntimeException exception) {
            throw new ArticleExtractionException("LLM article classification failed for " + candidate.source(), exception);
        }
        return parse(response);
    }

    static String prompt(ArticleCandidate candidate) {
        return """
                Tu es un classificateur documentaire strict. Détermine si le texte ci-dessous est un article
                dont le sujet principal concerne la biologie totale ou le décodage biologique.
                Réponds exactement par trois lignes sans Markdown :
                DECISION: RELEVANT ou NOT_RELEVANT
                TITLE: titre fidèle et concis (vide si NOT_RELEVANT)
                TOPICS: biologie totale, décodage biologique (liste vide si NOT_RELEVANT)
                Une simple mention, publicité, sommaire ou éditorial générique ne suffit pas.

                SOURCE: %s, pages %d-%d
                TEXTE:
                %s
                """.formatted(candidate.source().getFileName(), candidate.startPage(), candidate.endPage(), candidate.text());
    }

    static ArticleSelection parse(String response) {
        if (response == null || response.isBlank()) {
            throw new ArticleExtractionException("LLM article classification returned an empty response");
        }
        String decision = value(response, "DECISION:").toUpperCase(Locale.ROOT);
        if (NOT_RELEVANT.equals(decision)) {
            return ArticleSelection.rejected("LLM classified the candidate as not relevant");
        }
        if (!RELEVANT.equals(decision)) {
            throw new ArticleExtractionException("LLM article classification returned an invalid decision");
        }
        String title = value(response, "TITLE:");
        String topics = value(response, "TOPICS:");
        return new ArticleSelection(true, title,
                topics.isBlank() ? List.of() : List.of(topics.split("\\s*,\\s*")),
                "LLM semantic classification");
    }

    private static String value(String response, String prefix) {
        return response.lines()
                .map(String::strip)
                .filter(line -> line.startsWith(prefix))
                .findFirst()
                .map(line -> line.substring(prefix.length()).strip())
                .orElse("");
    }
}
