package net.cotechnoe.kb.genai.document.codebio;

import net.cotechnoe.kb.genai.document.MarkdownDocument;
import net.cotechnoe.kb.genai.document.PdfTransformationException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts one CodeBio dictionary entry by using its dedicated Markdown table of contents.
 *
 * <p>CodeBio's printed page numbering starts 105 pages after the PDF page numbering. The
 * transformer confirms the requested heading and, when necessary, searches later physical pages.
 * It retains all text until the next indexed entry begins, including any continuation pages.</p>
 */
public final class CodeBioDictionaryEntryPdfToMarkdownTransformer {

    private static final int PDF_PAGE_OFFSET = 105;
    private static final Pattern TABLE_OF_CONTENTS_ENTRY = Pattern.compile(
            "(?m)^-\\s+(.+?)\\s+—\\s+p\\.\\s+(\\d+)\\s*$");
    private static final Pattern LINE = Pattern.compile("(?m)^.*$");
    private static final Pattern ALIAS_AND_TARGET = Pattern.compile(
            "^([\\p{L}\\p{N}]{2,10})\\s*[-=]+\\s*(.+)$");
    private static final Pattern FOOTER = Pattern.compile("^\\s*\\d+\\s*$");
    private static final Pattern SECTION_HEADING = Pattern.compile("^[A-ZÀ-ÖØ-Þ][A-ZÀ-ÖØ-Þ ]{2,}:?$");
    private static final Pattern INLINE_SECTION_HEADING = Pattern.compile(
            "^([A-ZÀ-ÖØ-Þ][A-ZÀ-ÖØ-Þ ]{2,})\\s*:\\s+(.+)$");
    private static final Pattern RESOLUTION_INDEXED_TERM = Pattern.compile("resolution\\.(\\d+)\\.indexed_term");
    private static final Pattern INDEX_REDIRECT = Pattern.compile("^(.+?)\\s*-+\\s*(?:>|\\+)\\s*(.+)$");
    private static final String MANUALLY_VERIFIED = "manually_verified";
    private static final Map<String, String> SECTION_LABEL_CORRECTIONS = Map.of(
            "deftnition", "DÉFINITION");
    private static final Set<String> DEFAULT_SECTION_TYPES = Set.of(
            "definition", "causes", "symptomes", "conflits", "fonction", "ethologie", "mots", "verbes", "remedes");

    public MarkdownDocument transform(Path dictionaryPdf, Path tableOfContentsMarkdown, String term) {
        return transform(dictionaryPdf, tableOfContentsMarkdown, term, DEFAULT_SECTION_TYPES, null);
    }

    public MarkdownDocument transform(Path dictionaryPdf, Path tableOfContentsMarkdown, String term, Path structure) {
        return transform(dictionaryPdf, tableOfContentsMarkdown, term, readSectionTypes(structure), null);
    }

    public MarkdownDocument transform(Path dictionaryPdf, Path tableOfContentsMarkdown, String term, Path structure,
            Path indexResolutions) {
        return transform(dictionaryPdf, tableOfContentsMarkdown, term,
                structure == null ? DEFAULT_SECTION_TYPES : readSectionTypes(structure), indexResolutions);
    }

