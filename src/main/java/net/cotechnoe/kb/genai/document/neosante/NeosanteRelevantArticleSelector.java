package net.cotechnoe.kb.genai.document.neosante;

/** Port that decides whether a candidate is about total biology or biological decoding. */
@FunctionalInterface
public interface NeosanteRelevantArticleSelector {

    ArticleSelection select(ArticleCandidate candidate);
}
