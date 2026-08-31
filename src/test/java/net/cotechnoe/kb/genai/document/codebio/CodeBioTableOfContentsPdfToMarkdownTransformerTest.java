package net.cotechnoe.kb.genai.document.codebio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeBioTableOfContentsPdfToMarkdownTransformerTest {

    @Test
    void formatsEntriesAndPreservesAmbiguousOcrFragments() {
        String source = """
                Tables des matières
                Abcès ................................................................ 1
                Accident vasculaire cérébral .............................. 4
                t=.:-· Accident ............................................ 3
                Tables des matières
                Fragment OCR illisible
                """;

        assertEquals("""
                # Table des matières — Dictionnaire CodeBio

                > Transcription OCR structurée automatiquement. Les fragments ambigus sont conservés sans correction interprétative.

                ## A

                - Abcès — p. 1
                - Accident vasculaire cérébral — p. 4

                <details>
                <summary>Fragments OCR non structurés (2)</summary>

                Ces lignes ne peuvent pas être associées de manière fiable à une entrée et à une page.

                - `t=.:-· Accident 3`
                - `Fragment OCR illisible`

                </details>

                """, CodeBioTableOfContentsPdfToMarkdownTransformer.format(source));
    }
}
