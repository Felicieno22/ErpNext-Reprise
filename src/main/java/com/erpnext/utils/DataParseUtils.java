package com.erpnext.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Utility class providing safe conversion helpers used across the service layer.
 */
public final class DataParseUtils {

    private DataParseUtils() {
        // utility class, do not instantiate
    }

    /**
     * Convert an Object to LocalDate.
     * Handles common ERPNext and custom formats :
     *  - "yyyy-MM-dd" (default ISO)
     *  - "yyyy/MM/dd"
     *  - "dd/MM/yyyy"
     *  - "dd,MM,yyyy"
     */
    public static LocalDate parseLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate date) return date;
        if (value instanceof String s) {
            s = s.trim();
            if (s.isEmpty()) return null;
            // Try DD/MM/YYYY
            try {
                return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (Exception ignored) {}
            // Try DD,MM,YYYY
            try {
                return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd,MM,yyyy"));
            } catch (Exception ignored) {}
            // Try ISO yyyy-MM-dd
            try {
                return LocalDate.parse(s);
            } catch (Exception ignored) {}
            // Try yyyy/MM/dd
            try {
                return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * Safe boolean parsing for various possible representations.
     */
    public static Boolean toBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean b) return b;
        if (value instanceof Integer i) return i != 0;
        if (value instanceof String s) {
            s = s.trim().toLowerCase();
            return switch (s) {
                case "true", "1" -> true;
                case "false", "0" -> false;
                default -> null;
            };
        }
        return null;
    }

    /**
     * Safe double parsing returning 0.0 on invalid input instead of throwing.
     */
    public static Double parseDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Double d) return d;
        if (value instanceof Integer i) return i.doubleValue();
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }
        return 0.0;
    }
}
