package net.cotechnoe.kb.genai.document.neosante;

import java.nio.file.Path;
import java.util.Objects;

/** Emits concise, secret-free execution traces for the Néosanté extraction workflow. */
public interface NeosanteExtractionTrace {

    NeosanteExtractionTrace SILENT = new NeosanteExtractionTrace() {
    };

    default void extractionStarted(Path source, int pageCount) {
    }

    default void pageClassified(int page, boolean relevant) {
    }

    default void articleSelected(ArticleCandidate candidate) {
    }

    default void extractionCompleted(int selectedCount) {
    }

    static NeosanteExtractionTrace standardOutput() {
        return new NeosanteExtractionTrace() {
            @Override
            public void extractionStarted(Path source, int pageCount) {
                System.out.printf("[Néosanté] Source : %s (%d pages)%n", source.getFileName(), pageCount);
            }

            @Override
            public void pageClassified(int page, boolean relevant) {
                System.out.printf("[Néosanté] Page %d : %s%n", page, relevant ? "retenue" : "ignorée");
            }

            @Override
            public void articleSelected(ArticleCandidate candidate) {
                System.out.printf("[Néosanté] Article retenu : pages %d-%d%n",
                        candidate.startPage(), candidate.endPage());
            }

            @Override
            public void extractionCompleted(int selectedCount) {
                System.out.printf("[Néosanté] Extraction terminée : %d article(s) retenu(s)%n", selectedCount);
            }
        };
    }

    static NeosanteExtractionTrace requireNonNull(NeosanteExtractionTrace trace) {
        return Objects.requireNonNull(trace, "trace must not be null");
    }
}
