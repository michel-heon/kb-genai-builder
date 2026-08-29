package net.cotechnoe.kb.genai.document.neosante;

import java.util.Objects;
import java.util.stream.Collectors;

/** Transcribes selected source content to Markdown while preserving its source and page provenance. */
public final class NeosanteArticleMarkdownTranscriber {

    public String transcribe(SelectedArticle article) {
        Objects.requireNonNull(article, "article must not be null");
        ArticleCandidate candidate = article.candidate();
        ArticleSelection selection = article.selection();
        String topics = selection.topics().stream().collect(Collectors.joining(", "));
        String content = NeosanteReviewTextNormalizer.normalize(candidate.text()).strip();
        int sourceTitleStart = sourceTitleStart(content);
        String title = sourceTitleOr(selection.title(), content, sourceTitleStart);
        String body = sourceTitleStart >= 0
                ? content.substring(content.indexOf('\n', sourceTitleStart) + 1).stripLeading()
                : content;
        return """
                ---
                source: %s
                pages: %d-%d
                topics: %s
                selection: %s
                ---

                # %s

                %s
                """.formatted(
                candidate.source().getFileName(),
                candidate.startPage(),
                candidate.endPage(),
                yamlValue(topics),
                yamlValue(selection.rationale()),
                title,
                body).strip() + "\n";
    }

    private static int sourceTitleStart(String content) {
        int editorialMarkerEnd = content.indexOf('\n');
        if (editorialMarkerEnd >= 0 && content.substring(0, editorialMarkerEnd).strip().equalsIgnoreCase("édito")) {
            int titleStart = editorialMarkerEnd + 1;
            return content.startsWith("# ", titleStart) ? titleStart : -1;
        }
        return -1;
    }

    private static String sourceTitleOr(String selectionTitle, String content, int sourceTitleStart) {
        if (sourceTitleStart < 0) {
            return selectionTitle;
        }
        int titleStart = sourceTitleStart + 2;
        int lineEnd = content.indexOf('\n', titleStart);
        return lineEnd < 0 ? content.substring(titleStart).strip() : content.substring(titleStart, lineEnd).strip();
    }

    private static String yamlValue(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
