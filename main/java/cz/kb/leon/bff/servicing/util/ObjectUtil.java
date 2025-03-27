package cz.kb.leon.bff.servicing.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import java.util.stream.Stream;

public class ObjectUtil {

    public static final String DATE_PATTERN_CZ = "d. M. yyyy";

    private ObjectUtil() {
    }

    public static <T> String toString(T value) {
        if (value == null) {
            return "null";
        }

        return value.toString();
    }

    public static String toUpperCase(String value) {
        if (value == null) {
            return null;
        }
        return value.toUpperCase();
    }

    public static String toLowerCase(String value) {
        if (value == null) {
            return null;
        }
        return value.toLowerCase();
    }

    public static String evaluateMessage(String format, Object...values) {
        return Stream.of(values)
                .map(toSubst -> (Function<String, String>) s -> s.replaceFirst("\\{}", ObjectUtil.toString(toSubst)))
                .reduce(Function.identity(), Function::andThen)
                .apply(format);
    }

    public static boolean evaluateBoolean(Boolean value) {
        return evaluateBoolean(value, false);
    }

    public static boolean evaluateBoolean(Boolean value, boolean defaultValue) {
        return value == null ? defaultValue : value;
    }

    public static String formatDate(LocalDate date) {
        return formatDate(date, DATE_PATTERN_CZ);
    }

    public static String formatDate(LocalDate date, String pattern) {
        if (date == null) {
            return null;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return formatter.format(date);
    }

    public static Object formatAmount(BigDecimal amount, String currency) {
        return evaluateMessage("{} {}", amount, currency == null || "CZK".equalsIgnoreCase(currency) ? "Kč" : currency).replace(".", ",");
    }

}
