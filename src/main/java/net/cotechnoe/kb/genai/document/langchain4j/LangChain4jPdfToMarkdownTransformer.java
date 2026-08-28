package net.cotechnoe.kb.genai.document.langchain4j;

import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import net.cotechnoe.kb.genai.document.MarkdownDocument;
import net.cotechnoe.kb.genai.document.PdfToMarkdownTransformer;
import net.cotechnoe.kb.genai.document.PdfTransformationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/** LangChain4j adapter using PDFBox; it performs no network operation. */
public final class LangChain4jPdfToMarkdownTransformer implements PdfToMarkdownTransformer {

    @Override
    public MarkdownDocument transform(Path pdf) {
        Path source = validate(pdf);
        try (InputStream input = Files.newInputStream(source)) {
            String text = new ApachePdfBoxDocumentParser().parse(input).text();
            return new MarkdownDocument(source, normalizeMarkdown(text));
        } catch (IOException | RuntimeException exception) {
            throw new PdfTransformationException("Unable to transform PDF: " + source, exception);
        }
    }

    private static Path validate(Path pdf) {
        Path source = Objects.requireNonNull(pdf, "pdf must not be null").toAbsolutePath().normalize();
        if (!Files.isRegularFile(source)) {
            throw new PdfTransformationException("PDF source must be a readable file: " + source);
        }
        if (!source.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new PdfTransformationException("PDF source must use the .pdf extension: " + source);
        }
        return source;
    }

    private static String normalizeMarkdown(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n').strip() + "\n";
    }
}