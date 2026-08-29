package net.cotechnoe.kb.genai.cli;

import net.cotechnoe.kb.genai.document.neosante.NeosanteArticleMarkdownTranscriber;
import net.cotechnoe.kb.genai.document.neosante.NeosanteExtractionTrace;
import net.cotechnoe.kb.genai.document.neosante.NeosanteReviewArticleExtractor;
import net.cotechnoe.kb.genai.document.neosante.SelectedArticle;
import net.cotechnoe.kb.genai.document.neosante.langchain4j.LangChain4jNeosanteArticleClassificationModel;
import net.cotechnoe.kb.genai.document.neosante.langchain4j.LangChain4jNeosanteReviewArticleExtractor;
import net.cotechnoe.kb.genai.document.neosante.llm.NeosanteLlmRelevantArticleSelector;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

@Command(
    name = "extract-neosante-relevant-articles",
    mixinStandardHelpOptions = true,
    description = "Extrait des revues Néosanté les pages pertinentes sur la biologie totale ou le décodage biologique.")
public final class ExtractNeosanteRelevantArticlesCommand implements Callable<Integer> {

    private static final String CONFIGURATION_PROPERTY = "kb.genai.builder.java.configuration";

    @Parameters(index = "0", paramLabel = "SOURCE", description = "Revue PDF source.")
    private Path source;

    @Option(names = {"-o", "--output-directory"}, required = true, paramLabel = "RÉPERTOIRE",
            description = "Répertoire qui recevra les articles Markdown.")
    private Path outputDirectory;

    @Override
    public Integer call() {
        try {
            Properties configuration = loadConfiguration();
            NeosanteReviewArticleExtractor extractor = new LangChain4jNeosanteReviewArticleExtractor(
                    new NeosanteLlmRelevantArticleSelector(LangChain4jNeosanteArticleClassificationModel.from(configuration)),
                    NeosanteExtractionTrace.standardOutput());
            List<SelectedArticle> articles = extractor.extract(source);
            System.out.println("[Néosanté] Écriture des fichiers Markdown dans : " + outputDirectory.toAbsolutePath().normalize());
            write(articles, outputDirectory);
            Files.writeString(outputDirectory.resolve(".complete"), "selected=" + articles.size() + "\n",
                    StandardCharsets.UTF_8);
            System.out.println("Articles extraits : " + articles.size());
            return 0;
        } catch (IOException | RuntimeException exception) {
            System.err.println(message(exception));
            return 1;
        }
    }

    private static String message(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String detail = cause.getMessage();
        if (detail == null || detail.isBlank()) {
            return exception.getMessage();
        }
        return exception.getMessage() + " (cause: " + detail + ")";
    }

    private static Properties loadConfiguration() throws IOException {
        Path configuration = Path.of(System.getProperty(CONFIGURATION_PROPERTY, "env/generated/java.properties"));
        if (!Files.isRegularFile(configuration)) {
            throw new IOException("Configuration Java absente : " + configuration + "; exécutez make bootstrap neosante.");
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(configuration)) {
            properties.load(input);
        }
        return properties;
    }

    private static void write(List<SelectedArticle> articles, Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        NeosanteArticleMarkdownTranscriber transcriber = new NeosanteArticleMarkdownTranscriber();
        for (SelectedArticle article : articles) {
            String filename = "pages-" + article.candidate().startPage() + "-" + article.candidate().endPage() + ".md";
            Files.writeString(outputDirectory.resolve(filename), transcriber.transcribe(article), StandardCharsets.UTF_8);
        }
    }
}
