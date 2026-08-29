package net.cotechnoe.kb.genai.cli;

import net.cotechnoe.kb.genai.document.langchain4j.LangChain4jPdfToMarkdownTransformer;
import picocli.CommandLine;

@CommandLine.Command(
    name = "kb-genai-builder",
    mixinStandardHelpOptions = true,
    description = "Utilitaires de transformation documentaire.")
public final class PdfToMarkdownCommand {

    PdfToMarkdownCommand() {
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new PdfToMarkdownCommand())
                .addSubcommand(new TransformPdfCommand(new LangChain4jPdfToMarkdownTransformer()))
                .addSubcommand(new ExtractNeosanteRelevantArticlesCommand())
                .execute(args);
        System.exit(exitCode);
    }
}