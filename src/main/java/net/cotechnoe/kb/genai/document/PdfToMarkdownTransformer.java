package net.cotechnoe.kb.genai.document;

import java.nio.file.Path;

/** Port for deterministic transformations from one PDF file to Markdown. */
public interface PdfToMarkdownTransformer {

    MarkdownDocument transform(Path pdf);
}