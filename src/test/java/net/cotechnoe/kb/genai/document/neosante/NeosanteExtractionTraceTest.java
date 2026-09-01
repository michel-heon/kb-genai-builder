package net.cotechnoe.kb.genai.document.neosante;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NeosanteExtractionTraceTest {

    @Test
    void prefixesEveryTraceEventWithTheSourcePdfName() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            NeosanteExtractionTrace trace = NeosanteExtractionTrace.standardOutput();
            trace.extractionStarted(Path.of("collection", "neosante02.pdf"), 40);
            trace.pageClassified(3, true);
            trace.articleSelected(new ArticleCandidate(Path.of("collection", "neosante02.pdf"), 2, 3, "Texte"));
            trace.extractionCompleted(1);
        } finally {
            System.setOut(originalOut);
        }

        assertEquals("""
                [Néosanté][neosante02.pdf] Source : 40 pages
                [Néosanté][neosante02.pdf] Page 3 : retenue
                [Néosanté][neosante02.pdf] Article retenu : pages 2-3
                [Néosanté][neosante02.pdf] Extraction terminée : 1 article(s) retenu(s)
                """, output.toString(StandardCharsets.UTF_8));
    }
}