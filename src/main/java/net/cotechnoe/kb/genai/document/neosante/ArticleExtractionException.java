package net.cotechnoe.kb.genai.document.neosante;

/** Raised when a review cannot be analyzed or transcribed into article Markdown. */
public final class ArticleExtractionException extends RuntimeException {

    public ArticleExtractionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ArticleExtractionException(String message) {
        super(message);
    }
}
