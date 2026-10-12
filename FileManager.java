import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Reads and writes data/words.txt and data/report.txt. */
public class FileManager {
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path WORDS_FILE = DATA_DIR.resolve("words.txt");
    private static final Path REPORT_FILE = DATA_DIR.resolve("report.txt");

    /** Creates the data folder if it is missing. */
    public void ensureDataFolder() throws IOException {
        Files.createDirectories(DATA_DIR);
    }

    /**
     * Loads all records. Returns an empty list if the file does not exist.
     * Throws IOException with the file name and line number if any record is invalid.
     */
    public List<WordEntry> loadWords() throws IOException {
        List<WordEntry> loaded = new ArrayList<>(); // temporary list
        if (!Files.exists(WORDS_FILE)) {
            return loaded;
        }

        Set<Integer> ids = new HashSet<>();
        Set<String> keys = new HashSet<>();

        try (BufferedReader reader = Files.newBufferedReader(WORDS_FILE, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue; // ignore blank lines
                }
                try {
                    WordEntry entry = parseLine(line);
                    if (!ids.add(entry.getId())) {
                        throw new IllegalArgumentException("Duplicate ID " + entry.getId() + ".");
                    }
                    if (!keys.add(entry.getKey())) {
                        throw new IllegalArgumentException("Duplicate word '" + entry.getWord() + "'.");
                    }
                    loaded.add(entry);
                } catch (IllegalArgumentException e) {
                    throw new IOException("words.txt line " + lineNumber + ": " + e.getMessage());
                }
            }
        }
        return loaded; // accepted only when every record is valid
    }

    private WordEntry parseLine(String line) {
        String[] f = line.split("\\|", -1); // -1 keeps an empty last field
        if (f.length != 7) {
            throw new IllegalArgumentException("Expected 7 fields but found " + f.length + ".");
        }

        int id;
        try {
            id = Integer.parseInt(f[0].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID is not a number.");
        }
        if (id <= 0) {
            throw new IllegalArgumentException("ID must be positive.");
        }

        String type = f[1].trim();
        String[] examples = { f[4], f[5] };
        String status = f[6].trim();

        if (type.equals("NOUN")) {
            return new NounEntry(id, f[2], f[3], examples, status);
        } else if (type.equals("VERB")) {
            return new VerbEntry(id, f[2], f[3], examples, status);
        }
        throw new IllegalArgumentException("Type must be NOUN or VERB.");
    }

    /** Saves all records to words.txt (temp file first, then replace). */
    public void saveWords(List<WordEntry> words) throws IOException {
        List<String> lines = new ArrayList<>();
        for (WordEntry w : words) {
            String[] ex = w.getExamples();
            lines.add(w.getId() + "|" + w.getTypeLabel().toUpperCase() + "|" + w.getWord() + "|"
                    + w.getMeaning() + "|" + ex[0] + "|" + ex[1] + "|" + w.getStatus());
        }
        writeSafely(WORDS_FILE, lines);
    }

    /** Saves readable report lines to report.txt. */
    public void saveReport(List<String> reportLines) throws IOException {
        writeSafely(REPORT_FILE, reportLines);
    }

    private void writeSafely(Path target, List<String> lines) throws IOException {
        ensureDataFolder();
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            } // writer closed here
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // nothing more to do
            }
            throw e;
        }
    }
}
