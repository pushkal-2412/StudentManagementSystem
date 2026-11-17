package model;

import java.util.ArrayList;
import java.util.List;

/** Generic headers + rows container used for tables, CSV and PDF export. */
public class TableData {
    private final List<String> headers = new ArrayList<>();
    private final List<List<String>> rows = new ArrayList<>();

    public TableData() { }

    public TableData(List<String> headers) {
        this.headers.addAll(headers);
    }

    public void addRow(List<String> row) { rows.add(row); }

    public void addRow(String... cells) {
        List<String> row = new ArrayList<>();
        for (String c : cells) row.add(c);
        rows.add(row);
    }

    public List<String> getHeaders() { return headers; }
    public List<List<String>> getRows() { return rows; }
    public int size() { return rows.size(); }
}
