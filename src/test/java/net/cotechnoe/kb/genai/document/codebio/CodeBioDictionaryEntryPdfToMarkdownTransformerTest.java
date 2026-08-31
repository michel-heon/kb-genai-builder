package net.cotechnoe.kb.genai.document.codebio;

import net.cotechnoe.kb.genai.document.PdfTransformationException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        assertTrue(CodeBioDictionaryEntryPdfToMarkdownTransformer
                .headingMatchesTitle("Angine lv", "Angine"));
    }

    @Test
    void recognizesHeadingWithSeveralOcrMarginAnnotations() {
        assertTrue(CodeBioDictionaryEntryPdfToMarkdownTransformer
                .headingMatchesTitle("Abstinence I CA", "Abstinence"));
    }

    @Test
    void recognizesHeadingWithParenthesizedAcronym() {
        assertTrue(CodeBioDictionaryEntryPdfToMarkdownTransformer
                .headingMatchesTitle("Accident vasculaire cérébral (AVC)", "Accident vasculaire cérébral"));
    }

    @Test
    void recognizesEquivalentHeadingPunctuationAndNumericMarginAnnotations() {
        assertTrue(CodeBioDictionaryEntryPdfToMarkdownTransformer
                .headingMatchesTitle("Accouchement (complications)", "Accouchement - complications"));
        assertTrue(CodeBioDictionaryEntryPdfToMarkdownTransformer
                .headingMatchesTitle("Albumine 1 CA", "Albumine"));
    }

    @Test
    void doesNotTreatARealTitleContinuationAsAnOcrAnnotation() {
        assertFalse(CodeBioDictionaryEntryPdfToMarkdownTransformer
                .headingMatchesTitle("Angine de poitrine", "Angine"));
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
    void normalizesKnownOcrErrorInDefinitionHeading() {
        assertEquals("""
                # Abstinence

                > Source : *Dictionnaire des codes biologies des maladies*, p. 2.

                ## DÉFINITION

                Action de s'abstenir.
                """, CodeBioDictionaryEntryPdfToMarkdownTransformer.format(
                new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Abstinence", 2),
                "DÉFTNITION: Action de s'abstenir."));
    }

    @Test
    void resolvesAnIndexedTermFoundInsideAnotherEntryOnTheSamePage() {
        var indexedEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Accès éclamptiques", 228);
        var canonicalEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Eclampsie", 228);

        var reference = CodeBioDictionaryEntryPdfToMarkdownTransformer.findContainingEntryReference("""
                1   Eclampsie
                Étymologie.
                Syn : accès éclamptiques
                DÉFINITION : Accident convulsif.
                1   Ectopie
                """, indexedEntry, List.of(indexedEntry, canonicalEntry,
                new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Ectopie", 229))).orElseThrow();

        assertEquals(canonicalEntry, reference.canonicalEntry());
        assertEquals("Syn : accès éclamptiques", reference.evidenceText());
    }

    @Test
    void assignsAnIndexTermToTheHeadingOnItsOwnLineBeforeSearchingItsEvidence() {
        var indexedEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Appendice", 95);
        var canonicalEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Caecum - appendice", 95);
        var previousEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Bursite", 94);

        var reference = CodeBioDictionaryEntryPdfToMarkdownTransformer.findContainingEntryReference("""
                1   Bursite
                Contenu précédent.
                1   Caecum - appendice
                Appendice : définition source.
                """, indexedEntry, List.of(indexedEntry, previousEntry, canonicalEntry)).orElseThrow();

        assertEquals(canonicalEntry, reference.canonicalEntry());
        assertEquals("Appendice : définition source.", reference.evidenceText());
    }

    @Test
    void resolvesAnIndexedTermUnderAnExplicitPdfHeadingAbsentFromTheStructuredIndex() {
        var indexedEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Arachnoïdite", 54);

        var reference = CodeBioDictionaryEntryPdfToMarkdownTransformer.findContainingEntryReference("""
                1 Arachnoïde (méninges)
                DÉFINITION : Une membrane.
                L'arachnoïdite est une inflammation de l'arachnoïde.
                1 Arachnophilie
                """, indexedEntry, List.of(indexedEntry,
                new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Arachnophilie", 54))).orElseThrow();

        assertEquals(new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Arachnoïde (méninges)", 54),
                reference.canonicalEntry());
        assertEquals("L'arachnoïdite est une inflammation de l'arachnoïde.", reference.evidenceText());
    }

    @Test
    void resolvesAnIndexedTermFromASynonymUnderAnOcrHeadingMarker() {
        var indexedEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Antésystolie", 692);
        var canonicalEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry(
                "Wolff -Parkinson-White (syndrome de)", 692);

        var reference = CodeBioDictionaryEntryPdfToMarkdownTransformer.findContainingEntryReference("""
                j   Wolff -Parkinson-White (syndrome de)
                Syn : antésystolie, syndrome de pré-excitation ventriculaire, WPW
                """, indexedEntry, List.of(indexedEntry, canonicalEntry)).orElseThrow();

        assertEquals(canonicalEntry, reference.canonicalEntry());
        assertEquals("Syn : antésystolie, syndrome de pré-excitation ventriculaire, WPW", reference.evidenceText());
    }

    @Test
    void resolvesAnOcrDamagedAliasWithoutAConfiguredTermSpecificRule() {
        var indexedEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry(
                "ACJ -Arthrite cluonique juvénile", 59);
        var canonicalEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry(
                "Arthrite chronique juvénile", 59);

        var reference = CodeBioDictionaryEntryPdfToMarkdownTransformer.findContainingEntryReference("""
                1   Arthrite
                Contenu.
                1   Arthrite chronique juvénile
                Syn: ACJ (abréviation), polyarthrite chronique de l'enfant
                DÉFINITION : Contenu canonique.
                """, indexedEntry, List.of(indexedEntry,
                new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Arthrite", 59),
                canonicalEntry)).orElseThrow();

        assertEquals(canonicalEntry, reference.canonicalEntry());
        assertEquals("Syn: ACJ (abréviation), polyarthrite chronique de l'enfant", reference.evidenceText());
    }

    @Test
    void resolvesAFuzzyIndexedTermFromAnExplicitSynonymLine() {
        var indexedEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry(
                "Adénocarcirlome intra-canalaire", 248);
        var canonicalEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry(
                "Epithélioma canalaire in situ", 248);

        var reference = CodeBioDictionaryEntryPdfToMarkdownTransformer.findContainingEntryReference("""
                1   Epithélioma canalaire in situ
                Syn : adénocarcinome intra-canalaire
                DÉFINITION : Contenu canonique.
                """, indexedEntry, List.of(indexedEntry, canonicalEntry)).orElseThrow();

        assertEquals(canonicalEntry, reference.canonicalEntry());
    }

    @Test
    void evaluatesEvidenceBeforeReclassifyingAnOcrLineAsAHeading() {
        var indexedEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry(
                "Adénome hypophysaire (à PRL ~ et GH 71)", 14);
        var canonicalEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry(
                "Adénome hypophysaire", 13);

        var reference = CodeBioDictionaryEntryPdfToMarkdownTransformer.findContainingEntryReference("""
                1   Adénome hypophysaire
                CONFLITS
                Adénome hypophysaire (à PRL ~ et GH ?I)
                (CA - EC) - contenu.
                """, indexedEntry, List.of(indexedEntry, canonicalEntry)).orElseThrow();

        assertEquals(canonicalEntry, reference.canonicalEntry());
    }

    @Test
    void formatsAutomaticallyResolvedIndexReferenceWithCanonicalContentForRag() {
        var indexedEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Accès éclamptiques", 228);
        var canonicalEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Eclampsie", 228);
        var reference = new CodeBioDictionaryEntryPdfToMarkdownTransformer.DetectedIndexReference(
                canonicalEntry, "Syn : accès éclamptiques");

        assertEquals("""
                # Accès éclamptiques

                > Type : référence d'index résolue automatiquement ; le terme indexé a été retrouvé dans le contenu d'une entrée canonique au voisinage de la page indexée.
                > Termes de recherche : Accès éclamptiques; Eclampsie.
                > Source de l'index : *Dictionnaire des codes biologies des maladies*, p. 228.
                > Entrée canonique : Eclampsie, p. 228.
                > Statut de vérification : correspondance textuelle contrôlée automatiquement dans la source OCR.

                ## Extrait de correspondance

                > Syn : accès éclamptiques

                ## Contenu de l'entrée canonique

                ## DÉFINITION

                Accident convulsif.
                """, CodeBioDictionaryEntryPdfToMarkdownTransformer.formatDetectedIndexReference(
                indexedEntry, reference, "DÉFINITION : Accident convulsif.", Set.of("definition")));
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
    void resolvesRedirectTargetFromItsIndexedPageWhenOcrChangesTheTitle() {
        var target = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Amyloïdose-", 33);
        var otherPage = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Amyloïdose", 120);

        assertEquals(target, CodeBioDictionaryEntryPdfToMarkdownTransformer.findRedirectTarget(
                List.of(target, otherPage), "Amyloïdose", 33).orElseThrow());
    }

    @Test
    void resolvesRedirectTargetWhenOnlyPunctuationDiffers() {
        var target = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry(
                "Wolff -Parkinson-White (syndrome de)", 692);

        assertEquals(target, CodeBioDictionaryEntryPdfToMarkdownTransformer.findRedirectTarget(
                List.of(target), "Wolff-Parkinson-White (syndrome de)", 692).orElseThrow());
    }

    @Test
    void resolvesTheAntesystolieRedirectAndItsOcrHeading() {
        var indexedEntry = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry(
                "Antésystolie --> Wolff-Parkinson-White (syndrome de)", 692);
        var target = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry(
                "Wolff -Parkinson-White (syndrome de)", 692);

        var synonymRedirect = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry(
                "Syndrome de pré-excitation ventriculaire --> Wolff-Parkinson-White (syndrome de)", 692);
        var aliasTarget = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry(
                "WPW -Wolff -Parkinson-White (syndrome de)", 692);

        assertEquals(target, CodeBioDictionaryEntryPdfToMarkdownTransformer.findIndexRedirect(
                List.of(indexedEntry, synonymRedirect, target, aliasTarget), indexedEntry)
                .orElseThrow().canonicalEntry());
        assertTrue(CodeBioDictionaryEntryPdfToMarkdownTransformer.headingMatchesTitle(
                "j Wolff -Parkinson-White (syndrome de)", target.title()));
    }

    @Test
    void rejectsAmbiguousFuzzyRedirectTargetsOnTheIndexedPage() {
        var first = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Pied ( os du)", 515);
        var second = new CodeBioDictionaryEntryPdfToMarkdownTransformer.IndexedEntry("Pied (os du) anatomie", 515);

        assertTrue(CodeBioDictionaryEntryPdfToMarkdownTransformer.findRedirectTarget(
                List.of(first, second), "Pied (os du)", 515).isEmpty());
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
