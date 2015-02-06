package org.onosproject.store.consistent.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.kuujo.copycat.state.Initializer;
import net.kuujo.copycat.state.StateContext;

/**
 * Default database state.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class DefaultDatabaseState<K, V> implements DatabaseState<K, V> {

    private Long nextVersion;
    private Map<String, Map<K, Versioned<V>>> tables;

    @Initializer
    @Override
    public void init(StateContext<DatabaseState<K, V>> context) {
        tables = context.get("tables");
        if (tables == null) {
            tables = new HashMap<>();
            context.put("tables", tables);
        }
        nextVersion = context.get("nextVersion");
        if (nextVersion == null) {
            nextVersion = new Long(0);
            context.put("nextVersion", nextVersion);
        }
    }

    private Map<K, Versioned<V>> getTableMap(String tableName) {
        Map<K, Versioned<V>> table = tables.get(tableName);
        if (table == null) {
            table = new HashMap<>();
            tables.put(tableName, table);
        }
        return table;
    }

    @Override
    public int size(String tableName) {
      return getTableMap(tableName).size();
    }

    @Override
    public boolean isEmpty(String tableName) {
        return getTableMap(tableName).isEmpty();
    }

    @Override
    public boolean containsKey(String tableName, K key) {
        return getTableMap(tableName).containsKey(key);
    }

    @Override
    public boolean containsValue(String tableName, V value) {
        return getTableMap(tableName).values().stream().anyMatch(v -> checkEquality(v.value(), value));
    }

    @Override
    public Versioned<V> get(String tableName, K key) {
        return getTableMap(tableName).get(key);
    }

    @Override
    public Versioned<V> put(String tableName, K key, V value) {
        return getTableMap(tableName).put(key, new Versioned<>(value, ++nextVersion));
    }

    @Override
    public Versioned<V> remove(String tableName, K key) {
        return getTableMap(tableName).remove(key);
    }

    @Override
    public void clear(String tableName) {
        getTableMap(tableName).clear();
    }

    @Override
    public Set<K> keySet(String tableName) {
        return getTableMap(tableName).keySet();
    }

    @Override
    public Collection<Versioned<V>> values(String tableName) {
        return getTableMap(tableName).values();
    }

    @Override
    public Set<Entry<K, Versioned<V>>> entrySet(String tableName) {
        return getTableMap(tableName).entrySet();
    }

    @Override
    public Versioned<V> putIfAbsent(String tableName, K key, V value) {
        Versioned<V> existingValue = getTableMap(tableName).get(key);
        return existingValue != null ? existingValue : put(tableName, key, value);
    }

    @Override
    public boolean remove(String tableName, K key, V value) {
        Versioned<V> existing = getTableMap(tableName).get(key);
        if (existing != null && existing.value().equals(value)) {
            getTableMap(tableName).remove(key);
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(String tableName, K key, long version) {
        Versioned<V> existing = getTableMap(tableName).get(key);
        if (existing != null && existing.version() == version) {
            remove(tableName, key);
            return true;
        }
        return false;
    }

    @Override
    public boolean replace(String tableName, K key, V oldValue, V newValue) {
        Versioned<V> existing = getTableMap(tableName).get(key);
        if (existing != null && existing.value().equals(oldValue)) {
            put(tableName, key, newValue);
            return true;
        }
        return false;
    }

    @Override
    public boolean replace(String tableName, K key, long oldVersion, V newValue) {
        Versioned<V> existing = getTableMap(tableName).get(key);
        if (existing != null && existing.version() == oldVersion) {
            put(tableName, key, newValue);
            return true;
        }
        return false;
    }

    @Override
    public boolean batchUpdate(List<UpdateOperation<K, V>> updates) {
        if (updates.stream().anyMatch(update -> !checkIfUpdateIsPossible(update))) {
            return false;
        } else {
            updates.stream().forEach(this::doUpdate);
            return true;
        }
    }

    private void doUpdate(UpdateOperation<K, V> update) {
        String tableName = update.tableName();
        K key = update.key();
        switch (update.type()) {
        case PUT:
            put(tableName, key, update.value());
            return;
        case REMOVE:
            remove(tableName, key);
            return;
        case PUT_IF_ABSENT:
            putIfAbsent(tableName, key, update.value());
            return;
        case PUT_IF_VERSION_MATCH:
            replace(tableName, key, update.currentValue(), update.value());
            return;
        case PUT_IF_VALUE_MATCH:
            replace(tableName, key, update.currentVersion(), update.value());
            return;
        case REMOVE_IF_VERSION_MATCH:
            remove(tableName, key, update.currentVersion());
            return;
        case REMOVE_IF_VALUE_MATCH:
            remove(tableName, key, update.currentValue());
            return;
        default:
            throw new IllegalStateException("Unsupported type: " + update.type());
        }
    }

    private boolean checkIfUpdateIsPossible(UpdateOperation<K, V> update) {
        Versioned<V> existingEntry = get(update.tableName(), update.key());
        switch (update.type()) {
        case PUT:
        case REMOVE:
            return true;
        case PUT_IF_ABSENT:
            return existingEntry == null;
        case PUT_IF_VERSION_MATCH:
            return existingEntry != null && existingEntry.version() == update.currentVersion();
        case PUT_IF_VALUE_MATCH:
            return existingEntry != null && existingEntry.value().equals(update.currentValue());
        case REMOVE_IF_VERSION_MATCH:
            return existingEntry == null || existingEntry.version() == update.currentVersion();
        case REMOVE_IF_VALUE_MATCH:
            return existingEntry == null || existingEntry.value().equals(update.currentValue());
        default:
            throw new IllegalStateException("Unsupported type: " + update.type());
        }
    }

    private boolean checkEquality(V value1, V value2) {
        if (value1 instanceof byte[]) {
            return Arrays.equals((byte[]) value1, (byte[]) value2);
        }
        return value1.equals(value2);
    }
}
