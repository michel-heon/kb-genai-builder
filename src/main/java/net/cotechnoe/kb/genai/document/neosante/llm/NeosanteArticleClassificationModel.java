package net.cotechnoe.kb.genai.document.neosante.llm;

/** Minimal project-owned boundary for a remote semantic classification request. */
@FunctionalInterface
public interface NeosanteArticleClassificationModel {

    String classify(String prompt);
}
