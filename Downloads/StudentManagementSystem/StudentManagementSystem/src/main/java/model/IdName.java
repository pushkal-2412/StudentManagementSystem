package model;

/** Generic id + label pair for combo boxes (departments, courses, exams). */
public class IdName {
    private final int id;
    private final String label;

    public IdName(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int getId() { return id; }
    public String getLabel() { return label; }

    @Override
    public String toString() { return label; }
}
