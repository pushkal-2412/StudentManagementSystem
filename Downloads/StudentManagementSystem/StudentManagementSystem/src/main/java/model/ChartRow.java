package model;

/** Simple label/value pair used for charts and pickers. */
public class ChartRow {
    private final String label;
    private final double value;

    public ChartRow(String label, double value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() { return label; }
    public double getValue() { return value; }

    @Override
    public String toString() { return label; }
}
