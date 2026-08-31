package net.cotechnoe.kb.genai.cli;

import picocli.CommandLine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PdfToMarkdownCommandTest {

    @Test
    void displaysSubcommandHelpWithoutRequiringSource() {
        CommandLine commandLine = new CommandLine(new PdfToMarkdownCommand())
            .addSubcommand(new TransformPdfCommand(path -> null))
            .addSubcommand(new TransformCodeBioTableOfContentsCommand(path -> null));

        int exitCode = commandLine.execute("transform-pdf", "--help");

        assertEquals(0, exitCode);
    }

    @Test
    void displaysCodeBioSubcommandHelpWithoutRequiringSource() {
        CommandLine commandLine = new CommandLine(new PdfToMarkdownCommand())
            .addSubcommand(new TransformCodeBioTableOfContentsCommand(path -> null));

        int exitCode = commandLine.execute("transform-codebio-table-of-contents", "--help");

        assertEquals(0, exitCode);
    }
}