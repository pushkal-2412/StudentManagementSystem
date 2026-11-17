package utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** One place for every date format used in the UI, CSV and PDF output. */
public final class DateUtil {

    public static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("dd MMM yyyy");
    public static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private DateUtil() { }

    public static String format(LocalDate d) {
        return d == null ? "-" : d.format(DISPLAY);
    }

    public static String format(LocalDateTime d) {
        return d == null ? "-" : d.format(STAMP);
    }

    public static String iso(LocalDate d) {
        return d == null ? "" : d.format(ISO);
    }

    public static LocalDate parse(String text) {
        return LocalDate.parse(text.trim(), ISO);
    }
}