    private MarkdownDocument transform(Path dictionaryPdf, Path tableOfContentsMarkdown, String term, Set<String> sectionTypes,
            Path indexResolutions) {
        Path dictionary = validatePdf(dictionaryPdf);
        Path tableOfContents = validateMarkdown(tableOfContentsMarkdown);
        String requestedTerm = requireTerm(term);

        try {
            List<IndexedEntry> entries = parseTableOfContents(Files.readString(tableOfContents));
            IndexedEntry requestedEntry = findEntry(entries, requestedTerm);
            Optional<IndexResolution> resolution = findResolution(indexResolutions, requestedTerm);
            Optional<IndexRedirect> indexRedirect = resolution.isEmpty()
                    ? findIndexRedirect(entries, requestedEntry)
                    : Optional.empty();
            IndexedEntry sourceEntry = resolution.map(value -> findEntry(entries, value.canonicalEntry()))
                    .orElseGet(() -> indexRedirect.map(IndexRedirect::canonicalEntry).orElse(requestedEntry));
            try (PDDocument document = Loader.loadPDF(dictionary.toFile())) {
                try {
                    String entryText = extractEntry(
                            document, sourceEntry.page() + PDF_PAGE_OFFSET, sourceEntry.title(), entries);
                    if (resolution.isPresent()) {
                        return new MarkdownDocument(dictionary,
                                formatReference(requestedEntry, sourceEntry, entryText, resolution.orElseThrow()));
                    }
                    if (indexRedirect.isPresent()) {
                        return new MarkdownDocument(dictionary,
                                formatIndexRedirectReference(requestedEntry, sourceEntry, entryText, sectionTypes));
                    }
                    return new MarkdownDocument(dictionary, format(requestedEntry, entryText, sectionTypes));
                } catch (PdfTransformationException exception) {
                    if (resolution.isEmpty() && isMissingAutonomousHeading(exception, requestedEntry.title())) {
                        Optional<DetectedIndexReference> detectedReference = findContainingEntryReference(
                                document, requestedEntry, entries);
                        if (detectedReference.isPresent()) {
                            DetectedIndexReference reference = detectedReference.orElseThrow();
                            IndexedEntry canonicalEntry = reference.canonicalEntry();
                            String canonicalText = extractEntry(document,
                                    canonicalEntry.page() + PDF_PAGE_OFFSET,
                                    canonicalEntry.title(), entries);
                            return new MarkdownDocument(dictionary, formatDetectedIndexReference(
                                    requestedEntry, reference, canonicalText, sectionTypes));
                        }
                        return new MarkdownDocument(dictionary, formatUnresolvedIndexReference(requestedEntry));
                    }
                    throw exception;
                }
            }
        } catch (IOException exception) {
            throw new PdfTransformationException("Impossible d'extraire l'entrée CodeBio : " + requestedTerm, exception);
        }
    }

    static List<IndexedEntry> parseTableOfContents(String markdown) {
        List<IndexedEntry> entries = new ArrayList<>();
        Matcher matcher = TABLE_OF_CONTENTS_ENTRY.matcher(markdown);
        while (matcher.find()) {
            entries.add(new IndexedEntry(matcher.group(1).replace("\\|", "|").trim(), Integer.parseInt(matcher.group(2))));
        }
        if (entries.isEmpty()) {
            throw new PdfTransformationException("Aucune entrée CodeBio exploitable dans la table des matières Markdown.");
        }
        return entries;
    }

    static String format(IndexedEntry entry, String entryText) {
        return format(entry, entryText, DEFAULT_SECTION_TYPES);
    }

    static String format(IndexedEntry entry, String entryText, Set<String> sectionTypes) {
        return "# " + entry.title() + "\n\n"
                + "> Source : *Dictionnaire des codes biologies des maladies*, p. " + entry.page() + ".\n\n"
                + formatBody(entryText, sectionTypes) + "\n";
    }

    static String formatReference(IndexedEntry indexedEntry, IndexedEntry canonicalEntry, String canonicalText,
            IndexResolution resolution) {
        if (!containsEvidence(canonicalText, resolution.evidenceText())) {
            throw new PdfTransformationException("La preuve configurée pour le terme CodeBio " + indexedEntry.title()
                    + " est absente de l'entrée canonique : " + canonicalEntry.title());
        }
        return "# " + indexedEntry.title() + "\n\n"
                + "> Type : référence d'index vérifiée ; ce terme n'est pas un en-tête autonome du dictionnaire.\n"
                + "> Source de l'index : *Dictionnaire des codes biologies des maladies*, p. " + indexedEntry.page() + ".\n"
                + "> Entrée canonique : [" + canonicalEntry.title() + "](./" + canonicalEntry.title() + ".md), p. "
                + canonicalEntry.page() + ".\n"
                + "> Statut de vérification : " + resolution.reviewStatus() + ".\n\n"
                + "## Extrait source\n\n"
                + "> " + resolution.evidenceText() + "\n";
    }

    static String formatIndexRedirectReference(IndexedEntry indexedEntry, IndexedEntry canonicalEntry,
            String canonicalText, Set<String> sectionTypes) {
        return "# " + indexedEntry.title() + "\n\n"
                + "> Type : renvoi d'index transcrit ; la relation ci-dessous est explicitement indiquée par la table des matières.\n"
                + "> Termes de recherche : " + indexedEntry.title() + "; " + canonicalEntry.title() + ".\n"
                + "> Source de l'index : *Dictionnaire des codes biologies des maladies*, p. " + indexedEntry.page() + ".\n"
                + "> Entrée cible : [" + canonicalEntry.title() + "](./" + canonicalEntry.title() + ".md), p. "
                + canonicalEntry.page() + ".\n\n"
                + "## Renvoi source\n\n"
                + "> " + indexedEntry.title() + "\n\n"
                + "## Contenu de l'entrée cible\n\n"
                + formatBody(canonicalText, sectionTypes) + "\n";
    }

