package net.cotechnoe.kb.genai.document.neosante.llm;

import net.cotechnoe.kb.genai.document.neosante.ArticleCandidate;
import net.cotechnoe.kb.genai.document.neosante.ArticleSelection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeosanteLlmRelevantArticleSelectorTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void parsesRelevantStructuredResponseFromModel() {
        NeosanteLlmRelevantArticleSelector selector = new NeosanteLlmRelevantArticleSelector(
                ignored -> "DECISION: RELEVANT\nTITLE: Le sens biologique\nTOPICS: biologie totale, décodage biologique");

        ArticleSelection selection = selector.select(candidate());

        assertTrue(selection.relevant());
        assertEquals("Le sens biologique", selection.title());
        assertEquals(2, selection.topics().size());
    }

    @Test
    void rejectsAClassifiedNonRelevantCandidate() {
        NeosanteLlmRelevantArticleSelector selector = new NeosanteLlmRelevantArticleSelector(ignored -> "DECISION: NOT_RELEVANT");

        ArticleSelection selection = selector.select(candidate());

        assertFalse(selection.relevant());
    }

    private ArticleCandidate candidate() {
        return new ArticleCandidate(temporaryDirectory.resolve("revue.pdf"), 1, 1, "texte de revue");
    }
}
