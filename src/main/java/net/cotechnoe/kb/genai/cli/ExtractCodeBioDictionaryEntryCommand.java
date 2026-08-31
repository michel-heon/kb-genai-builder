package net.cotechnoe.kb.genai.cli;

import net.cotechnoe.kb.genai.document.MarkdownDocument;
import net.cotechnoe.kb.genai.document.codebio.CodeBioDictionaryEntryPdfToMarkdownTransformer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
    name = "extract-codebio-entry",
    mixinStandardHelpOptions = true,
    description = "Extrait une entrée du dictionnaire CodeBio à partir de sa table des matières Markdown.")
public final class ExtractCodeBioDictionaryEntryCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "DICTIONNAIRE", description = "PDF du dictionnaire CodeBio.")
    private Path dictionary;

    @Parameters(index = "1", paramLabel = "TERME", description = "Terme à extraire, par exemple Amylase.")
    private String term;

    @Option(names = {"-t", "--table-of-contents"}, required = true, paramLabel = "FICHIER",
            description = "Table des matières CodeBio au format Markdown.")
    private Path tableOfContents;

    @Option(names = {"-o", "--output"}, paramLabel = "FICHIER", description = "Fichier Markdown cible.")
    private Path output;

    @Option(names = {"-s", "--structure"}, paramLabel = "FICHIER",
            description = "Référentiel de structure CodeBio versionné (optionnel).")
    private Path structure;

    @Option(names = "--index-resolutions", paramLabel = "FICHIER",
            description = "Registre des renvois CodeBio vérifiés (optionnel).")
    private Path indexResolutions;

    @Option(names = "--failure-list", paramLabel = "FICHIER",
            description = "Journal Markdown des échecs d'extraction (optionnel).")
    private Path failureList;

    @Override
    public Integer call() {
        try {
            CodeBioDictionaryEntryPdfToMarkdownTransformer transformer =
                    new CodeBioDictionaryEntryPdfToMarkdownTransformer();
            MarkdownDocument document = transformer.transform(
                    dictionary, tableOfContents, term, structure == null ? null : structure, indexResolutions);
            Path resolvedOutput = output == null ? defaultOutput(dictionary, term) : output;
            Path parent = resolvedOutput.toAbsolutePath().normalize().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(resolvedOutput, document.content(), StandardCharsets.UTF_8);
            System.out.println(resolvedOutput.toAbsolutePath().normalize());
            return 0;
        } catch (IOException | RuntimeException exception) {
            logFailure(exception);
            System.err.println(exception.getMessage());
            return 1;
        }
    }

    private void logFailure(Exception exception) {
        if (failureList == null) {
            return;
        }
        Path target = failureList.toAbsolutePath().normalize();
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        String entry = "- **" + term.trim() + "** — " + message.replace('\n', ' ') + "\n";
        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(target)) {
                Files.writeString(target, "# Échecs d'extraction CodeBio\n\n", StandardCharsets.UTF_8);
            }
            Files.writeString(target, entry, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException loggingException) {
            exception.addSuppressed(loggingException);
        }
    }

    private static Path defaultOutput(Path dictionary, String term) {
        return dictionary.resolveSibling(safeFileName(term) + ".md");
    }

    private static String safeFileName(String term) {
        return term.trim().replaceAll("[\\\\/:*?\"<>|]", "-");
    }
}
