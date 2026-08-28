package net.cotechnoe.kb.genai.document;

/** Stable failure exposed when a PDF cannot be transformed. */
public final class PdfTransformationException extends RuntimeException {

    public PdfTransformationException(String message) {
        super(message);
    }

    public PdfTransformationException(String message, Throwable cause) {
        super(message, cause);
    }
}