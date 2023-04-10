package com.hillayes.commons;

public class Strings {
    /**
     * Tests whether the given string is null or empty.
     *
     * @param str the string to test.
     * @return true if the string is null or empty, false otherwise.
     */
    static public boolean isBlank(String str) {
        return (str == null) || (str.isBlank());
    }

    /**
     * Returns the given value's toString() result, or null if the value is null.
     */
    static public String toStringOrNull(Object value) {
        return value == null ? null : value.toString();
    }
}
