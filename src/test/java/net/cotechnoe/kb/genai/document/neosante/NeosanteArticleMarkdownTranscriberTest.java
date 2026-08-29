package net.cotechnoe.kb.genai.document.neosante;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NeosanteArticleMarkdownTranscriberTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void preservesArticleContentAndProvenanceInMarkdown() {
        Path source = temporaryDirectory.resolve("revue.pdf");
        SelectedArticle article = new SelectedArticle(
                new ArticleCandidate(source, 3, 3, "Texte source"),
                new ArticleSelection(true, "Un article", List.of("biologie totale"), "test"));

        String markdown = new NeosanteArticleMarkdownTranscriber().transcribe(article);

        assertEquals("""
                ---
                source: revue.pdf
                pages: 3-3
                topics: "biologie totale"
                selection: "test"
                ---

                # Un article

                Texte source
                """, markdown);
    }

    @Test
    void rejoinsWordsSplitByTheReviewPageLayout() {
        Path source = temporaryDirectory.resolve("revue.pdf");
        SelectedArticle article = new SelectedArticle(
                new ArticleCandidate(source, 46, 46, "le système parasympa-\nthique et des mo-\nments.\n\nUne phrase complète."),
                new ArticleSelection(true, "Un article", List.of("décodage biologique"), "test"));

        String markdown = new NeosanteArticleMarkdownTranscriber().transcribe(article);

        assertEquals("""
                ---
                source: revue.pdf
                pages: 46-46
                topics: "décodage biologique"
                selection: "test"
                ---

                # Un article

                le système parasympathique et des moments.

                Une phrase complète.
                """, markdown);
    }

    @Test
    void formatsWrappedInterviewQuestionsAsSectionHeadings() {
        Path source = temporaryDirectory.resolve("revue.pdf");
        SelectedArticle article = new SelectedArticle(
                new ArticleCandidate(source, 10, 10, "Quelles institutions exactement ?\nRéponse.\n\nPour vous, les lois biologiques sont bien davantage que des hy-\npothèses ?\nRéponse suivante."),
                new ArticleSelection(true, "Un article", List.of("biologie totale"), "test"));

        String markdown = new NeosanteArticleMarkdownTranscriber().transcribe(article);

        assertEquals("""
                ---
                source: revue.pdf
                pages: 10-10
                topics: "biologie totale"
                selection: "test"
                ---

                # Un article

                ## Quelles institutions exactement ?
                Réponse.

                ## Pour vous, les lois biologiques sont bien davantage que des hypothèses ?
                Réponse suivante.
                """, markdown);
    }

    @Test
    void formatsEditorialTitleAndParagraphs() {
        Path source = temporaryDirectory.resolve("revue.pdf");
        SelectedArticle article = new SelectedArticle(
                new ArticleCandidate(source, 3, 3, "éDITO\nNON à la Terreur secTIcIDe !\nAinsi donc, la menace est réelle.\nVu de Belgique, tout ce ramdam paraît surréaliste.\nUne anecdote l’illustre."),
                new ArticleSelection(true, "Un article", List.of("biologie totale"), "test"));

        String markdown = new NeosanteArticleMarkdownTranscriber().transcribe(article);

        assertEquals("""
                ---
                source: revue.pdf
                pages: 3-3
                topics: "biologie totale"
                selection: "test"
                ---

                # NON à la Terreur secTIcIDe !

                Ainsi donc, la menace est réelle.

                Vu de Belgique, tout ce ramdam paraît surréaliste.

                Une anecdote l’illustre.
                """, markdown);
    }

    @Test
    void removesPdfProductionMarkers() {
        Path source = temporaryDirectory.resolve("revue.pdf");
        SelectedArticle article = new SelectedArticle(
                new ArticleCandidate(source, 3, 3, "Texte utile\nNéosanté7.indd 45 25/11/11 17:38\nwww.neosante.eu"),
                new ArticleSelection(true, "Un article", List.of("biologie totale"), "test"));

        String markdown = new NeosanteArticleMarkdownTranscriber().transcribe(article);

        assertEquals("""
                ---
                source: revue.pdf
                pages: 3-3
                topics: "biologie totale"
                selection: "test"
                ---

                # Un article

                Texte utile
                """, markdown);
    }
}
