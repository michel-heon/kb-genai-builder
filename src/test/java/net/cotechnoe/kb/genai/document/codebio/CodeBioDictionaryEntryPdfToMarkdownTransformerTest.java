package net.cotechnoe.kb.genai.document.codebio;

import net.cotechnoe.kb.genai.document.PdfTransformationException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CodeBioDictionaryEntryPdfToMarkdownTransformerTest {

    @Test
    void parsesEntriesFromTheDedicatedTableOfContentsFormat() {
        assertEquals(List.of(
                        new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Amylase", 32),
                        new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Amyloïdose", 33)),
                CodeBioDictionaryEntryPdfToMarkdownTransformer.parseTableOfContents("""
                        # Table des matières — Dictionnaire CodeBio

                        ## A

                        - Amylase — p. 32
                        - Amyloïdose — p. 33
                        """));
    }

    @Test
    void recognizesHeadingWithOcrAnnotation() {
        assertEquals(true, CodeBioDictionaryEntryPdfToMarkdownTransformer
                .headingMatchesTitle("Angine lv", "Angine"));
    }

    @Test
    void formatsAnEntryWithReadableSectionHeadings() {
        assertEquals("""
                # Amylase

                > Source : *Dictionnaire des codes biologies des maladies*, p. 32.

                Étym : Amylase

                ## DÉFINITION

                Enzyme digestive.

                ## CONFLITS

                Conflit familial.
                """, CodeBioDictionaryEntryPdfToMarkdownTransformer.format(
                new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Amylase", 32), """
                        Étym : Amylase

                        DÉFINITION : Enzyme digestive.

                        CONFLITS
                        Conflit familial.

                        32
                        """));
    }

    @Test
    void preservesNonCodeBioLabelsAsContent() {
        assertEquals("""
                # Angine

                > Source : *Dictionnaire des codes biologies des maladies*, p. 37.

                Sens biologique: Contenu conservé.
                """, CodeBioDictionaryEntryPdfToMarkdownTransformer.format(
                new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Angine", 37),
                "Sens biologique: Contenu conservé.", Set.of("definition", "conflits")));
    }

    @Test
    void formatsVerifiedIndexReferenceWithoutInventingAnAutonomousEntry() {
        var indexedEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Abcès pulmonaire", 1);
        var canonicalEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Abcès", 1);
        var resolution = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexResolution(
                "Abcès pulmonaire", "Abcès", "Abcès pulmonaire: preuve source.", "manually_verified");

        assertEquals("""
                # Abcès pulmonaire

                > Type : référence d'index vérifiée ; ce terme n'est pas un en-tête autonome du dictionnaire.
                > Source de l'index : *Dictionnaire des codes biologies des maladies*, p. 1.
                > Entrée canonique : [Abcès](./Abcès.md), p. 1.
                > Statut de vérification : manually_verified.

                ## Extrait source

                > Abcès pulmonaire: preuve source.
                """, CodeBioDictionaryEntryPdfToMarkdownTransformer.formatReference(
                indexedEntry, canonicalEntry, "Contenu. Abcès pulmonaire: preuve source.", resolution));
    }

    @Test
    void rejectsAReferenceWhenItsConfiguredEvidenceIsAbsent() {
        var indexedEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Abcès pulmonaire", 1);
        var canonicalEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Abcès", 1);
        var resolution = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexResolution(
                "Abcès pulmonaire", "Abcès", "Preuve absente.", "manually_verified");

        assertThrows(PdfTransformationException.class, () -> CodeBioDictionaryEntryPdfToMarkdownTransformer.formatReference(
                indexedEntry, canonicalEntry, "Contenu source sans cette preuve.", resolution));
    }

    @Test
    void formatsUnresolvedIndexReferenceWithoutInventingContent() {
        assertEquals("""
                # Abstinence

                > Type : référence d'index non résolue ; ce terme est présent dans la table des matières mais n'a pas été trouvé comme en-tête autonome du dictionnaire.
                > Source de l'index : *Dictionnaire des codes biologies des maladies*, p. 2.
                > Statut de vérification : à rapprocher manuellement d'une entrée canonique avant toute interprétation.

                ## Contenu

                > Aucun contenu n'est généré afin de ne pas inventer de correspondance ou d'information médicale.
                """, CodeBioDictionaryEntryPdfToMarkdownTransformer.formatUnresolvedIndexReference(
                new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Abstinence", 2)));
    }

    @Test
    void formatsIndexRedirectReferenceWithTheCanonicalContentForRagRetrieval() {
        var indexedEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry(
                "Adamantinome --> Améloblastome", 29);
        var canonicalEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Améloblastome", 29);

        assertEquals("""
                # Adamantinome --> Améloblastome

                > Type : renvoi d'index transcrit ; la relation ci-dessous est explicitement indiquée par la table des matières.
                > Termes de recherche : Adamantinome --> Améloblastome; Améloblastome.
                > Source de l'index : *Dictionnaire des codes biologies des maladies*, p. 29.
                > Entrée cible : [Améloblastome](./Améloblastome.md), p. 29.

                ## Renvoi source

                > Adamantinome --> Améloblastome

                ## Contenu de l'entrée cible

                ## DÉFINITION

                Contenu transcrit.
                """, CodeBioDictionaryEntryPdfToMarkdownTransformer.formatIndexRedirectReference(
                indexedEntry, canonicalEntry, "DÉFINITION : Contenu transcrit.", Set.of("definition")));
    }

    @Test
    void readsVerifiedResolutionFromPropertiesRegistry() throws IOException {
        Path registry = Files.createTempFile("codebio-resolutions", ".properties");
        Files.writeString(registry, """
                resolution.1.indexed_term=Abcès pulmonaire
                resolution.1.canonical_entry=Abcès
                resolution.1.evidence_text=Preuve source.
                resolution.1.review_status=manually_verified
                """);

        assertEquals("Abcès", CodeBioDictionaryEntryPdfToMarkdownTransformer
                .findResolution(registry, "Abcès pulmonaire")
                .orElseThrow()
                .canonicalEntry());
    }
}
