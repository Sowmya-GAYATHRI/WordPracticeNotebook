import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Console menus. One Scanner, nextLine() only. */
public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static WordService service;

    public static void main(String[] args) {
        service = new WordService(new FileManager());
        try {
            service.load();
        } catch (IOException e) {
            System.out.println("Cannot load data: " + e.getMessage());
            System.out.println("Please fix the file and start again.");
            return;
        }

        try {
            boolean running = true;
            while (running) {
                showMenu();
                int choice = readInt("Choose: ");
                switch (choice) {
                    case 1: addWord(); break;
                    case 2: viewAll(); break;
                    case 3: searchWord(); break;
                    case 4: viewDetails(); break;
                    case 5: changeStatus(); break;
                    case 6: showReport(); break;
                    case 0:
                        System.out.println("Goodbye!");
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Enter a number from 0 to 6.");
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nInput ended. Goodbye!");
        }
    }

    private static void showMenu() {
        System.out.println();
        System.out.println("WORD NOTEBOOK MENU");
        System.out.println("1. Add word");
        System.out.println("2. View all words");
        System.out.println("3. Search word");
        System.out.println("4. View meaning and examples");
        System.out.println("5. Mark Learned or New");
        System.out.println("6. View and save progress report");
        System.out.println("0. Exit");
    }

    // ---------- input helpers ----------

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String text = scanner.nextLine().trim();
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    /** Reads non-blank text without '|' (so it can be saved safely). */
    private static String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String text = scanner.nextLine();
            try {
                return WordEntry.checkText("Input", text);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage().replace("Input", "This field"));
            }
        }
    }

    // ---------- menu actions ----------

    private static void addWord() {
        System.out.println();
        System.out.println("WORD TYPES");
        System.out.println("1. Noun");
        System.out.println("2. Verb");
        System.out.println("0. Back");
        String type = null;
        while (type == null) {
            int t = readInt("Choose type: ");
            if (t == 0) {
                return;
            } else if (t == 1) {
                type = "NOUN";
            } else if (t == 2) {
                type = "VERB";
            } else {
                System.out.println("Invalid choice. Enter 0, 1, or 2.");
            }
        }

        String word = readText("Word: ");
        if (service.hasWord(word)) {
            System.out.println("Word already exists.");
            return;
        }
        String meaning = readText("Meaning: ");
        String ex1 = readText("Example 1: ");
        String ex2 = readText("Example 2: ");

        try {
            WordEntry entry = service.addWord(type, word, meaning, ex1, ex2);
            System.out.println("Word " + entry.getId() + " saved. Status: " + entry.getStatus());
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println("Could not save: " + e.getMessage());
        }
    }

    private static void printRow(WordEntry w) {
        System.out.println(w.getId() + " | " + w.getWord() + " | " + w.getTypeLabel() + " | " + w.getStatus());
    }

    private static void viewAll() {
        List<WordEntry> all = service.getAll();
        if (all.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (WordEntry w : all) {
            printRow(w); // polymorphism: getTypeLabel() depends on the child class
        }
    }

    private static void searchWord() {
        String part = readText("Search: ");
        List<WordEntry> found = service.search(part);
        if (found.isEmpty()) {
            System.out.println("No matching words.");
            return;
        }
        for (WordEntry w : found) {
            printRow(w);
        }
    }

    /** Asks for an ID until it exists. Returns null if the user enters 0 to go back. */
    private static WordEntry askForEntry() {
        if (service.getAll().isEmpty()) {
            System.out.println("No records found.");
            return null;
        }
        while (true) {
            int id = readInt("Enter word ID (0 to go back): ");
            if (id == 0) {
                return null;
            }
            WordEntry entry = service.findById(id);
            if (entry != null) {
                return entry;
            }
            System.out.println("No word with ID " + id + ". Try again.");
        }
    }

    private static void viewDetails() {
        WordEntry w = askForEntry();
        if (w == null) {
            return;
        }
        System.out.println(w.getWord() + " (" + w.getTypeLabel() + ") - " + w.getStatus());
        System.out.println("Meaning: " + w.getMeaning());
        String[] examples = w.getExamples();
        for (int i = 0; i < examples.length; i++) {
            System.out.println("Example " + (i + 1));
            System.out.println(examples[i]);
        }
    }

    private static void changeStatus() {
        WordEntry w = askForEntry();
        if (w == null) {
            return;
        }
        System.out.println(w.getWord() + " is currently " + w.getStatus());
        System.out.println("1. LEARNED");
        System.out.println("2. NEW");
        System.out.println("0. Back");
        String newStatus = null;
        while (newStatus == null) {
            int c = readInt("Choose status: ");
            if (c == 0) {
                return;
            } else if (c == 1) {
                newStatus = WordEntry.LEARNED;
            } else if (c == 2) {
                newStatus = WordEntry.NEW;
            } else {
                System.out.println("Invalid choice. Enter 0, 1, or 2.");
            }
        }
        try {
            if (service.markStatus(w.getId(), newStatus)) {
                System.out.println("Status saved.");
            } else {
                System.out.println("Word already has status " + newStatus + ". No change made.");
            }
        } catch (IOException e) {
            System.out.println("Could not save: " + e.getMessage());
        }
    }

    private static void showReport() {
        service.showReport();
        while (true) {
            System.out.print("Save report to data/report.txt? (y/n): ");
            String answer = scanner.nextLine().trim().toLowerCase();
            if (answer.equals("n")) {
                return;
            } else if (answer.equals("y")) {
                try {
                    service.saveReport();
                    System.out.println("Report saved.");
                } catch (IOException e) {
                    System.out.println("Could not save report: " + e.getMessage());
                }
                return;
            }
            System.out.println("Please enter y or n.");
        }
    }
}
