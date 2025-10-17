package com.hillayes.shares.repository;

import com.hillayes.shares.errors.DatabaseException;
import org.postgresql.util.PGobject;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public abstract class SqlEntityMapper<T> {
    /**
     * Returns the ordered map of ColMappers keyed by the name of the column on
     * which they operate.
     * <p>
     * The order need not be alphabetic but MUST be consistent.
     */
    public Map<String, ColMapper<T>> colMappings = initColMappings();

    /**
     * Returns an ordered list of the column names matching the columns given in
     * the colMappings. The column names are separated with a comma and the whole
     * is enclosed in brackets.
     * <p>
     * The order MUST be consistent with the column order returned by the colMappings.
     */
    public String colNames = initColNames();

    /**
     * Returns a sequence of SQL value placeholders (i.e. ?) to match the number of
     * columns given in the colMappings. The placeholders are separated with a comma
     * and the whole is enclosed in brackets.
     */
    public String colPlaceholders = initColPlaceholders();

    /**
     * The implementation must provide an ordered map of ColMappers keyed on the table
     * column name. The implementation may make use of the various setter methods defined
     * in this class.
     */
    abstract public Map<String, ColMapper<T>> initColMappings();

    private String initColPlaceholders() {
        // initialise the column placeholders
        StringBuilder result = new StringBuilder("(");
        for (int i = 0; i < colMappings.size(); i++) {
            if (i > 0) result.append(',');
            result.append('?');
        }
        result.append(')');
        return result.toString();
    }

    private String initColNames() {
        // initialise the column names
        StringBuilder result = new StringBuilder("(");
        colMappings.keySet().forEach(col -> {
            if (result.length() > 1) result.append(',');
            result.append(col);
        });
        result.append(')');
        return result.toString();
    }

    /**
     * Maps the columns of the given entity to the column placeholders of the given
     * PreparedStatement.
     */
    public void map(PreparedStatement statement, int index, T entity) {
        int offset = index * colMappings.size();
        colMappings.forEach((key, value) ->
            value.map(statement, offset, entity)
        );
    }

    public void setString(PreparedStatement statement, int index, String value) {
        try {
            if (value == null) statement.setNull(index, java.sql.Types.VARCHAR);
            else statement.setString(index, value);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    public void setDecimal(PreparedStatement statement, int index, BigDecimal value) {
        try {
            if (value == null) statement.setNull(index, java.sql.Types.DECIMAL);
            else statement.setBigDecimal(index, value);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    public void setUuid(PreparedStatement statement, int index, UUID value) {
        try {
            if (value == null) statement.setNull(index, java.sql.Types.OTHER);
            else statement.setObject(index, value);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    public void setDate(PreparedStatement statement, int index, LocalDate value) {
        try {
            if (value == null) statement.setNull(index, java.sql.Types.DATE);
            else statement.setDate(index, java.sql.Date.valueOf(value));
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    public void setTimestamp(PreparedStatement statement, int index, Instant value) {
        try {
            if (value == null) statement.setNull(index, java.sql.Types.TIMESTAMP_WITH_TIMEZONE);
            else statement.setTimestamp(index, java.sql.Timestamp.from(value));
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    public void setJsonb(PreparedStatement statement, int index, String value) {
        try {
            PGobject jsonObject = new PGobject();
            jsonObject.setType("json");
            jsonObject.setValue((value == null) ? "null" : value);
            statement.setObject(index, jsonObject);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    /**
     * A functional interface to map a column value from the given entity to the
     * given indexed column of the given PreparedStatement. The implementation can
     * perform any transformation required to convert the entity's value to a
     * suitable SQL value.
     */
    @FunctionalInterface
    public interface ColMapper<T> {
        public void map(PreparedStatement statement, int index, T entity);
    }
}
