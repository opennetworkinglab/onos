package org.onlab.onos.store.service.impl;

/**
 * A table modification event.
 */
public final class TableModificationEvent {

    /**
     * Type of table modification event.
     *
     */
    public enum Type {
        ROW_ADDED,
        ROW_DELETED,
        ROW_UPDATED
    }

    private final String tableName;
    private final String key;
    private final Type type;

    /**
     * Creates a new row deleted table modification event.
     * @param tableName table name.
     * @param key row key
     * @return table modification event.
     */
    public static TableModificationEvent rowDeleted(String tableName, String key) {
        return new TableModificationEvent(tableName, key, Type.ROW_DELETED);
    }

    /**
     * Creates a new row added table modification event.
     * @param tableName table name.
     * @param key row key
     * @return table modification event.
     */
    public static TableModificationEvent rowAdded(String tableName, String key) {
        return new TableModificationEvent(tableName, key, Type.ROW_ADDED);
    }

    /**
     * Creates a new row updated table modification event.
     * @param tableName table name.
     * @param key row key
     * @return table modification event.
     */
    public static TableModificationEvent rowUpdated(String tableName, String key) {
        return new TableModificationEvent(tableName, key, Type.ROW_UPDATED);
    }

    private TableModificationEvent(String tableName, String key, Type type) {
        this.tableName = tableName;
        this.key = key;
        this.type = type;
    }

    /**
     * Returns name of table this event is for.
     * @return table name
     */
    public String tableName() {
        return tableName;
    }

    /**
     * Returns the row key this event is for.
     * @return row key
     */
    public String key() {
        return key;
    }

    /**
     * Returns the type of table modification event.
     * @return event type.
     */
    public Type type() {
        return type;
    }
}
