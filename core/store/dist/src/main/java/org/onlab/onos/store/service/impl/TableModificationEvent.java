package org.onlab.onos.store.service.impl;

public class TableModificationEvent {

    public enum Type {
        ROW_ADDED,
        ROW_DELETED,
        ROW_UPDATED
    }
    
    private final String tableName;
    private final String key;
    private final Type type;

    public static TableModificationEvent rowDeleted(String tableName, String key) {
        return new TableModificationEvent(tableName, key, Type.ROW_DELETED);
    }
    
    public static TableModificationEvent rowAdded(String tableName, String key) {
        return new TableModificationEvent(tableName, key, Type.ROW_ADDED);
    }
    
    public static TableModificationEvent rowUpdated(String tableName, String key) {
        return new TableModificationEvent(tableName, key, Type.ROW_UPDATED);
    }

    private TableModificationEvent(String tableName, String key, Type type) {
        this.tableName = tableName;
        this.key = key;
        this.type = type;
    }

    public String tableName() {
        return tableName;
    }

    public String key() {
        return key;
    }

    public Type type() {
        return type;
    }
}
