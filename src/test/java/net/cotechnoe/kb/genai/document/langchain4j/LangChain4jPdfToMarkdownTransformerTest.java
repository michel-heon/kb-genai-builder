package net.cotechnoe.kb.genai.document.langchain4j;

import net.cotechnoe.kb.genai.document.MarkdownDocument;
import net.cotechnoe.kb.genai.document.PdfTransformationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LangChain4jPdfToMarkdownTransformerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void extractsTextFromPdfAsMarkdown() throws IOException {
        Path pdf = temporaryDirectory.resolve("source.pdf");
        Files.writeString(pdf, minimalPdf("Bonjour KB"), StandardCharsets.US_ASCII);

        MarkdownDocument result = new LangChain4jPdfToMarkdownTransformer().transform(pdf);

        assertEquals(pdf.toAbsolutePath(), result.source());
        assertEquals("Bonjour KB\n", result.content());
    }

    @Test
    void rejectsNonPdfFilesBeforeParsing() throws IOException {
        Path source = temporaryDirectory.resolve("source.txt");
        Files.writeString(source, "not a PDF", StandardCharsets.UTF_8);

        assertThrows(PdfTransformationException.class,
                () -> new LangChain4jPdfToMarkdownTransformer().transform(source));
    }

    private static String minimalPdf(String text) {
        String objects = "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"
                + "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n"
                + "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n"
                + "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n";
        String content = "BT\n/F1 24 Tf\n72 720 Td\n(" + text + ") Tj\nET\n";
        int offset1 = 9;
        int offset2 = offset1 + objects.indexOf("2 0 obj");
        int offset3 = offset1 + objects.indexOf("3 0 obj");
        int offset4 = offset1 + objects.indexOf("4 0 obj");
        int offset5 = offset1 + objects.length();
        String stream = "5 0 obj\n<< /Length " + content.length() + " >>\nstream\n" + content + "endstream\nendobj\n";
        int xref = offset1 + objects.length() + stream.length();
        return "%PDF-1.4\n" + objects + stream
                + "xref\n0 6\n0000000000 65535 f \n"
                + entry(offset1) + entry(offset2) + entry(offset3) + entry(offset4) + entry(offset5)
                + "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n";
    }

    private static String entry(int offset) {
        return String.format("%010d 00000 n \n", offset);
    }
}