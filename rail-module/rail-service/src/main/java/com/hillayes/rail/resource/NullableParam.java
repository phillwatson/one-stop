package com.hillayes.rail.resource;

import com.hillayes.commons.Strings;
import lombok.ToString;

/**
 * A utility class to allow the use of null values in query parameters.
 *
 * As an example:
 * <pre>@QueryParam("acknowledged") NullableParam acknowledgedParam</pre>
 */
public class NullableParam {
    private final String value;

    public NullableParam(String param) {
        this.value = param;
    }

    /**
     * Returns true if the parameter is null or empty.
     */
    public boolean isNull() {
        return Strings.isBlank(value);
    }

    /**
     * Returns the parameter value as a string, or null if the parameter is null or empty.
     */
    public String asString() {
        return isNull() ? null : value;
    }

    /**
     * Returns the parameter value as a Boolean, or null if the parameter is null or empty.
     */
    public Boolean asBoolean() {
        return isNull() ? null : Boolean.valueOf(value);
    }

    public String toString() {
        return asString();
    }
}