    static String formatUnresolvedIndexReference(IndexedEntry indexedEntry) {
        return "# " + indexedEntry.title() + "\n\n"
                + "> Type : référence d'index non résolue ; ce terme est présent dans la table des matières mais n'a pas "
                + "été trouvé comme en-tête autonome du dictionnaire.\n"
                + "> Source de l'index : *Dictionnaire des codes biologies des maladies*, p. " + indexedEntry.page() + ".\n"
                + "> Statut de vérification : à rapprocher manuellement d'une entrée canonique avant toute interprétation.\n\n"
                + "## Contenu\n\n"
                + "> Aucun contenu n'est généré afin de ne pas inventer de correspondance ou d'information médicale.\n";
    }

    static String formatDetectedIndexReference(IndexedEntry indexedEntry, DetectedIndexReference reference,
            String canonicalText, Set<String> sectionTypes) {
        IndexedEntry canonicalEntry = reference.canonicalEntry();
        return "# " + indexedEntry.title() + "\n\n"
                + "> Type : référence d'index résolue automatiquement ; le terme indexé a été retrouvé "
                + "dans le contenu d'une entrée canonique au voisinage de la page indexée.\n"
                + "> Termes de recherche : " + indexedEntry.title() + "; " + canonicalEntry.title() + ".\n"
                + "> Source de l'index : *Dictionnaire des codes biologies des maladies*, p. "
                + indexedEntry.page() + ".\n"
                + "> Entrée canonique : " + canonicalEntry.title() + ", p. " + canonicalEntry.page() + ".\n"
                + "> Statut de vérification : correspondance textuelle contrôlée automatiquement dans la source OCR.\n\n"
                + "## Extrait de correspondance\n\n"
                + "> " + reference.evidenceText() + "\n\n"
                + "## Contenu de l'entrée canonique\n\n"
                + formatBody(canonicalText, sectionTypes) + "\n";
    }

    private static boolean isMissingAutonomousHeading(PdfTransformationException exception, String title) {
        return exception.getMessage().equals("Entrée CodeBio introuvable dans le dictionnaire : " + title);
    }

    private static String extractEntry(PDDocument document, int expectedPhysicalPage, String title,
            List<IndexedEntry> entries)
            throws IOException {
        if (expectedPhysicalPage > document.getNumberOfPages()) {
            throw new PdfTransformationException("La page CodeBio demandée est hors du PDF : " + expectedPhysicalPage);
        }

        StringBuilder extracted = new StringBuilder();
        boolean started = false;
        int firstPhysicalPage = Math.max(1, expectedPhysicalPage - 1);
        int lastStartSearchPage = Math.min(document.getNumberOfPages(), expectedPhysicalPage + 1);
        for (int physicalPage = firstPhysicalPage; physicalPage <= document.getNumberOfPages(); physicalPage++) {
            if (!started && physicalPage > lastStartSearchPage) {
                break;
            }
            String pageText = extractPage(document, physicalPage);
            String slice = started ? pageText : contentAfterHeading(pageText, title);
            if (slice == null) {
                continue;
            }
            started = true;
            int nextHeading = indexOfAnyEntryHeading(slice, entries, title);
            extracted.append(nextHeading >= 0 ? slice.substring(0, nextHeading) : slice).append('\n');
            if (nextHeading >= 0) {
                break;
            }
        }
        if (!started) {
            throw new PdfTransformationException("Entrée CodeBio introuvable dans le dictionnaire : " + title);
        }
        return extracted.toString();
    }

