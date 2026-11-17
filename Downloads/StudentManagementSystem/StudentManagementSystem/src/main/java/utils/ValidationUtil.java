package utils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** All input validation rules live here so the UI and services agree. */
public final class ValidationUtil {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    /** 10 digit Indian mobile starting 6-9, optional +91 prefix. */
    private static final Pattern PHONE =
            Pattern.compile("^(\\+91[- ]?)?[6-9]\\d{9}$");
    private static final Pattern STUDENT_CODE =
            Pattern.compile("^[A-Z0-9]{3,20}$");
    private static final Pattern COURSE_CODE =
            Pattern.compile("^[A-Z0-9-]{2,15}$");

    private ValidationUtil() { }

    /** @throws IllegalArgumentException with a readable message. */
    public static void requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
    }

    public static void requireLength(String value, String fieldName, int max) {
        requireText(value, fieldName);
        if (value.trim().length() > max) {
            throw new IllegalArgumentException(
                    fieldName + " cannot exceed " + max + " characters.");
        }
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }

    public static void requireEmail(String email) {
        requireText(email, "Email");
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException(
                    "Invalid email address. Example: student@college.edu");
        }
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        return PHONE.matcher(phone.trim().replace(" ", "")).matches();
    }

    public static void requirePhone(String phone) {
        requireText(phone, "Phone number");
        if (!isValidPhone(phone)) {
            throw new IllegalArgumentException(
                    "Invalid phone number. Enter 10 digits starting with 6-9.");
        }
    }

    public static boolean isValidStudentCode(String code) {
        return code != null && STUDENT_CODE.matcher(code.trim().toUpperCase()).matches();
    }

    public static boolean isValidCourseCode(String code) {
        return code != null && COURSE_CODE.matcher(code.trim().toUpperCase()).matches();
    }

    /** Parses yyyy-MM-dd and reports a friendly message. */
    public static LocalDate parseDate(String value, String fieldName) {
        requireText(value, fieldName);
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    fieldName + " is not a valid date. Use format YYYY-MM-DD.");
        }
    }

    public static void requirePastDate(LocalDate date, String fieldName) {
        if (date == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(fieldName + " cannot be in the future.");
        }
    }

    public static void requireNotFuture(LocalDate date, String fieldName) {
        requirePastDate(date, fieldName);
    }

    public static int parseInt(String value, String fieldName, int min, int max) {
        requireText(value, fieldName);
        int parsed;
        try {
            parsed = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a whole number.");
        }
        if (parsed < min || parsed > max) {
            throw new IllegalArgumentException(
                    fieldName + " must be between " + min + " and " + max + ".");
        }
        return parsed;
    }

    public static double parseDouble(String value, String fieldName, double min, double max) {
        requireText(value, fieldName);
        double parsed;
        try {
            parsed = Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a number.");
        }
        if (parsed < min || parsed > max) {
            throw new IllegalArgumentException(
                    fieldName + " must be between " + fmt(min) + " and " + fmt(max) + ".");
        }
        return parsed;
    }

    /** Validates a mark against the exam maximum (0 <= mark <= max). */
    public static double validateMark(double mark, double maxMarks) {
        if (Double.isNaN(mark)) {
            throw new IllegalArgumentException("Marks must be a number.");
        }
        if (mark < 0) {
            throw new IllegalArgumentException("Marks cannot be negative.");
        }
        if (mark > maxMarks) {
            throw new IllegalArgumentException(
                    "Marks cannot exceed the maximum of " + fmt(maxMarks) + ".");
        }
        return mark;
    }

    public static String fmt(double d) {
        if (d == Math.rint(d)) return String.valueOf((long) d);
        return String.format("%.2f", d);
    }

    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
