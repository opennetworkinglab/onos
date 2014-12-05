/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.store.service.impl;

import org.onosproject.store.service.VersionedValue;

import com.google.common.base.MoreObjects;

/**
 * A table modification event.
 */
public final class TableModificationEvent {

    /**
     * Type of table modification event.
     */
    public enum Type {
        ROW_ADDED,
        ROW_DELETED,
        ROW_UPDATED
    }

    private final String tableName;
    private final String key;
    private final VersionedValue value;
    private final Type type;

    /**
     * Creates a new row deleted table modification event.
     * @param tableName table name.
     * @param key row key
     * @param value value associated with the key when it was deleted.
     * @return table modification event.
     */
    public static TableModificationEvent rowDeleted(String tableName, String key, VersionedValue value) {
        return new TableModificationEvent(tableName, key, value, Type.ROW_DELETED);
    }

    /**
     * Creates a new row added table modification event.
     * @param tableName table name.
     * @param key row key
     * @param value value associated with the key
     * @return table modification event.
     */
    public static TableModificationEvent rowAdded(String tableName, String key, VersionedValue value) {
        return new TableModificationEvent(tableName, key, value, Type.ROW_ADDED);
    }

    /**
     * Creates a new row updated table modification event.
     * @param tableName table name.
     * @param key row key
     * @param newValue value
     * @return table modification event.
     */
    public static TableModificationEvent rowUpdated(String tableName, String key, VersionedValue newValue) {
        return new TableModificationEvent(tableName, key, newValue, Type.ROW_UPDATED);
    }

    private TableModificationEvent(String tableName, String key, VersionedValue value, Type type) {
        this.tableName = tableName;
        this.key = key;
        this.value = value;
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
     * Returns the value associated with the key. If the event for a deletion, this
     * method returns value that was deleted.
     * @return row value
     */
    public VersionedValue value() {
        return value;
    }

    /**
     * Returns the type of table modification event.
     * @return event type.
     */
    public Type type() {
        return type;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("type", type)
                .add("tableName", tableName)
                .add("key", key)
                .add("version", value.version())
                .toString();
    }
}