    private static String extractPage(PDDocument document, int page) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        return stripper.getText(document).replace("\r\n", "\n").replace('\r', '\n');
    }

    private static String contentAfterHeading(String text, String title) {
        int heading = indexOfHeading(text, title);
        return heading < 0 ? null : text.substring(heading + headingLength(text, heading));
    }

    private static int indexOfHeading(String text, String title) {
        Matcher lines = LINE.matcher(text);
        while (lines.find()) {
            if (headingMatchesTitle(headingCandidate(lines.group()), title)) {
                return lines.start();
            }
        }
        return -1;
    }

    static boolean headingMatchesTitle(String heading, String title) {
        String normalizedHeading = normalizeTitleForComparison(headingCandidate(heading));
        String normalizedTitle = normalizeTitleForComparison(title);
        return normalizedHeading.equals(normalizedTitle)
                || normalizedHeading.matches(Pattern.quote(normalizedTitle)
                        + "(?: [a-z0-9]{1,3}){1,3}");
    }

    private static Optional<DetectedIndexReference> findContainingEntryReference(PDDocument document,
            IndexedEntry indexedEntry, List<IndexedEntry> entries) throws IOException {
        int expectedPhysicalPage = indexedEntry.page() + PDF_PAGE_OFFSET;
        StringBuilder pageWindow = new StringBuilder();
        for (int page = Math.max(1, expectedPhysicalPage - 1);
                page <= Math.min(document.getNumberOfPages(), expectedPhysicalPage + 1); page++) {
            pageWindow.append(extractPage(document, page)).append('\n');
        }
        return findContainingEntryReference(pageWindow.toString(), indexedEntry, entries);
    }

    static Optional<DetectedIndexReference> findContainingEntryReference(String pageText,
            IndexedEntry indexedEntry, List<IndexedEntry> entries) {
        IndexedEntry currentEntry = null;
        for (String line : pageText.lines().toList()) {
            Optional<IndexedEntry> headingEntry = findHeadingEntry(line, indexedEntry.page(), entries);
            boolean explicitHeading = line.trim().matches("^(?:[1j!]\\s+|[\\\\|]+\\s*).+");
            if (headingEntry.isPresent() && explicitHeading) {
                currentEntry = headingEntry.orElseThrow();
                continue;
            }
            if (currentEntry != null && !sameTerm(currentEntry.title(), indexedEntry.title())
                    && matchesIndexedEvidence(line, indexedEntry.title())) {
                return Optional.of(new DetectedIndexReference(currentEntry, line.trim()));
            }
            if (headingEntry.isPresent()) {
                currentEntry = headingEntry.orElseThrow();
                continue;
            }
            Optional<String> pdfHeading = findExplicitPdfHeading(line);
            if (pdfHeading.isPresent()) {
                currentEntry = new IndexedEntry(pdfHeading.orElseThrow(), indexedEntry.page());
            }
        }
        return Optional.empty();
    }

    /**
     * Returns a genuine PDF entry heading that is absent from the structured OCR index. This is
     * deliberately restricted to explicit heading markers so prose cannot become a canonical entry.
     */
    private static Optional<String> findExplicitPdfHeading(String line) {
        String trimmed = line.trim();
        if (!trimmed.matches("^(?:[1j!]\\s+|[\\\\|]+\\s*).+")) {
            return Optional.empty();
        }
        String candidate = headingCandidate(line);
        return looksLikeHeading(line, candidate) ? Optional.of(candidate) : Optional.empty();
    }

    private static boolean containsWholeNormalizedTerm(String text, String term) {
        String normalizedText = " " + normalizeForEvidence(text) + " ";
        String normalizedTerm = " " + normalizeForEvidence(term) + " ";
        return normalizedText.contains(normalizedTerm);
    }

    private static Optional<IndexedEntry> findHeadingEntry(String line, int indexedPage, List<IndexedEntry> entries) {
        String candidate = headingCandidate(line);
        if (!looksLikeHeading(line, candidate)) {
            return Optional.empty();
        }
        IndexedEntry bestEntry = null;
        double bestScore = 0.0;
        for (IndexedEntry entry : entries) {
            if (Math.abs(entry.page() - indexedPage) > 1) {
                continue;
            }
            double score = titleSimilarity(candidate, entry.title());
            if (score > bestScore) {
                bestEntry = entry;
                bestScore = score;
            }
        }
        return bestScore >= 0.92 ? Optional.of(bestEntry) : Optional.empty();
    }

    private static boolean looksLikeHeading(String rawLine, String candidate) {
        String trimmed = rawLine.trim();
        boolean explicitMarker = trimmed.matches("^(?:[1j!]\\s+|[\\\\|]+\\s*).+");
        return !candidate.isBlank() && candidate.length() <= 100
                && (explicitMarker || (!candidate.contains(":") && !candidate.matches(".*[.!?]$")));
    }

    private static boolean matchesIndexedEvidence(String line, String indexedTitle) {
        if (containsWholeNormalizedTerm(line, indexedTitle)) {
            return true;
        }
        Matcher aliasAndTarget = ALIAS_AND_TARGET.matcher(indexedTitle.trim());
        if (aliasAndTarget.matches()) {
            String alias = aliasAndTarget.group(1);
            String normalizedLine = normalizeForEvidence(line);
            if (normalizedLine.startsWith("syn ") && containsWholeNormalizedTerm(line, alias)) {
                return true;
            }
            if (evidenceSimilarity(line, aliasAndTarget.group(2)) >= 0.90) {
                return true;
            }
        }
        return evidenceSimilarity(line, indexedTitle) >= 0.90;
    }

    private static double evidenceSimilarity(String line, String term) {
        double bestScore = 0.0;
        for (String fragment : line.split("[:,;]")) {
            String candidate = fragment.trim();
            if (candidate.length() >= 8) {
                bestScore = Math.max(bestScore, titleSimilarity(candidate, term));
            }
        }
        return bestScore;
    }

    private static int headingLength(String text, int headingStart) {
        int lineEnd = text.indexOf('\n', headingStart);
        return (lineEnd < 0 ? text.length() : lineEnd + 1) - headingStart;
    }

    private static int indexOfAnyEntryHeading(String text, List<IndexedEntry> entries, String currentTitle) {
        Matcher lines = LINE.matcher(text);
        while (lines.find()) {
            String candidate = headingCandidate(lines.group());
            if (!looksLikeHeading(lines.group(), candidate)) {
                continue;
            }
            for (IndexedEntry entry : entries) {
                if (!sameTerm(entry.title(), currentTitle) && headingMatchesTitle(candidate, entry.title())) {
                    return lines.start();
                }
            }
        }
        return -1;
    }

    private static IndexedEntry findEntry(List<IndexedEntry> entries, String term) {
        return entries.stream()
                .filter(entry -> sameTerm(entry.title(), term))
                .findFirst()
                .orElseThrow(() -> new PdfTransformationException("Terme CodeBio absent de la table des matières : " + term));
    }

    static Optional<IndexRedirect> findIndexRedirect(List<IndexedEntry> entries, IndexedEntry indexedEntry) {
        Matcher matcher = INDEX_REDIRECT.matcher(indexedEntry.title());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return findRedirectTarget(entries, matcher.group(2).trim(), indexedEntry.page())
                .map(IndexRedirect::new);
    }

    /**
     * Resolves an explicit index redirect without altering the source transcription. An exact title
     * always wins; otherwise, only one sufficiently similar title at the indexed page (or an
     * adjacent page) is accepted. The page constraint prevents a broad fuzzy match from selecting
     * an unrelated medical entry.
     */
    static Optional<IndexedEntry> findRedirectTarget(List<IndexedEntry> entries, String targetTitle, int indexedPage) {
        List<IndexedEntry> nearbyEntries = entries.stream()
                .filter(entry -> Math.abs(entry.page() - indexedPage) <= 1)
                .toList();
        Optional<IndexedEntry> exact = nearbyEntries.stream()
                .filter(entry -> sameTerm(entry.title(), targetTitle))
                .findFirst();
        if (exact.isPresent()) {
            return exact;
        }

        List<IndexedEntry> candidates = nearbyEntries.stream()
                .filter(entry -> isPlausibleRedirectTarget(entry.title(), targetTitle))
                .toList();
        return candidates.size() == 1 ? Optional.of(candidates.getFirst()) : Optional.empty();
    }

    private static boolean isPlausibleRedirectTarget(String candidate, String target) {
        String normalizedCandidate = normalizeTitleForComparison(candidate);
        String normalizedTarget = normalizeTitleForComparison(target);
        return normalizedCandidate.equals(normalizedTarget)
                || normalizedCandidate.startsWith(normalizedTarget + " ")
                || titleSimilarity(candidate, target) >= 0.90;
    }

    private static boolean sameTerm(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private static String headingCandidate(String line) {
        return line.trim()
                .replaceFirst("^(?:[1j!]\\s+|[\\\\|]+\\s*)", "")
                .trim();
    }

    private static String normalizeTitleForComparison(String value) {
        return normalize(value)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static double titleSimilarity(String left, String right) {
        String normalizedLeft = normalizeTitleForComparison(left);
        String normalizedRight = normalizeTitleForComparison(right);
        if (normalizedLeft.isBlank() || normalizedRight.isBlank()) {
            return 0.0;
        }
        if (normalizedLeft.equals(normalizedRight)) {
            return 1.0;
        }
        int longest = Math.max(normalizedLeft.length(), normalizedRight.length());
        return 1.0 - ((double) levenshteinDistance(normalizedLeft, normalizedRight) / longest);
    }

    private static int levenshteinDistance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int index = 0; index <= right.length(); index++) {
            previous[index] = index;
        }
        for (int leftIndex = 1; leftIndex <= left.length(); leftIndex++) {
            current[0] = leftIndex;
            for (int rightIndex = 1; rightIndex <= right.length(); rightIndex++) {
                int substitution = previous[rightIndex - 1]
                        + (left.charAt(leftIndex - 1) == right.charAt(rightIndex - 1) ? 0 : 1);
                current[rightIndex] = Math.min(
                        Math.min(current[rightIndex - 1] + 1, previous[rightIndex] + 1), substitution);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static String formatBody(String text, Set<String> sectionTypes) {
        StringBuilder markdown = new StringBuilder();
        boolean previousBlank = true;
        for (String line : text.lines().toList()) {
            String cleaned = line.trim();
            if (FOOTER.matcher(cleaned).matches()) {
                continue;
            }
            if (cleaned.isBlank()) {
                if (!previousBlank && !markdown.isEmpty()) {
                    markdown.append('\n');
                }
                previousBlank = true;
                continue;
            }
            Matcher inlineSection = INLINE_SECTION_HEADING.matcher(cleaned);
            if (inlineSection.matches() && isKnownSection(inlineSection.group(1), sectionTypes)) {
                if (!previousBlank && !markdown.isEmpty()) {
                    markdown.append('\n');
                }
                markdown.append("## ").append(canonicalSectionLabel(inlineSection.group(1))).append("\n\n");
                markdown.append(inlineSection.group(2).trim()).append('\n');
                previousBlank = false;
                continue;
            }
            if (SECTION_HEADING.matcher(cleaned).matches()
                    && isKnownSection(cleaned.replaceFirst(":$", ""), sectionTypes)) {
                if (!previousBlank && !markdown.isEmpty()) {
                    markdown.append('\n');
                }
                markdown.append("## ").append(canonicalSectionLabel(cleaned.replaceFirst(":$", ""))).append("\n\n");
                previousBlank = true;
                continue;
            }
            markdown.append(cleaned).append('\n');
            previousBlank = false;
        }
        return markdown.toString().strip();
    }

    static Optional<IndexResolution> findResolution(Path resolutionFile, String term) {
        if (resolutionFile == null) {
            return Optional.empty();
        }
        Path source = resolutionFile.toAbsolutePath().normalize();
        if (!Files.isRegularFile(source)) {
            throw new PdfTransformationException("Le registre des résolutions CodeBio est introuvable : " + source);
        }
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(source, java.nio.charset.StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException exception) {
            throw new PdfTransformationException("Impossible de lire le registre des résolutions CodeBio : " + source, exception);
        }
        List<IndexResolution> resolutions = new ArrayList<>();
        Set<String> indexedTerms = new HashSet<>();
        TreeSet<Integer> indexes = new TreeSet<>();
        for (String key : properties.stringPropertyNames()) {
            Matcher matcher = RESOLUTION_INDEXED_TERM.matcher(key);
            if (matcher.matches()) {
                indexes.add(Integer.parseInt(matcher.group(1)));
            }
        }
        for (int index : indexes) {
            String prefix = "resolution." + index + ".";
            String indexedTerm = requireResolutionValue(properties, prefix + "indexed_term", "resolution " + index);
            String canonicalEntry = requireResolutionValue(properties, prefix + "canonical_entry", indexedTerm);
            String evidenceText = requireResolutionValue(properties, prefix + "evidence_text", indexedTerm);
            String reviewStatus = requireResolutionValue(properties, prefix + "review_status", indexedTerm);
            if (!MANUALLY_VERIFIED.equals(reviewStatus)) {
                throw new PdfTransformationException("La résolution CodeBio doit être manuellement vérifiée : " + indexedTerm);
            }
            if (!indexedTerms.add(normalize(indexedTerm))) {
                throw new PdfTransformationException("Résolutions CodeBio dupliquées pour le terme : " + indexedTerm);
            }
            resolutions.add(new IndexResolution(indexedTerm, canonicalEntry, evidenceText, reviewStatus));
        }
        return resolutions.stream().filter(resolution -> sameTerm(resolution.indexedTerm(), term)).findFirst();
    }

    private static String requireResolutionValue(Properties properties, String key, String indexedTerm) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new PdfTransformationException("Résolution CodeBio incomplète pour le terme : " + indexedTerm);
        }
        return value.trim();
    }

    private static boolean containsEvidence(String canonicalText, String evidenceText) {
        String normalizedEvidence = normalizeForEvidence(evidenceText);
        String normalizedCanonicalText = normalizeForEvidence(canonicalText);
        if (normalizedCanonicalText.contains(normalizedEvidence)) {
            return true;
        }
        String[] evidenceWords = normalizedEvidence.split(" ");
        int firstWord = normalizedCanonicalText.indexOf(evidenceWords[0]);
        if (firstWord < 0) {
            return false;
        }
        String evidencePattern = "(?s)" + Pattern.quote(evidenceWords[0]);
        for (int index = 1; index < evidenceWords.length; index++) {
            evidencePattern += "(?:\\s+|\\s*[^\\p{L}\\p{N}]+\\s*)" + Pattern.quote(evidenceWords[index]);
        }
        return Pattern.compile(evidencePattern).matcher(normalizedCanonicalText.substring(firstWord)).find();
    }

    private static String normalizeForEvidence(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }

    static Set<String> readSectionTypes(Path structure) {
        Path source = Objects.requireNonNull(structure, "structure must not be null").toAbsolutePath().normalize();
        if (!Files.isRegularFile(source)) {
            throw new PdfTransformationException("Le référentiel de structure CodeBio est introuvable : " + source);
        }
        Properties properties = new Properties();
        try (var input = Files.newInputStream(source)) {
            properties.load(input);
        } catch (IOException exception) {
            throw new PdfTransformationException("Impossible de lire le référentiel de structure CodeBio : " + source, exception);
        }
        String configuredTypes = properties.getProperty("section.types", "");
        Set<String> sectionTypes = configuredTypes.isBlank()
                ? DEFAULT_SECTION_TYPES
                : Set.of(configuredTypes.split(","));
        return sectionTypes.stream()
                .map(String::trim)
                .filter(type -> !type.isBlank())
                .map(CodeBioDictionaryEntryPdfToMarkdownTransformer::normalize)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static boolean isKnownSection(String label, Set<String> sectionTypes) {
        return sectionTypes.contains(normalize(canonicalSectionLabel(label)));
    }

    private static String canonicalSectionLabel(String label) {
        String trimmed = label.trim();
        return SECTION_LABEL_CORRECTIONS.getOrDefault(normalize(trimmed), trimmed);
    }

    private static Path validatePdf(Path pdf) {
        Path source = Objects.requireNonNull(pdf, "dictionaryPdf must not be null").toAbsolutePath().normalize();
        if (!Files.isRegularFile(source) || !source.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new PdfTransformationException("Le dictionnaire CodeBio doit être un fichier PDF lisible : " + source);
        }
        return source;
    }

    private static Path validateMarkdown(Path markdown) {
        Path source = Objects.requireNonNull(markdown, "tableOfContentsMarkdown must not be null").toAbsolutePath().normalize();
        if (!Files.isRegularFile(source) || !source.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md")) {
            throw new PdfTransformationException("La table des matières CodeBio doit être un fichier Markdown lisible : " + source);
        }
        return source;
    }

    private static String requireTerm(String term) {
        if (term == null || term.isBlank()) {
            throw new PdfTransformationException("Le terme CodeBio ne doit pas être vide.");
        }
        return term.trim();
    }

    record IndexedEntry(String title, int page) {
    }

    record IndexResolution(String indexedTerm, String canonicalEntry, String evidenceText, String reviewStatus) {
    }

    record DetectedIndexReference(IndexedEntry canonicalEntry, String evidenceText) {
    }

    record IndexRedirect(IndexedEntry canonicalEntry) {
    }
}
