public class NounEntry extends WordEntry {

    public NounEntry(int id, String word, String meaning, String[] examples, String status) {
        super(id, word, meaning, examples, status);
    }

    @Override
    public String getTypeLabel() {
        return "Noun";
    }
}
