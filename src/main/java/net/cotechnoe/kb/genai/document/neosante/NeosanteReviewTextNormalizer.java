package net.cotechnoe.kb.genai.document.neosante;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Normalizes PDF extraction artifacts that are specific to Néosanté review transcription. */
public final class NeosanteReviewTextNormalizer {

    private static final Pattern WORD_SPLIT_AT_LINE_END =
            Pattern.compile("(?<=\\p{L})[-\\u00AD\\u2010\\u2011]\\h*\\R\\h*(?=\\p{L})");
    private static final Pattern SENTENCE_TERMINATOR = Pattern.compile(".*[.!?…]\\h*$");
    private static final Pattern EDITORIAL_TITLE = Pattern.compile("(?i)^non\\h+à\\h+la\\h+terreur\\h+secticide\\h*!$");
    private static final Pattern PARAGRAPH_START = Pattern.compile(
            "^(?:Ainsi|Vu|Une|Pour|Tout|Il|Sous|Comme|Si|Mais|En|Par|Cette|Ce|Deux|Sans|Finalement|Physicien|Son)\\b.*");
    private static final Pattern INDENTED_PARAGRAPH = Pattern.compile("^\\h{2,}\\S.*");
    private static final Pattern PAGE_NUMBER = Pattern.compile("^\\d{1,4}$");
    private static final Pattern PDF_PRODUCTION_MARKER = Pattern.compile("^Néosanté\\d*\\.indd\\b.*$|.*\\bwww\\.neosante\\.eu\\b.*");

    private NeosanteReviewTextNormalizer() {
    }

    /**
     * Rejoins words split by the source layout, restores paragraphs, and marks review headings in Markdown.
     */
    public static String normalize(String extractedText) {
        Objects.requireNonNull(extractedText, "extractedText must not be null");
        String unixLineEndings = extractedText.replace("\r\n", "\n").replace('\r', '\n');
        String dehyphenated = WORD_SPLIT_AT_LINE_END.matcher(unixLineEndings).replaceAll("");
        String withoutProductionMarkers = dehyphenated.lines()
                .filter(line -> !PDF_PRODUCTION_MARKER.matcher(line.strip()).matches())
                .collect(java.util.stream.Collectors.joining("\n"));
        return formatEditorialTitle(formatInterviewQuestions(formatSectionHeadings(restoreParagraphBreaks(withoutProductionMarkers))));
    }

    private static String restoreParagraphBreaks(String text) {
        List<String> lines = text.lines().toList();
        List<String> formatted = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.strip();
            if (!formatted.isEmpty() && !trimmed.isEmpty()
                    && (PARAGRAPH_START.matcher(trimmed).matches() || INDENTED_PARAGRAPH.matcher(line).matches())
                    && !formatted.getLast().isBlank()) {
                formatted.add("");
            }
            formatted.add(line);
        }
        return String.join("\n", formatted);
    }

    private static String formatEditorialTitle(String text) {
        return text.lines()
                .map(line -> EDITORIAL_TITLE.matcher(line.strip()).matches() ? "# " + line.strip() : line)
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private static String formatSectionHeadings(String text) {
        List<String> lines = text.lines().toList();
        List<String> formatted = new ArrayList<>();
        for (int index = 0; index < lines.size();) {
            String line = lines.get(index);
            if (PAGE_NUMBER.matcher(line.strip()).matches() && index + 1 < lines.size() && isSectionHeading(lines.get(index + 1))) {
                index++;
                continue;
            }
            if (!isSectionHeading(line)) {
                formatted.add(line);
                index++;
                continue;
            }
            StringBuilder heading = new StringBuilder(line.strip());
            index++;
            while (index < lines.size() && isSectionHeading(lines.get(index))) {
                heading.append(' ').append(lines.get(index).strip());
                index++;
            }
            if (!formatted.isEmpty() && !formatted.getLast().isBlank()) {
                formatted.add("");
            }
            formatted.add("## " + heading);
            formatted.add("");
        }
        return String.join("\n", formatted);
    }

    private static boolean isSectionHeading(String line) {
        String text = line.strip();
        if (text.isEmpty() || text.startsWith("#")) {
            return false;
        }
        boolean containsLetter = false;
        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index);
            if (Character.isLetter(codePoint)) {
                containsLetter = true;
                if (Character.isLowerCase(codePoint)) {
                    return false;
                }
            }
            index += Character.charCount(codePoint);
        }
        return containsLetter;
    }

    private static String formatInterviewQuestions(String text) {
        List<String> lines = text.lines().toList();
        List<String> formatted = new ArrayList<>();
        for (int index = 0; index < lines.size();) {
            Question question = questionAt(lines, index);
            if (question == null) {
                formatted.add(lines.get(index));
                index++;
            } else {
                formatted.add("## " + question.text());
                index = question.lastLineIndex() + 1;
            }
        }
        return String.join("\n", formatted);
    }

    private static Question questionAt(List<String> lines, int firstLineIndex) {
        String firstLine = lines.get(firstLineIndex).strip();
        if (firstLine.isEmpty() || !Character.isUpperCase(firstLine.codePointAt(0))) {
            return null;
        }
        StringBuilder text = new StringBuilder(firstLine);
        for (int lastLineIndex = firstLineIndex; lastLineIndex < Math.min(lines.size(), firstLineIndex + 3); lastLineIndex++) {
            if (lastLineIndex > firstLineIndex) {
                String line = lines.get(lastLineIndex).strip();
                if (line.isEmpty() || SENTENCE_TERMINATOR.matcher(text).matches()) {
                    return null;
                }
                text.append(' ').append(line);
            }
            String candidate = text.toString();
            if (candidate.endsWith("?") && wordCount(candidate) <= 40) {
                return new Question(candidate, lastLineIndex);
            }
        }
        return null;
    }

    private static int wordCount(String text) {
        return text.isBlank() ? 0 : text.trim().split("\\s+").length;
    }

    private record Question(String text, int lastLineIndex) {
    }
}
