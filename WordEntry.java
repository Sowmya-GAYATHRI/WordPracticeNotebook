import java.util.Locale;

/**
 * Abstract parent of NounEntry and VerbEntry.
 * Encapsulation: all fields are private; the ID never changes.
 */
public abstract class WordEntry {
    public static final String NEW = "NEW";
    public static final String LEARNED = "LEARNED";

    private final int id;
    private final String word;
    private final String meaning;
    private final String[] examples; // exactly two sentences
    private String status;

    protected WordEntry(int id, String word, String meaning, String[] examples, String status) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID must be positive.");
        }
        if (examples == null || examples.length != 2) {
            throw new IllegalArgumentException("Exactly two examples are required.");
        }
        // Validate everything first, then assign.
        String cleanWord = checkText("Word", word);
        String cleanMeaning = checkText("Meaning", meaning);
        String[] copy = examples.clone(); // copy so outside code cannot change our array
        copy[0] = checkText("Example 1", copy[0]);
        copy[1] = checkText("Example 2", copy[1]);
        if (!isValidStatus(status)) {
            throw new IllegalArgumentException("Status must be NEW or LEARNED.");
        }

        this.id = id;
        this.word = cleanWord;
        this.meaning = cleanMeaning;
        this.examples = copy;
        this.status = status;
    }

    /** Trims text and rejects blanks, '|' and line breaks. */
    public static String checkText(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        if (value.contains("|") || value.contains("\n") || value.contains("\r")) {
            throw new IllegalArgumentException(fieldName + " cannot contain '|' or line breaks.");
        }
        return value.trim();
    }

    public static boolean isValidStatus(String status) {
        return NEW.equals(status) || LEARNED.equals(status);
    }

    /** Unique key: trimmed and lowercase. */
    public static String normalize(String word) {
        return word.trim().toLowerCase(Locale.ROOT);
    }

    public int getId() {
        return id;
    }

    public String getWord() {
        return word;
    }

    public String getKey() {
        return normalize(word);
    }

    public String getMeaning() {
        return meaning;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if (!isValidStatus(status)) {
            throw new IllegalArgumentException("Status must be NEW or LEARNED.");
        }
        this.status = status;
    }

    public String[] getExamples() {
        return examples.clone();
    }

    /** Child classes return "Noun" or "Verb". */
    public abstract String getTypeLabel();
}
