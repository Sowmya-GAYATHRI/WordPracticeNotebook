import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Project actions: add, search, update status, report. */
public class WordService {
    private final FileManager fileManager;
    private final List<WordEntry> words = new ArrayList<>(); // Noun and Verb in one list
    private final Set<String> wordKeys = new HashSet<>();    // normalized words
    private int nextId = 1;

    public WordService(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    /** Loads saved records and rebuilds the key set and next ID. */
    public void load() throws IOException {
        fileManager.ensureDataFolder();
        List<WordEntry> loaded = fileManager.loadWords();
        words.clear();
        wordKeys.clear();
        int maxId = 0;
        for (WordEntry w : loaded) {
            words.add(w);
            wordKeys.add(w.getKey());
            maxId = Math.max(maxId, w.getId());
        }
        nextId = maxId + 1;
    }

    public boolean hasWord(String word) {
        return wordKeys.contains(WordEntry.normalize(word));
    }

    /** type must be "NOUN" or "VERB". Throws IllegalArgumentException for invalid data. */
    public WordEntry addWord(String type, String word, String meaning, String ex1, String ex2)
            throws IOException {
        String cleanWord = WordEntry.checkText("Word", word);
        if (hasWord(cleanWord)) {
            throw new IllegalArgumentException("Word already exists.");
        }
        String[] examples = { ex1, ex2 };

        WordEntry entry;
        if (type.equals("NOUN")) {
            entry = new NounEntry(nextId, cleanWord, meaning, examples, WordEntry.NEW);
        } else if (type.equals("VERB")) {
            entry = new VerbEntry(nextId, cleanWord, meaning, examples, WordEntry.NEW);
        } else {
            throw new IllegalArgumentException("Type must be NOUN or VERB.");
        }

        words.add(entry);
        wordKeys.add(entry.getKey());
        nextId++;
        try {
            fileManager.saveWords(words);
        } catch (IOException e) { // cancel the change in memory
            words.remove(entry);
            wordKeys.remove(entry.getKey());
            nextId--;
            throw e;
        }
        return entry;
    }

    public List<WordEntry> getAll() {
        return new ArrayList<>(words);
    }

    public WordEntry findById(int id) {
        for (WordEntry w : words) {
            if (w.getId() == id) {
                return w;
            }
        }
        return null;
    }

    /** Case-insensitive search by part of a word. */
    public List<WordEntry> search(String part) {
        String needle = part.trim().toLowerCase(Locale.ROOT);
        List<WordEntry> found = new ArrayList<>();
        if (needle.isEmpty()) {
            return found;
        }
        for (WordEntry w : words) {
            if (w.getKey().contains(needle)) {
                found.add(w);
            }
        }
        return found;
    }

    /**
     * Changes the status and saves.
     * Returns false (record unchanged) if the status is already the same.
     */
    public boolean markStatus(int id, String newStatus) throws IOException {
        WordEntry entry = findById(id);
        if (entry == null) {
            throw new IllegalArgumentException("No word with ID " + id + ".");
        }
        if (entry.getStatus().equals(newStatus)) {
            return false;
        }
        String old = entry.getStatus();
        entry.setStatus(newStatus);
        try {
            fileManager.saveWords(words);
        } catch (IOException e) {
            entry.setStatus(old); // cancel the change in memory
            throw e;
        }
        return true;
    }

    /** Builds the report lines (also used for report.txt). */
    public List<String> buildReport() {
        int nouns = 0, verbs = 0, newCount = 0, learned = 0;
        for (WordEntry w : words) {
            if (w.getTypeLabel().equals("Noun")) {
                nouns++;
            } else {
                verbs++;
            }
            if (w.getStatus().equals(WordEntry.LEARNED)) {
                learned++;
            } else {
                newCount++;
            }
        }
        List<String> lines = new ArrayList<>();
        lines.add("Total words: " + words.size());
        lines.add("Noun entries: " + nouns);
        lines.add("Verb entries: " + verbs);
        lines.add("NEW words: " + newCount);
        lines.add("LEARNED words: " + learned);
        return lines;
    }

    public void showReport() {
        System.out.println("--- Progress Report ---");
        for (String line : buildReport()) {
            System.out.println(line);
        }
    }

    public void saveReport() throws IOException {
        fileManager.saveReport(buildReport());
    }
}
