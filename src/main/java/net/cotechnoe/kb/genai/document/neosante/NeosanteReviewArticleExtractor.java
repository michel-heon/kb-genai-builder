package net.cotechnoe.kb.genai.document.neosante;

import java.nio.file.Path;
import java.util.List;

/** Port for extracting relevant articles from a single PDF review. */
@FunctionalInterface
public interface NeosanteReviewArticleExtractor {

    List<SelectedArticle> extract(Path review);
}
