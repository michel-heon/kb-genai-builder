package net.cotechnoe.kb.genai.cli;

import picocli.CommandLine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PdfToMarkdownCommandTest {

    @Test
    void displaysSubcommandHelpWithoutRequiringSource() {
        CommandLine commandLine = new CommandLine(new PdfToMarkdownCommand())
            .addSubcommand(new TransformPdfCommand(path -> null));

        int exitCode = commandLine.execute("transform-pdf", "--help");

        assertEquals(0, exitCode);
    }
}