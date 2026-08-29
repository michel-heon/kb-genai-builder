package net.cotechnoe.kb.genai.document.neosante.langchain4j;

import net.cotechnoe.kb.genai.document.neosante.ArticleCandidate;
import net.cotechnoe.kb.genai.document.neosante.ArticleExtractionException;
import net.cotechnoe.kb.genai.document.neosante.ArticleSelection;
import net.cotechnoe.kb.genai.document.neosante.NeosanteExtractionTrace;
import net.cotechnoe.kb.genai.document.neosante.NeosanteRelevantArticleSelector;
import net.cotechnoe.kb.genai.document.neosante.NeosanteReviewArticleExtractor;
import net.cotechnoe.kb.genai.document.neosante.SelectedArticle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Extracts review pages locally, then delegates semantic relevance to a project-owned selector. */
public final class LangChain4jNeosanteReviewArticleExtractor implements NeosanteReviewArticleExtractor {

    private final NeosanteRelevantArticleSelector selector;
    private final NeosanteExtractionTrace trace;

    public LangChain4jNeosanteReviewArticleExtractor(NeosanteRelevantArticleSelector selector) {
        this(selector, NeosanteExtractionTrace.SILENT);
    }

    public LangChain4jNeosanteReviewArticleExtractor(
            NeosanteRelevantArticleSelector selector, NeosanteExtractionTrace trace) {
        this.selector = Objects.requireNonNull(selector, "selector must not be null");
        this.trace = NeosanteExtractionTrace.requireNonNull(trace);
    }

    @Override
    public List<SelectedArticle> extract(Path review) {
        Path source = validate(review);
        try (PDDocument document = Loader.loadPDF(source.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<SelectedArticle> selected = new ArrayList<>();
            trace.extractionStarted(source, document.getNumberOfPages());
            ArticleCandidate pending = null;
            ArticleSelection pendingSelection = null;
            ArticleCandidate preceding = null;
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document).strip();
                if (text.isBlank()) {
                    continue;
                }
                ArticleCandidate candidate = new ArticleCandidate(source, page, page, text);
                ArticleSelection decision = selector.select(candidate);
                trace.pageClassified(page, decision.relevant());
                if (decision.relevant()) {
                    if (pending == null) {
                        pending = preceding != null && hasSplitWordAtBoundary(preceding, candidate)
                                ? append(preceding, candidate)
                                : candidate;
                        pendingSelection = decision;
                    } else {
                        pending = append(pending, candidate);
                    }
                } else if (pending != null) {
                    selected.add(new SelectedArticle(pending, pendingSelection));
                    trace.articleSelected(pending);
                    pending = null;
                    pendingSelection = null;
                }
                preceding = candidate;
            }
            if (pending != null) {
                selected.add(new SelectedArticle(pending, pendingSelection));
                trace.articleSelected(pending);
            }
            trace.extractionCompleted(selected.size());
            return List.copyOf(selected);
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof ArticleExtractionException articleExtractionException) {
                throw articleExtractionException;
            }
            throw new ArticleExtractionException("Unable to extract relevant articles from " + source, exception);
        }
    }

    private static ArticleCandidate append(ArticleCandidate current, ArticleCandidate following) {
        return new ArticleCandidate(
                current.source(),
                current.startPage(),
                following.endPage(),
                current.text() + "\n\n" + following.text());
    }

    /**
     * Retains a preceding unselected page only when the PDF itself proves that a short word fragment
     * continues on the following page (for example, {@code sys-} followed by {@code tème}). The short
     * fragment constraint avoids absorbing unrelated pages that merely contain ordinary line wrapping.
     */
    private static boolean hasSplitWordAtBoundary(ArticleCandidate current, ArticleCandidate following) {
        return current.endPage() + 1 == following.startPage()
                && current.text().matches("(?s).*\\b\\p{L}{1,3}[-\\u00AD\\u2010\\u2011]\\h*(?:\\R|$).*")
                && following.text().matches("(?s)^\\p{Ll}.*");
    }

    private static Path validate(Path review) {
        Path source = Objects.requireNonNull(review, "review must not be null").toAbsolutePath().normalize();
        if (!Files.isRegularFile(source)) {
            throw new ArticleExtractionException("Review source must be a readable file: " + source);
        }
        if (!source.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new ArticleExtractionException("Review source must use the .pdf extension: " + source);
        }
        return source;
    }
}
