package com.hillayes.commons;

public class Strings {
    /**
     * Tests whether the given string is null or empty or contains only white
     * space codepoints.
     *
     * @param str the string to test.
     * @return true if the string is null or empty, false otherwise.
     */
    static public boolean isBlank(String str) {
        return (str == null) || (str.isBlank());
    }

    /**
     * Tests whether the given string is not null and not empty.
     *
     * @param str the string to test.
     * @return true if the string is NOT null and NOT empty, false otherwise.
     */
    static public boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * Returns the given value, or null if the value is blank.
     * @see #isBlank(String)
     */
    static public String toStringOrNull(String value) {
        return isBlank(value) ? null : value;
    }

    /**
     * Returns the given value's toString() result, or null if the value is null.
     */
    static public String toStringOrNull(Object value) {
        return value == null ? null : value.toString();
    }

    static public String getOrDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    /**
     * Returns the given String trimmed, or null if the trimmed value is blank.
     */
    static public String trimOrNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    /**
     * Masks the email address by replacing the characters before the '@' with
     * a single character followed by '****'. This is intended for including
     * email addresses in log messages.
     */
    static public String maskEmail(String email) {
        if (isBlank(email)) {
            return email;
        }
        int at = email.indexOf('@');
        if (at < 0) {
            return email;
        }
        return email.charAt(0) + "****" + email.substring(at);
    }
}
