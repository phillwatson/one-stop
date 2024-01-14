package com.hillayes.commons.jpa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Encapsulates the order by clause of a SQL query. The order by clause is a list of
 * named columns, each of which may be ordered ascending or descending.
 */
public class OrderBy {
    private List<Column> columns = new ArrayList();

    private OrderBy() {
    }

    public static OrderBy by(String column) {
        return new OrderBy().and(column);
    }

    public static OrderBy by(String column, Direction direction) {
        return new OrderBy().and(column, direction);
    }

    public static OrderBy descending(String... columns) {
        OrderBy result = new OrderBy();
        for (String column : columns) {
            result.and(column, OrderBy.Direction.Descending);
        }

        return result;
    }

    public static OrderBy ascending(String... columns) {
        OrderBy result = new OrderBy();
        for (String column : columns) {
            result.and(column, OrderBy.Direction.Ascending);
        }

        return result;
    }

    /**
     * Sets all named columns to descending order; overwriting any direction they might
     * already have.
     */
    public OrderBy descending() {
        return this.direction(Direction.Descending);
    }

    /**
     * Sets all named columns to ascending order; overwriting any direction they might
     * already have.
     */
    public OrderBy ascending() {
        return this.direction(Direction.Ascending);
    }

    /**
     * Sets all named columns to the given direction; overwriting any direction they might
     * already have.
     */
    public OrderBy direction(Direction direction) {
        Column column;
        for (Iterator<Column> cols = this.columns.iterator(); cols.hasNext(); column.direction = direction) {
            column = cols.next();
        }

        return this;
    }

    /**
     * Appends a named column to the "order by" clause, with default Ascending order.
     */
    public OrderBy and(String name) {
        this.columns.add(new Column(name));
        return this;
    }

    public OrderBy and(String name, Direction direction) {
        this.columns.add(new Column(name, direction));
        return this;
    }

    public List<Column> getColumns() {
        return this.columns;
    }

    public static class Column {
        private final String name;
        private Direction direction;

        public Column(String name) {
            this(name, Direction.Ascending);
        }

        public Column(String name, Direction direction) {
            this.name = name;
            this.direction = direction;
        }

        public String getName() {
            return this.name;
        }

        public Direction getDirection() {
            return this.direction;
        }
    }

    public static enum Direction {
        Ascending,
        Descending;
    }
}
