package net.cotechnoe.kb.genai.cli;

import net.cotechnoe.kb.genai.document.MarkdownDocument;
import net.cotechnoe.kb.genai.document.PdfToMarkdownTransformer;
import net.cotechnoe.kb.genai.document.codebio.CodeBioTableOfContentsPdfToMarkdownTransformer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
    name = "transform-codebio-table-of-contents",
    mixinStandardHelpOptions = true,
    description = "Transforme la table des matières OCR du dictionnaire CodeBio en index Markdown.")
public final class TransformCodeBioTableOfContentsCommand implements Callable<Integer> {

    private final PdfToMarkdownTransformer transformer;

    @Parameters(index = "0", paramLabel = "SOURCE", description = "PDF de table des matières CodeBio.")
    private Path source;

    @Option(names = {"-o", "--output"}, paramLabel = "FICHIER", description = "Fichier Markdown cible.")
    private Path output;

    public TransformCodeBioTableOfContentsCommand() {
        this(new CodeBioTableOfContentsPdfToMarkdownTransformer());
    }

    TransformCodeBioTableOfContentsCommand(PdfToMarkdownTransformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public Integer call() {
        try {
            MarkdownDocument document = transformer.transform(source);
            Path resolvedOutput = output == null ? defaultOutput(source) : output;
            Path parent = resolvedOutput.toAbsolutePath().normalize().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(resolvedOutput, document.content(), StandardCharsets.UTF_8);
            System.out.println(resolvedOutput.toAbsolutePath().normalize());
            return 0;
        } catch (IOException | RuntimeException exception) {
            System.err.println(exception.getMessage());
            return 1;
        }
    }

    private static Path defaultOutput(Path source) {
        String name = source.getFileName().toString();
        int extension = name.lastIndexOf('.');
        String stem = extension > 0 ? name.substring(0, extension) : name;
        return source.resolveSibling(stem + ".md");
    }
}
