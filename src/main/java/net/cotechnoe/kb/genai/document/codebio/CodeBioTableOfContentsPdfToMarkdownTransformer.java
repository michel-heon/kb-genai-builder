package net.cotechnoe.kb.genai.document.codebio;

import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import net.cotechnoe.kb.genai.document.MarkdownDocument;
import net.cotechnoe.kb.genai.document.PdfToMarkdownTransformer;
import net.cotechnoe.kb.genai.document.PdfTransformationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formats the CodeBio table of contents as a Markdown index.
 *
 * <p>This transformer is intentionally specific to the OCR layout of the CodeBio table of
 * contents. It does not attempt to correct OCR text: entries without a reliably detected page
 * number are retained in a separate section.</p>
 */
public final class CodeBioTableOfContentsPdfToMarkdownTransformer implements PdfToMarkdownTransformer {

    private static final Pattern DOT_LEADERS = Pattern.compile("\\.{3,}");
    private static final Pattern TRAILING_PAGE = Pattern.compile("^(.*?)\\s+(\\d+)\\s*[.,;:']*$");
    private static final Pattern TABLE_OF_CONTENTS_HEADER = Pattern.compile(
            "(?iu).*bles des .{0,3}ti.res.*");

    @Override
    public MarkdownDocument transform(Path pdf) {
        Path source = validate(pdf);
        try (InputStream input = Files.newInputStream(source)) {
            String text = new ApachePdfBoxDocumentParser().parse(input).text();
            return new MarkdownDocument(source, format(text));
        } catch (IOException | RuntimeException exception) {
            throw new PdfTransformationException("Impossible de transformer la table des matières CodeBio : " + source, exception);
        }
    }

    static String format(String text) {
        List<TableOfContentsEntry> entries = new ArrayList<>();
        List<String> fragments = new ArrayList<>();

        for (String line : text.replace("\r\n", "\n").replace('\r', '\n').lines().toList()) {
            String cleaned = cleanLine(line);
            if (cleaned.isBlank() || TABLE_OF_CONTENTS_HEADER.matcher(cleaned).matches()) {
                continue;
            }

            Matcher page = TRAILING_PAGE.matcher(cleaned);
            if (page.matches() && hasReadableTitle(page.group(1))) {
                entries.add(new TableOfContentsEntry(cleanTitle(page.group(1)), page.group(2)));
            } else {
                fragments.add(cleaned);
            }
        }

        Map<String, List<TableOfContentsEntry>> entriesByInitial = new TreeMap<>();
        for (TableOfContentsEntry entry : entries) {
            entriesByInitial.computeIfAbsent(initialOf(entry.title()), ignored -> new ArrayList<>()).add(entry);
        }

        StringBuilder markdown = new StringBuilder("# Table des matières — Dictionnaire CodeBio\n\n")
                .append("> Transcription OCR structurée automatiquement. Les fragments ambigus sont conservés sans correction interprétative.\n");
        for (Map.Entry<String, List<TableOfContentsEntry>> section : entriesByInitial.entrySet()) {
            markdown.append("\n## ").append(section.getKey()).append("\n\n");
            for (TableOfContentsEntry entry : section.getValue()) {
                markdown.append("- ")
                        .append(entry.title().replace("|", "\\|"))
                        .append(" — p. ")
                        .append(entry.page())
                        .append("\n");
            }
        }

        if (!fragments.isEmpty()) {
            markdown.append("\n<details>\n<summary>Fragments OCR non structurés (")
                    .append(fragments.size())
                    .append(")</summary>\n\n")
                    .append("Ces lignes ne peuvent pas être associées de manière fiable à une entrée et à une page.\n\n");
            for (String fragment : fragments) {
                markdown.append("- `")
                        .append(fragment.replace("`", "'"))
                        .append("`\n");
            }
            markdown.append("\n</details>\n");
        }
        return markdown.append('\n').toString();
    }

    private static String initialOf(String title) {
        return Normalizer.normalize(title.replace("Œ", "OE").replace("œ", "oe"), Normalizer.Form.NFD)
                .codePoints()
                .filter(Character::isLetter)
                .mapToObj(codePoint -> String.valueOf((char) Character.toUpperCase(codePoint)))
                .findFirst()
                .orElse("Autres");
    }

    private static Path validate(Path pdf) {
        Path source = Objects.requireNonNull(pdf, "pdf must not be null").toAbsolutePath().normalize();
        if (!Files.isRegularFile(source)) {
            throw new PdfTransformationException("PDF source must be a readable file: " + source);
        }
        if (!source.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new PdfTransformationException("PDF source must use the .pdf extension: " + source);
        }
        return source;
    }

    private static String cleanLine(String line) {
        return DOT_LEADERS.matcher(line)
                .replaceAll(" ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean hasReadableTitle(String title) {
        String cleaned = cleanTitle(title);
        return cleaned.length() > 1
                && Character.isUpperCase(cleaned.codePointAt(0))
                && cleaned.codePoints().anyMatch(Character::isLetter);
    }

    private static String cleanTitle(String title) {
        return title.replaceAll("^[\\p{Punct}\\s]+", "")
                .replaceAll("[\\p{Punct}&&[^)'’+*/=-]]+$", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record TableOfContentsEntry(String title, String page) {
    }
}
