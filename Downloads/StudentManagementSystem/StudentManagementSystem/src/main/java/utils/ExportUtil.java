package utils;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import model.TableData;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/** Exports a TableData to CSV or PDF using a file chooser. */
public final class ExportUtil {

    private ExportUtil() { }

    /** @return the created file, or null when the user cancelled. */
    public static File exportCsv(Component parent, TableData data, String fileBaseName)
            throws IOException {
        File target = choose(parent, fileBaseName, "CSV file", "csv");
        if (target == null) return null;
        writeCsv(target, data);
        return target;
    }

    public static File exportPdf(Component parent, TableData data, String title, String subtitle)
            throws IOException {
        File target = choose(parent, sanitize(title), "PDF file", "pdf");
        if (target == null) return null;
        writePdf(target, data, title, subtitle);
        return target;
    }

    private static File choose(Component parent, String base, String desc, String ext) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save " + desc);
        fc.setFileFilter(new FileNameExtensionFilter(desc + " (*." + ext + ")", ext));
        fc.setSelectedFile(new File(base + "." + ext));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return null;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith("." + ext)) {
            f = new File(f.getParentFile(), f.getName() + "." + ext);
        }
        return f;
    }

    /** RFC-4180 style CSV with quoted cells, readable by Excel. */
    public static void writeCsv(File file, TableData data) throws IOException {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file),
                StandardCharsets.UTF_8)) {
            w.write('\uFEFF'); // BOM so Excel opens UTF-8 correctly
            w.write(join(data.getHeaders()));
            w.write("\r\n");
            for (List<String> row : data.getRows()) {
                w.write(join(row));
                w.write("\r\n");
            }
        }
    }

    private static String join(List<String> cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) sb.append(',');
            String v = cells.get(i) == null ? "" : cells.get(i);
            sb.append('"').append(v.replace("\"", "\"\"")).append('"');
        }
        return sb.toString();
    }

    public static void writePdf(File file, TableData data, String title, String subtitle)
            throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            writePdf(out, data, title, subtitle);
        }
    }

    public static void writePdf(OutputStream out, TableData data, String title, String subtitle)
            throws IOException {
        Document doc = new Document(PageSize.A4.rotate(), 28, 28, 30, 30);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph p = new Paragraph(pdfSafe(title),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, new Color(15, 23, 42)));
            p.setAlignment(Element.ALIGN_CENTER);
            doc.add(p);

            if (subtitle != null && !subtitle.isBlank()) {
                Paragraph s = new Paragraph(pdfSafe(subtitle),
                        FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(100, 116, 139)));
                s.setAlignment(Element.ALIGN_CENTER);
                s.setSpacingAfter(6);
                doc.add(s);
            }

            Paragraph stamp = new Paragraph(
                    "Generated: " + DateUtil.format(LocalDateTime.now())
                  + "   |   Records: " + data.size(),
                    FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(100, 116, 139)));
            stamp.setAlignment(Element.ALIGN_RIGHT);
            stamp.setSpacingAfter(10);
            doc.add(stamp);

            int cols = Math.max(1, data.getHeaders().size());
            PdfPTable table = new PdfPTable(cols);
            table.setWidthPercentage(100);
            float[] widths = new float[cols];
            for (int i = 0; i < cols; i++) widths[i] = 1f;
            table.setWidths(widths);

            com.lowagie.text.Font headFont =
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            for (String h : data.getHeaders()) {
                PdfPCell cell = new PdfPCell(new Phrase(pdfSafe(h), headFont));
                cell.setBackgroundColor(new Color(31, 111, 235));
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(cell);
            }

            com.lowagie.text.Font bodyFont =
                    FontFactory.getFont(FontFactory.HELVETICA, 8.5f, new Color(30, 41, 59));
            boolean alt = false;
            for (List<String> row : data.getRows()) {
                for (String v : row) {
                    PdfPCell cell = new PdfPCell(new Phrase(pdfSafe(v), bodyFont));
                    cell.setPadding(5);
                    if (alt) cell.setBackgroundColor(new Color(244, 246, 250));
                    table.addCell(cell);
                }
                alt = !alt;
            }
            doc.add(table);

            Paragraph foot = new Paragraph(
                    "Student Management System  -  generated report",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8,
                            new Color(148, 163, 184)));
            foot.setSpacingBefore(14);
            foot.setAlignment(Element.ALIGN_CENTER);
            doc.add(foot);
        } catch (com.lowagie.text.DocumentException e) {
            throw new IOException("Could not build the PDF: " + e.getMessage(), e);
        } finally {
            // Close the document BEFORE the stream: PdfWriter flushes into it on close.
            if (doc.isOpen()) {
                try {
                    doc.close();
                } catch (Exception ignored) {
                    // already reported through the primary exception
                }
            }
        }
    }

    /** OpenPDF's built-in Helvetica cannot draw the rupee sign, so spell it out. */
    private static String pdfSafe(String value) {
        if (value == null) return "";
        return value.replace("\u20B9", "Rs. ").replace("\u2013", "-").replace("\u2014", "-");
    }

    public static void confirmSaved(Component parent, File file) {
        JOptionPane.showMessageDialog(parent,
            "Saved successfully:\n" + file.getAbsolutePath(),
            "Export complete", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^A-Za-z0-9]+", "_");
    }
}
