package utils;

import java.text.NumberFormat;
import java.util.Locale;

/** Currency formatting for the fee module. */
public final class MoneyUtil {

    private static final NumberFormat INR =
            NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"));

    static {
        INR.setMinimumFractionDigits(2);
        INR.setMaximumFractionDigits(2);
    }

    private MoneyUtil() { }

    public static String format(double amount) {
        return "\u20B9 " + INR.format(amount);
    }

    public static String plain(double amount) {
        return INR.format(amount);
    }
}
