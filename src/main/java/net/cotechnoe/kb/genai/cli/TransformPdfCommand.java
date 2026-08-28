package net.cotechnoe.kb.genai.cli;

import net.cotechnoe.kb.genai.document.MarkdownDocument;
import net.cotechnoe.kb.genai.document.PdfToMarkdownTransformer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.Callable;

@Command(
    name = "transform-pdf",
    mixinStandardHelpOptions = true,
    description = "Transforme un PDF local en Markdown.")
public final class TransformPdfCommand implements Callable<Integer> {

    private final PdfToMarkdownTransformer transformer;

    @Parameters(index = "0", paramLabel = "SOURCE", description = "PDF source.")
    private Path source;

    @Option(names = {"-o", "--output"}, paramLabel = "FICHIER", description = "Fichier Markdown cible.")
    private Path output;

    public TransformPdfCommand(PdfToMarkdownTransformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public Integer call() {
        try {
            validateGeneratedJavaConfiguration();
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

    private static void validateGeneratedJavaConfiguration() throws IOException {
        Path configuration = Path.of(System.getProperty(
                "kb.genai.builder.java.configuration",
                "env/generated/java.properties"));
        if (!Files.isRegularFile(configuration)) {
            throw new IOException("Configuration Java absente : " + configuration + "; exécutez make bootstrap.");
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(configuration)) {
            properties.load(input);
        }

        String configuredVersion = properties.getProperty("kb.genai.builder.java.version");
        if (configuredVersion == null || configuredVersion.isBlank()) {
            throw new IOException("Propriété Java absente : kb.genai.builder.java.version");
        }
        if (!configuredVersion.equals(System.getProperty("java.version"))) {
            throw new IOException("Version Java incompatible avec la configuration générée; exécutez make bootstrap.");
        }
    }
}