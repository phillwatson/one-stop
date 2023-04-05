package com.hillayes.commons;

public class Strings {
    /**
     * Returns the given value's toString() result, or null if the value is null.
     */
    static public String toStringOrNull(Object value) {
        return value == null ? null : value.toString();
    }
}
